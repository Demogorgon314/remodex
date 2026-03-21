// FILE: notifications-handler.js
// Purpose: Intercepts notifications/push/* bridge RPCs and forwards device registration to the configured push service.
// Layer: Bridge handler
// Exports: createNotificationsHandler
// Depends on: none

function createNotificationsHandler({ pushServiceClient, logPrefix = "[remodex]" } = {}) {
  function handleNotificationsRequest(rawMessage, sendResponse) {
    let parsed;
    try {
      parsed = JSON.parse(rawMessage);
    } catch {
      return false;
    }

    const method = typeof parsed?.method === "string" ? parsed.method.trim() : "";
    if (method !== "notifications/push/register") {
      return false;
    }

    const id = parsed.id;
    const params = parsed.params || {};

    handleNotificationsMethod(method, params)
      .then((result) => {
        sendResponse(JSON.stringify({ id, result }));
      })
      .catch((error) => {
        console.error(`${logPrefix} push registration failed: ${error.message}`);
        sendResponse(JSON.stringify({
          id,
          error: {
            code: -32000,
            message: error.userMessage || error.message || "Push registration failed.",
            data: {
              errorCode: error.errorCode || "push_registration_failed",
            },
          },
        }));
      });

    return true;
  }

  async function handleNotificationsMethod(method, params) {
    if (!pushServiceClient?.hasConfiguredBaseUrl) {
      return { ok: false, skipped: true };
    }

    const deviceToken = readString(params.deviceToken);
    const alertsEnabled = Boolean(params.alertsEnabled);
    const authorizationStatus = readAuthorizationStatus(params.authorizationStatus);
    const platform = readPushPlatform(params.platform);
    const pushProvider = readPushProvider(params.pushProvider, platform);
    const appEnvironment = readAppEnvironment(params.appEnvironment);
    if (!deviceToken) {
      throw notificationsError(
        "missing_device_token",
        "notifications/push/register requires a deviceToken."
      );
    }

    await pushServiceClient.registerDevice({
      deviceToken,
      alertsEnabled,
      authorizationStatus,
      appEnvironment,
      platform,
      pushProvider,
    });

    return {
      ok: true,
      alertsEnabled,
      authorizationStatus,
      appEnvironment,
      platform,
      pushProvider,
    };
  }

  return {
    handleNotificationsRequest,
  };
}

function readString(value) {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function readAuthorizationStatus(value) {
  switch (value) {
    case "notDetermined":
    case "denied":
    case "authorized":
      return value;
    default:
      return "unknown";
  }
}

function readPushPlatform(value) {
  return value === "android" ? "android" : "ios";
}

function readPushProvider(value, platform) {
  if (value === "fcm") {
    return "fcm";
  }
  if (value === "apns") {
    return "apns";
  }
  return platform === "android" ? "fcm" : "apns";
}

function readAppEnvironment(value) {
  return value === "development" ? "development" : "production";
}

function notificationsError(errorCode, userMessage) {
  const error = new Error(userMessage);
  error.errorCode = errorCode;
  error.userMessage = userMessage;
  return error;
}

module.exports = {
  createNotificationsHandler,
};
