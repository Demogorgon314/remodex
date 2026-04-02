// FILE: codex-transport.test.js
// Purpose: Verifies endpoint-backed Codex transport only sends after the websocket is open.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, ../src/codex-transport

const test = require("node:test");
const assert = require("node:assert/strict");
const { EventEmitter } = require("node:events");

const { createCodexTransport } = require("../src/codex-transport");

class FakeWebSocket {
  static CONNECTING = 0;
  static OPEN = 1;
  static CLOSED = 3;
  static latestInstance = null;

  constructor(endpoint) {
    this.endpoint = endpoint;
    this.readyState = FakeWebSocket.CONNECTING;
    this.handlers = {};
    this.sentMessages = [];
    FakeWebSocket.latestInstance = this;
  }

  on(eventName, handler) {
    this.handlers[eventName] = handler;
  }

  send(message) {
    this.sentMessages.push(message);
  }

  close() {
    this.readyState = FakeWebSocket.CLOSED;
  }

  emit(eventName, ...args) {
    this.handlers[eventName]?.(...args);
  }
}

test("endpoint transport only sends outbound messages after the websocket opens", () => {
  const transport = createCodexTransport({
    endpoint: "ws://127.0.0.1:4321/codex",
    WebSocketImpl: FakeWebSocket,
  });

  const socket = FakeWebSocket.latestInstance;
  assert.ok(socket);
  assert.equal(socket.endpoint, "ws://127.0.0.1:4321/codex");

  transport.send('{"id":"init-1","method":"initialize"}');
  transport.send('{"id":"list-1","method":"thread/list"}');
  assert.deepEqual(socket.sentMessages, []);

  socket.readyState = FakeWebSocket.OPEN;
  socket.emit("open");

  assert.deepEqual(socket.sentMessages, []);

  transport.send('{"id":"list-2","method":"thread/list"}');
  assert.deepEqual(socket.sentMessages, ['{"id":"list-2","method":"thread/list"}']);
});

test("spawn transport preserves utf-8 assistant payloads split across stdout chunks", () => {
  const fakeChild = new FakeChildProcess();
  const transport = createCodexTransport({
    env: {},
    spawnImpl() {
      return fakeChild;
    },
  });

  const received = [];
  transport.onMessage((message) => {
    received.push(message);
  });

  const payload = Buffer.from('{"id":"delta-1","method":"item/agentMessage/delta","params":{"delta":"几何规则"}}\n', "utf8");
  const splitIndex = payload.indexOf(Buffer.from("何", "utf8")) + 1;
  fakeChild.stdout.emit("data", payload.subarray(0, splitIndex));
  fakeChild.stdout.emit("data", payload.subarray(splitIndex));

  assert.deepEqual(received, [
    '{"id":"delta-1","method":"item/agentMessage/delta","params":{"delta":"几何规则"}}',
  ]);
});

class FakeChildProcess extends EventEmitter {
  constructor() {
    super();
    this.stdin = new FakeWritable();
    this.stdout = new EventEmitter();
    this.stderr = new EventEmitter();
    this.pid = 1234;
    this.killed = false;
    this.exitCode = null;
  }

  kill() {
    this.killed = true;
  }
}

class FakeWritable extends EventEmitter {
  constructor() {
    super();
    this.writable = true;
    this.destroyed = false;
    this.writableEnded = false;
    this.writes = [];
  }

  write(message) {
    this.writes.push(message);
  }
}
