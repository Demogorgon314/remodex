import test from "node:test";
import assert from "node:assert/strict";

import {
  buildRelayHealthStats,
  classifyRelayTrafficLabel,
  createHealthSnapshot,
  mergeTrafficBatch,
  recordTraffic,
  updateSessionSummary,
} from "../src/health/state.js";

test("health state aggregates active sessions and connected clients", () => {
  let snapshot = createHealthSnapshot({ startedAt: 1_000, updatedAt: 1_000 });
  snapshot = updateSessionSummary(snapshot, {
    sessionId: "session-a",
    hasMac: true,
    clientCount: 1,
  }, {
    now: 2_000,
  });
  snapshot = updateSessionSummary(snapshot, {
    sessionId: "session-b",
    hasMac: false,
    clientCount: 1,
  }, {
    now: 3_000,
  });

  const relay = buildRelayHealthStats(snapshot);
  assert.equal(relay.activeSessions, 2);
  assert.equal(relay.sessionsWithMac, 1);
  assert.equal(relay.totalClients, 2);
});

test("health state drops empty sessions when nothing is connected", () => {
  let snapshot = createHealthSnapshot();
  snapshot = updateSessionSummary(snapshot, {
    sessionId: "session-a",
    hasMac: true,
    clientCount: 1,
  });
  snapshot = updateSessionSummary(snapshot, {
    sessionId: "session-a",
    hasMac: false,
    clientCount: 0,
  });

  const relay = buildRelayHealthStats(snapshot);
  assert.equal(relay.activeSessions, 0);
  assert.equal(relay.totalClients, 0);
});

test("health state records traffic totals and top labels", () => {
  let snapshot = createHealthSnapshot({ startedAt: 1_000, updatedAt: 1_000 });
  snapshot = recordTraffic(snapshot, {
    channel: "macToIphone",
    message: JSON.stringify({ kind: "encryptedEnvelope", payload: "AAA" }),
  }, {
    now: 2_000,
  });
  snapshot = recordTraffic(snapshot, {
    channel: "macToIphone",
    message: JSON.stringify({ method: "thread/start" }),
  }, {
    now: 3_000,
  });

  const relay = buildRelayHealthStats(snapshot);
  assert.equal(relay.traffic.channels.macToIphone.messages, 2);
  assert.ok(relay.traffic.channels.macToIphone.bytes > 0);
  assert.deepEqual(
    relay.traffic.channels.macToIphone.topLabels.map((entry) => entry.label),
    ["kind:encryptedEnvelope", "thread/start"]
  );
});

test("health state classifies non-json payloads separately", () => {
  assert.equal(classifyRelayTrafficLabel("not json"), "non_json");
});

test("health state merges batched traffic updates", () => {
  const snapshot = mergeTrafficBatch(createHealthSnapshot({ startedAt: 1_000, updatedAt: 1_000 }), {
    channels: {
      iphoneToMac: {
        messages: 3,
        bytes: 300,
        labels: {
          "kind:encryptedEnvelope": {
            messages: 2,
            bytes: 250,
          },
          "kind:clientHello": {
            messages: 1,
            bytes: 50,
          },
        },
      },
    },
  }, {
    now: 2_000,
  });

  const relay = buildRelayHealthStats(snapshot);
  assert.equal(relay.traffic.channels.iphoneToMac.messages, 3);
  assert.equal(relay.traffic.channels.iphoneToMac.bytes, 300);
  assert.deepEqual(
    relay.traffic.channels.iphoneToMac.topLabels.map((entry) => entry.label),
    ["kind:encryptedEnvelope", "kind:clientHello"]
  );
});
