// FILE: voice-handler.test.js
// Purpose: Verifies bridge-owned voice transcription auth, validation, and retry behavior.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, ../src/voice-handler

const test = require("node:test");
const assert = require("node:assert/strict");

const { createVoiceHandler } = require("../src/voice-handler");

test("voice/transcribe returns transcribed text without exposing auth tokens", async () => {
  const responses = [];
  const fetchCalls = [];
  const authRequests = [];
  const handler = createVoiceHandler({
    sendCodexRequest: async (method, params) => {
      authRequests.push({ method, params });
      assert.equal(method, "getAuthStatus");
      return {
        authMethod: "chatgpt",
        authToken: makeJWT({
          "https://api.openai.com/auth": {
            chatgpt_account_id: "acct-123",
          },
        }),
        requiresOpenaiAuth: false,
      };
    },
    fetchImpl: async (url, options) => {
      fetchCalls.push({ url, options });
      return {
        ok: true,
        status: 200,
        async json() {
          return { text: "hello world" };
        },
      };
    },
  });

  const handled = handler.handleVoiceRequest(JSON.stringify({
    id: "voice-1",
    method: "voice/transcribe",
    params: {
      mimeType: "audio/wav",
      audioBase64: makeTestWavBase64(),
      sampleRateHz: 24_000,
      durationMs: 1_200,
    },
  }), (response) => {
    responses.push(JSON.parse(response));
  });

  assert.equal(handled, true);
  await tick();

  assert.deepEqual(authRequests, [
    {
      method: "getAuthStatus",
      params: {
        includeToken: true,
        refreshToken: false,
      },
    },
  ]);
  assert.equal(fetchCalls.length, 1);
  assert.equal(fetchCalls[0].url, "https://chatgpt.com/backend-api/transcribe");
  assert.equal(fetchCalls[0].options.method, "POST");
  assert.equal(fetchCalls[0].options.headers.Authorization.startsWith("Bearer "), true);
  assert.equal(fetchCalls[0].options.headers["ChatGPT-Account-Id"], undefined);
  assert.deepEqual(responses, [{
    id: "voice-1",
    result: {
      text: "hello world",
    },
  }]);
});

test("voice/transcribe retries once after a 401 response", async () => {
  const responses = [];
  const authRequests = [];
  let fetchCount = 0;
  const handler = createVoiceHandler({
    sendCodexRequest: async (method, params) => {
      authRequests.push({ method, params });
      return {
        authMethod: "chatgpt",
        authToken: makeJWT({
          "https://api.openai.com/auth": {
            chatgpt_account_id: `acct-${authRequests.length}`,
          },
        }),
        requiresOpenaiAuth: false,
      };
    },
    readLocalAuthToken: async () => null,
    fetchImpl: async () => {
      fetchCount += 1;
      if (fetchCount === 1) {
        return {
          ok: false,
          status: 401,
          async json() {
            return { error: { message: "expired" } };
          },
        };
      }

      return {
        ok: true,
        status: 200,
        async json() {
          return { text: "second try works" };
        },
      };
    },
  });

  handler.handleVoiceRequest(JSON.stringify({
    id: "voice-2",
    method: "voice/transcribe",
    params: {
      mimeType: "audio/wav",
      audioBase64: makeTestWavBase64(),
      sampleRateHz: 24_000,
      durationMs: 800,
    },
  }), (response) => {
    responses.push(JSON.parse(response));
  });

  await tick();

  assert.deepEqual(authRequests, [
    {
      method: "getAuthStatus",
      params: {
        includeToken: true,
        refreshToken: false,
      },
    },
    {
      method: "getAuthStatus",
      params: {
        includeToken: true,
        refreshToken: true,
      },
    },
  ]);
  assert.equal(fetchCount, 2);
  assert.equal(responses[0].result?.text, "second try works");
});

test("voice/transcribe rejects API-key auth because voice remains ChatGPT-only", async () => {
  const responses = [];
  let fetchCalled = false;
  const handler = createVoiceHandler({
    sendCodexRequest: async () => ({
      authMethod: "apiKey",
      authToken: "sk-test",
      requiresOpenaiAuth: false,
    }),
    readLocalAuthToken: async () => null,
    fetchImpl: async () => {
      fetchCalled = true;
      throw new Error("fetch should not run for API-key auth");
    },
  });

  handler.handleVoiceRequest(JSON.stringify({
    id: "voice-4",
    method: "voice/transcribe",
    params: {
      mimeType: "audio/wav",
      audioBase64: makeTestWavBase64(),
      sampleRateHz: 24_000,
      durationMs: 300,
    },
  }), (response) => {
    responses.push(JSON.parse(response));
  });

  await tick();

  assert.equal(fetchCalled, false);
  assert.equal(responses[0].error?.data?.errorCode, "not_chatgpt");
  assert.match(responses[0].error?.message || "", /requires a ChatGPT account/);
});

test("voice/transcribe returns a user-facing auth error when Mac auth is missing", async () => {
  const responses = [];
  const handler = createVoiceHandler({
    sendCodexRequest: async () => ({
      authMethod: null,
      authToken: null,
      requiresOpenaiAuth: true,
    }),
    readLocalAuthToken: async () => null,
    fetchImpl: async () => {
      throw new Error("fetch should not run");
    },
  });

  handler.handleVoiceRequest(JSON.stringify({
    id: "voice-3",
    method: "voice/transcribe",
    params: {
      mimeType: "audio/wav",
      audioBase64: makeTestWavBase64(),
      sampleRateHz: 24_000,
      durationMs: 300,
    },
  }), (response) => {
    responses.push(JSON.parse(response));
  });

  await tick();

  assert.equal(responses[0].error?.data?.errorCode, "not_authenticated");
  assert.match(responses[0].error?.message || "", /Sign in with ChatGPT/);
});

test("voice/transcribe rejects malformed or non-WAV audio before contacting the provider", async () => {
  const cases = [
    {
      name: "malformed base64",
      audioBase64: "%%%not-base64%%%",
      message: /could not be decoded/,
    },
    {
      name: "non-WAV payload",
      audioBase64: Buffer.from("hello from remodex").toString("base64"),
      message: /not a valid WAV file/,
    },
  ];

  for (const testCase of cases) {
    const responses = [];
    let authRequests = 0;
    let fetchCalls = 0;
    const handler = createVoiceHandler({
      sendCodexRequest: async () => {
        authRequests += 1;
        throw new Error("auth should not be requested for invalid audio");
      },
      fetchImpl: async () => {
        fetchCalls += 1;
        throw new Error("fetch should not run for invalid audio");
      },
    });

    handler.handleVoiceRequest(JSON.stringify({
      id: `voice-invalid-${testCase.name}`,
      method: "voice/transcribe",
      params: {
        mimeType: "audio/wav",
        audioBase64: testCase.audioBase64,
        sampleRateHz: 24_000,
        durationMs: 300,
      },
    }), (response) => {
      responses.push(JSON.parse(response));
    });

    await tick();

    assert.equal(authRequests, 0);
    assert.equal(fetchCalls, 0);
    assert.equal(responses[0].error?.data?.errorCode, "invalid_audio");
    assert.match(responses[0].error?.message || "", testCase.message);
  }
});

// ─── resolveVoiceAuth tests ─────────────────────────────────

const { resolveVoiceAuth } = require("../src/voice-handler");

test("resolveVoiceAuth returns token for ChatGPT sessions", async () => {
  const requests = [];
  const result = await resolveVoiceAuth(async (method, params) => {
    requests.push({ method, params });
    assert.equal(method, "getAuthStatus");
    return {
      authMethod: "chatgpt",
      authToken: "chatgpt-token-abc",
      requiresOpenaiAuth: false,
    };
  }, null, { readLocalAuthToken: async () => null });

  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: false },
    },
  ]);
  assert.deepEqual(result, { token: "chatgpt-token-abc" });
});

test("resolveVoiceAuth can force a refresh after the provider rejects the current token", async () => {
  const requests = [];
  const result = await resolveVoiceAuth(async (method, params) => {
    requests.push({ method, params });
    assert.equal(method, "getAuthStatus");
    return {
      authMethod: "chatgpt",
      authToken: requests.length === 1 ? "chatgpt-token-refreshed" : "unused-token",
      requiresOpenaiAuth: true,
    };
  }, { forceRefresh: true }, { readLocalAuthToken: async () => null });

  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: true },
    },
  ]);
  assert.deepEqual(result, { token: "chatgpt-token-refreshed" });
});

test("resolveVoiceAuth falls back to local auth when forced refresh omits the token", async () => {
  const requests = [];
  const result = await resolveVoiceAuth(async (method, params) => {
    requests.push({ method, params });
    assert.equal(method, "getAuthStatus");
    return {
      authMethod: "chatgpt",
      authToken: null,
      requiresOpenaiAuth: true,
    };
  }, { forceRefresh: true }, {
    readLocalAuthToken: async () => ({
      authMethod: "chatgpt",
      token: "chatgpt-token-local",
      isChatGPT: true,
      requiresOpenaiAuth: true,
    }),
  });

  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: true },
    },
  ]);
  assert.deepEqual(result, { token: "chatgpt-token-local" });
});

test("resolveVoiceAuth refreshes only after the current token is unavailable", async () => {
  const requests = [];
  const result = await resolveVoiceAuth(async (method, params) => {
    requests.push({ method, params });
    assert.equal(method, "getAuthStatus");
    if (requests.length === 1) {
      return {
        authMethod: "chatgpt",
        authToken: null,
        requiresOpenaiAuth: true,
      };
    }
    return {
      authMethod: "chatgpt",
      authToken: "chatgpt-token-refreshed",
      requiresOpenaiAuth: true,
    };
  }, null, { readLocalAuthToken: async () => null });

  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: false },
    },
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: true },
    },
  ]);
  assert.deepEqual(result, { token: "chatgpt-token-refreshed" });
});

test("resolveVoiceAuth rejects when no token is available regardless of requiresOpenaiAuth", async () => {
  const requests = [];
  await assert.rejects(
    () => resolveVoiceAuth(async (method, params) => {
      requests.push({ method, params });
      return {
        authMethod: null,
        authToken: null,
        requiresOpenaiAuth: true,
      };
    }, null, { readLocalAuthToken: async () => null }),
    (error) => {
      assert.equal(error.errorCode, "token_missing");
      return true;
    }
  );
  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: false },
    },
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: true },
    },
  ]);
});

test("resolveVoiceAuth rejects when Mac has no token", async () => {
  const requests = [];
  await assert.rejects(
    () => resolveVoiceAuth(async (method, params) => {
      requests.push({ method, params });
      return {
        authMethod: "chatgpt",
        authToken: null,
        requiresOpenaiAuth: false,
      };
    }, null, { readLocalAuthToken: async () => null }),
    (error) => {
      assert.match(error.message, /No ChatGPT session token/);
      assert.equal(error.errorCode, "token_missing");
      return true;
    }
  );
  assert.deepEqual(requests, [
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: false },
    },
    {
      method: "getAuthStatus",
      params: { includeToken: true, refreshToken: true },
    },
  ]);
});

function makeJWT(payload) {
  const header = base64UrlEncode({ alg: "none", typ: "JWT" });
  const body = base64UrlEncode(payload);
  return `${header}.${body}.signature`;
}

function makeTestWavBase64() {
  const wav = Buffer.alloc(46);
  wav.write("RIFF", 0, "ascii");
  wav.writeUInt32LE(38, 4);
  wav.write("WAVE", 8, "ascii");
  wav.write("fmt ", 12, "ascii");
  wav.writeUInt32LE(16, 16);
  wav.writeUInt16LE(1, 20);
  wav.writeUInt16LE(1, 22);
  wav.writeUInt32LE(24_000, 24);
  wav.writeUInt32LE(48_000, 28);
  wav.writeUInt16LE(2, 32);
  wav.writeUInt16LE(16, 34);
  wav.write("data", 36, "ascii");
  wav.writeUInt32LE(2, 40);
  wav.writeInt16LE(0, 44);
  return wav.toString("base64");
}

function base64UrlEncode(value) {
  return Buffer.from(JSON.stringify(value))
    .toString("base64")
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/g, "");
}

function tick() {
  return new Promise((resolve) => setTimeout(resolve, 0));
}
