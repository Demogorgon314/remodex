// FILE: push-service.test.js
// Purpose: Verifies session registration, secret enforcement, persistence, and completion push dedupe.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, node:fs, node:os, node:path, ./push-service

const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");

const {
  createPushSessionService,
  createFileBackedPushStateStore,
  resolvePushStateFilePath,
} = require("./push-service");

test("push service stores device registration and sends one completion alert", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "remodex-push-state-"));
  const sent = [];
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        sent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: createFileBackedPushStateStore({
      stateFilePath: path.join(tempDir, "push-state.json"),
    }),
  });

  await service.registerDevice({
    sessionId: "session-1",
    notificationSecret: "secret-1",
    deviceToken: "aa bb cc",
    alertsEnabled: true,
    apnsEnvironment: "development",
  });

  await service.notifyCompletion({
    sessionId: "session-1",
    notificationSecret: "secret-1",
    threadId: "thread-1",
    turnId: "turn-1",
    dedupeKey: "done-1",
    title: "Fix auth bug",
    body: "Response ready",
  });
  await service.notifyCompletion({
    sessionId: "session-1",
    notificationSecret: "secret-1",
    threadId: "thread-1",
    turnId: "turn-1",
    dedupeKey: "done-1",
  });

  assert.equal(sent.length, 1);
  assert.equal(sent[0].deviceToken, "aabbcc");
  assert.equal(sent[0].apnsEnvironment, "development");
  assert.equal(sent[0].payload.threadId, "thread-1");
});

test("push service preserves Android FCM registrations and routes completions to the FCM client", async () => {
  const sent = [];
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification() {
        throw new Error("APNs should not be used for Android FCM registrations.");
      },
    },
    fcmClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        sent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: {
      read() {
        return {
          sessions: [],
          deliveredDedupeKeys: [],
        };
      },
      write() {},
    },
  });

  await service.registerDevice({
    sessionId: "session-fcm",
    notificationSecret: "secret-fcm",
    deviceToken: "fcm-token-android-1",
    alertsEnabled: true,
    authorizationStatus: "authorized",
    appEnvironment: "production",
    platform: "android",
    pushProvider: "fcm",
  });

  await service.notifyCompletion({
    sessionId: "session-fcm",
    notificationSecret: "secret-fcm",
    threadId: "thread-fcm",
    turnId: "turn-fcm",
    dedupeKey: "done-fcm",
    title: "Android parity",
    body: "FCM delivery ready",
  });

  assert.equal(sent.length, 1);
  assert.equal(sent[0].deviceToken, "fcm-token-android-1");
  assert.equal(sent[0].payload.threadId, "thread-fcm");
  assert.equal(sent[0].payload.turnId, "turn-fcm");
});

test("push service rejects registration when the relay session is not active", async () => {
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification() {},
    },
    canRegisterSession: () => false,
  });

  await assert.rejects(
    service.registerDevice({
      sessionId: "session-missing",
      notificationSecret: "secret-missing",
      deviceToken: "aabbcc",
      alertsEnabled: true,
    }),
    /active relay session/
  );
});

test("push service rejects mismatched notification secrets", async () => {
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification() {},
    },
    canRegisterSession: () => true,
  });

  await service.registerDevice({
    sessionId: "session-2",
    notificationSecret: "secret-2",
    deviceToken: "aabbcc",
    alertsEnabled: true,
  });

  await assert.rejects(
    service.notifyCompletion({
      sessionId: "session-2",
      notificationSecret: "wrong-secret",
      threadId: "thread-2",
      dedupeKey: "dedupe-2",
    }),
    /Invalid notification secret/
  );
});

test("push service rejects completion sends once the live relay session is gone", async () => {
  let sessionIsLive = true;
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification() {},
    },
    canRegisterSession: () => true,
    canNotifyCompletion: () => sessionIsLive,
  });

  await service.registerDevice({
    sessionId: "session-live-only",
    notificationSecret: "secret-live-only",
    deviceToken: "aabbcc",
    alertsEnabled: true,
  });

  sessionIsLive = false;

  await assert.rejects(
    service.notifyCompletion({
      sessionId: "session-live-only",
      notificationSecret: "secret-live-only",
      threadId: "thread-live-only",
      dedupeKey: "dedupe-live-only",
    }),
    /active relay session/
  );
});

test("push service reloads registrations from persisted state after a restart", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "remodex-push-state-"));
  const stateFilePath = path.join(tempDir, "push-state.json");
  const firstSent = [];

  const firstService = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        firstSent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: createFileBackedPushStateStore({ stateFilePath }),
  });

  await firstService.registerDevice({
    sessionId: "session-persisted",
    notificationSecret: "secret-persisted",
    deviceToken: "ddeeff",
    alertsEnabled: true,
  });

  const secondSent = [];
  const secondService = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        secondSent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: createFileBackedPushStateStore({ stateFilePath }),
  });

  await secondService.notifyCompletion({
    sessionId: "session-persisted",
    notificationSecret: "secret-persisted",
    threadId: "thread-persisted",
    dedupeKey: "done-persisted",
  });

  assert.equal(firstSent.length, 0);
  assert.equal(secondSent.length, 1);
  assert.equal(fs.statSync(stateFilePath).mode & 0o777, 0o600);
});

test("push service still reloads legacy APNs registrations that only stored apnsEnvironment", async () => {
  const tempDir = fs.mkdtempSync(path.join(os.tmpdir(), "remodex-push-state-"));
  const stateFilePath = path.join(tempDir, "push-state.json");
  fs.writeFileSync(stateFilePath, JSON.stringify({
    sessions: [[
      "session-legacy",
      {
        notificationSecret: "secret-legacy",
        deviceToken: "aa bb cc",
        alertsEnabled: true,
        apnsEnvironment: "development",
        updatedAt: Date.now(),
      },
    ]],
    deliveredDedupeKeys: [],
  }), "utf8");

  const sent = [];
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        sent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: createFileBackedPushStateStore({ stateFilePath }),
  });

  await service.notifyCompletion({
    sessionId: "session-legacy",
    notificationSecret: "secret-legacy",
    threadId: "thread-legacy",
    dedupeKey: "done-legacy",
  });

  assert.equal(sent.length, 1);
  assert.equal(sent[0].deviceToken, "aabbcc");
  assert.equal(sent[0].apnsEnvironment, "development");
});

test("push service defaults to a durable state file in the remodex home dir", () => {
  const resolved = resolvePushStateFilePath({
    CODEX_HOME: "/tmp/codex-home",
  });

  assert.equal(resolved, "/tmp/codex-home/remodex/push-state.json");
});

test("push service keeps working when state persistence fails", async () => {
  const sent = [];
  const service = createPushSessionService({
    apnsClient: {
      isConfigured: () => true,
      async sendNotification(payload) {
        sent.push(payload);
      },
    },
    canRegisterSession: () => true,
    stateStore: {
      read() {
        return {
          sessions: [],
          deliveredDedupeKeys: [],
        };
      },
      write() {
        throw new Error("disk full");
      },
    },
  });

  await service.registerDevice({
    sessionId: "session-no-disk",
    notificationSecret: "secret-no-disk",
    deviceToken: "aabbcc",
    alertsEnabled: true,
  });

  await service.notifyCompletion({
    sessionId: "session-no-disk",
    notificationSecret: "secret-no-disk",
    threadId: "thread-no-disk",
    dedupeKey: "done-no-disk",
  });

  assert.equal(sent.length, 1);
  assert.equal(sent[0].deviceToken, "aabbcc");
});
