// FILE: push-notification-service-client.test.js
// Purpose: Verifies timeout behavior for push-service HTTP calls from the local bridge.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, ../src/push-notification-service-client

const test = require("node:test");
const assert = require("node:assert/strict");

const { createPushNotificationServiceClient } = require("../src/push-notification-service-client");

test("push service client aborts stalled requests with a timeout error", async () => {
  const client = createPushNotificationServiceClient({
    baseUrl: "https://push.example.test",
    sessionId: "session-timeout",
    notificationSecret: "secret-timeout",
    requestTimeoutMs: 20,
    fetchImpl: async (_url, options) => new Promise((_, reject) => {
      options.signal.addEventListener("abort", () => {
        reject(options.signal.reason);
      }, { once: true });
    }),
  });

  await assert.rejects(
    client.registerDevice({
      deviceToken: "aabbcc",
      alertsEnabled: true,
      authorizationStatus: "authorized",
      appEnvironment: "development",
      platform: "ios",
      pushProvider: "apns",
    }),
    /timed out after 20ms/
  );
});

test("push service client forwards provider-aware registration fields", async () => {
  const requests = [];
  const client = createPushNotificationServiceClient({
    baseUrl: "https://push.example.test",
    sessionId: "session-payload",
    notificationSecret: "secret-payload",
    fetchImpl: async (url, options) => {
      requests.push({
        url,
        method: options.method,
        body: JSON.parse(options.body),
      });
      return {
        ok: true,
        async text() {
          return JSON.stringify({ ok: true });
        },
      };
    },
  });

  await client.registerDevice({
    deviceToken: "fcm-token-1",
    alertsEnabled: false,
    authorizationStatus: "authorized",
    appEnvironment: "production",
    platform: "android",
    pushProvider: "fcm",
  });

  assert.deepEqual(requests, [{
    url: "https://push.example.test/v1/push/session/register-device",
    method: "POST",
    body: {
      sessionId: "session-payload",
      notificationSecret: "secret-payload",
      deviceToken: "fcm-token-1",
      alertsEnabled: false,
      authorizationStatus: "authorized",
      appEnvironment: "production",
      platform: "android",
      pushProvider: "fcm",
    },
  }]);
});
