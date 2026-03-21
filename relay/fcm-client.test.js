// FILE: fcm-client.test.js
// Purpose: Verifies FCM HTTP v1 requests use bearer auth, high-priority Android delivery, and stringified data payloads.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, ./fcm-client

const test = require("node:test");
const assert = require("node:assert/strict");

const { createFCMClient } = require("./fcm-client");

test("FCM client sends a provider-authenticated HTTP v1 request", async () => {
  const requests = [];
  const client = createFCMClient({
    projectId: "remodex-fcm-test",
    googleAuth: {
      async getProjectId() {
        return "remodex-fcm-test";
      },
      async getClient() {
        return {
          async getAccessToken() {
            return { token: "access-token-123" };
          },
        };
      },
    },
    fetchImpl: async (url, options) => {
      requests.push({
        url,
        method: options.method,
        headers: options.headers,
        body: JSON.parse(options.body),
      });
      return {
        ok: true,
        async text() {
          return JSON.stringify({ name: "projects/remodex-fcm-test/messages/1" });
        },
      };
    },
  });

  await client.sendNotification({
    deviceToken: "fcm-token-1",
    title: "Conversation ready",
    body: "Tap to reopen the thread",
    payload: {
      source: "codex.runCompletion",
      threadId: "thread-1",
      turnId: "turn-1",
      result: "completed",
    },
  });

  assert.deepEqual(requests, [{
    url: "https://fcm.googleapis.com/v1/projects/remodex-fcm-test/messages:send",
    method: "POST",
    headers: {
      authorization: "Bearer access-token-123",
      "content-type": "application/json; UTF-8",
    },
    body: {
      message: {
        token: "fcm-token-1",
        data: {
          source: "codex.runCompletion",
          threadId: "thread-1",
          turnId: "turn-1",
          result: "completed",
        },
        android: {
          priority: "high",
          ttl: "30s",
        },
        notification: {
          title: "Conversation ready",
          body: "Tap to reopen the thread",
        },
      },
    },
  }]);
});

test("FCM client rejects unconfigured sends before making a request", async () => {
  const client = createFCMClient({
    projectId: "",
    credentialsFile: "",
    credentialsJSON: "",
    googleAuth: {
      async getProjectId() {
        return "";
      },
      async getClient() {
        throw new Error("should not be called");
      },
    },
    fetchImpl: async () => {
      throw new Error("should not be called");
    },
  });

  await assert.rejects(
    client.sendNotification({
      deviceToken: "fcm-token-2",
    }),
    /FCM credentials are not configured/
  );
});
