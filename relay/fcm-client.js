// FILE: fcm-client.js
// Purpose: Sends FCM push notifications for relay-hosted Remodex sessions using the HTTP v1 API.
// Layer: Hosted service helper
// Exports: createFCMClient
// Depends on: fs, google-auth-library

const fs = require("fs");
const { GoogleAuth } = require("google-auth-library");

const FCM_SCOPE = "https://www.googleapis.com/auth/firebase.messaging";
const DEFAULT_FCM_TIMEOUT_MS = 10_000;

function createFCMClient({
  projectId = "",
  credentialsFile = "",
  credentialsJSON = "",
  googleAuth = createGoogleAuth({
    projectId,
    credentialsFile,
    credentialsJSON,
  }),
  fetchImpl = globalThis.fetch,
  requestTimeoutMs = DEFAULT_FCM_TIMEOUT_MS,
} = {}) {
  const explicitProjectId = normalizeString(projectId) || projectIdFromCredentials(credentialsJSON);
  const hasExplicitCredentials = Boolean(
    normalizeString(credentialsFile)
      || normalizeString(credentialsJSON)
      || normalizeString(process.env.GOOGLE_APPLICATION_CREDENTIALS)
  );
  let resolvedProjectId = explicitProjectId;
  let authClientPromise = null;

  function isConfigured() {
    return Boolean(
      typeof fetchImpl === "function"
        && (
          resolvedProjectId
          || hasExplicitCredentials
          || normalizeString(process.env.GOOGLE_CLOUD_PROJECT)
        )
    );
  }

  async function sendNotification({
    deviceToken,
    title,
    body,
    payload = {},
  } = {}) {
    if (!isConfigured()) {
      throw fcmError("fcm_not_configured", "FCM credentials are not configured.", 503);
    }

    const normalizedDeviceToken = normalizeString(deviceToken);
    if (!normalizedDeviceToken) {
      throw fcmError("invalid_device_token", "A valid FCM device token is required.", 400);
    }

    const targetProjectId = await resolveProjectId();
    if (!targetProjectId) {
      throw fcmError("fcm_project_id_missing", "FCM project ID is not configured.", 503);
    }

    const accessToken = await resolveAccessToken();
    const response = await fetchWithTimeout(
      fetchImpl,
      `https://fcm.googleapis.com/v1/projects/${targetProjectId}/messages:send`,
      {
        method: "POST",
        headers: {
          authorization: `Bearer ${accessToken}`,
          "content-type": "application/json; UTF-8",
        },
        body: JSON.stringify({
          message: {
            token: normalizedDeviceToken,
            data: stringifyPayload(payload),
            android: {
              priority: "high",
              ttl: "30s",
            },
            notification: {
              title: normalizeString(title) || "Conversation",
              body: normalizeString(body) || "Response ready",
            },
          },
        }),
      },
      requestTimeoutMs
    );

    const responseText = await response.text();
    const parsed = safeParseJSON(responseText);
    if (!response.ok) {
      const message = parsed?.error?.message || parsed?.error || responseText || `HTTP ${response.status}`;
      throw fcmError("fcm_request_failed", message, response.status);
    }

    return parsed ?? { ok: true };
  }

  async function resolveProjectId() {
    if (resolvedProjectId) {
      return resolvedProjectId;
    }
    const googleCloudProject = normalizeString(process.env.GOOGLE_CLOUD_PROJECT);
    if (googleCloudProject) {
      resolvedProjectId = googleCloudProject;
      return resolvedProjectId;
    }
    resolvedProjectId = normalizeString(await googleAuth.getProjectId());
    return resolvedProjectId;
  }

  async function resolveAccessToken() {
    const authClient = await resolveAuthClient();
    const tokenValue = await authClient.getAccessToken();
    const resolvedToken = typeof tokenValue === "string" ? tokenValue : tokenValue?.token;
    if (!resolvedToken) {
      throw fcmError("fcm_access_token_missing", "Unable to mint an FCM access token.", 503);
    }
    return resolvedToken;
  }

  async function resolveAuthClient() {
    if (!authClientPromise) {
      authClientPromise = Promise.resolve(googleAuth.getClient());
    }
    return authClientPromise;
  }

  return {
    isConfigured,
    sendNotification,
  };
}

function createGoogleAuth({
  projectId,
  credentialsFile,
  credentialsJSON,
} = {}) {
  const options = {
    scopes: [FCM_SCOPE],
  };
  const normalizedProjectId = normalizeString(projectId);
  if (normalizedProjectId) {
    options.projectId = normalizedProjectId;
  }
  const normalizedCredentialsFile = normalizeString(credentialsFile);
  if (normalizedCredentialsFile) {
    options.keyFile = normalizedCredentialsFile;
  }
  const parsedCredentials = parseCredentials(credentialsJSON);
  if (parsedCredentials) {
    options.credentials = parsedCredentials;
    options.projectId = options.projectId || normalizeString(parsedCredentials.project_id);
  }
  return new GoogleAuth(options);
}

async function fetchWithTimeout(fetchImpl, url, options, timeoutMs) {
  const controller = typeof AbortController === "function" && timeoutMs > 0
    ? new AbortController()
    : null;
  const timeoutID = controller
    ? setTimeout(() => {
      controller.abort(createTimeoutAbortError(timeoutMs));
    }, timeoutMs)
    : null;

  try {
    return await fetchImpl(url, {
      ...options,
      signal: controller?.signal,
    });
  } catch (error) {
    if (isAbortError(error)) {
      throw fcmError(
        "fcm_request_timeout",
        `FCM request timed out after ${timeoutMs}ms.`,
        504
      );
    }
    throw error;
  } finally {
    if (timeoutID) {
      clearTimeout(timeoutID);
    }
  }
}

function parseCredentials(value) {
  const normalized = normalizeString(value);
  if (!normalized) {
    return null;
  }
  try {
    if (normalized.startsWith("{")) {
      return JSON.parse(normalized);
    }
    return JSON.parse(fs.readFileSync(normalized, "utf8"));
  } catch (error) {
    throw fcmError(
      "invalid_fcm_credentials",
      `FCM service account credentials could not be loaded: ${error.message}`,
      500
    );
  }
}

function projectIdFromCredentials(value) {
  return normalizeString(parseCredentials(value)?.project_id);
}

function stringifyPayload(payload) {
  return Object.fromEntries(
    Object.entries(payload || {}).map(([key, value]) => [key, stringifyDataValue(value)])
  );
}

function stringifyDataValue(value) {
  if (value == null) {
    return "";
  }
  if (typeof value === "string") {
    return value;
  }
  return String(value);
}

function safeParseJSON(value) {
  if (!value || typeof value !== "string") {
    return null;
  }
  try {
    return JSON.parse(value);
  } catch {
    return null;
  }
}

function createTimeoutAbortError(timeoutMs) {
  const error = new Error(`FCM request timed out after ${timeoutMs}ms.`);
  error.name = "AbortError";
  return error;
}

function isAbortError(error) {
  return error?.name === "AbortError" || error?.code === "ABORT_ERR";
}

function normalizeString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : "";
}

function fcmError(code, message, status) {
  const error = new Error(message);
  error.code = code;
  error.status = status;
  return error;
}

module.exports = {
  createFCMClient,
};
