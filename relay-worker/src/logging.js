import { normalizeNonEmptyString } from "./common.js";

export function logRelayInfo(message) {
  console.log(`[relay] ${message}`);
}

export function logRelayError(message) {
  console.error(`[relay] ${message}`);
}

export function relaySessionLogLabel(sessionId) {
  const normalizedSessionId = normalizeNonEmptyString(sessionId);
  if (!normalizedSessionId) {
    return "session=[redacted]";
  }

  return `session#${hashLabel(normalizedSessionId)}`;
}

export function relayDeviceLogLabel(prefix, deviceId) {
  const normalizedPrefix = normalizeNonEmptyString(prefix) || "device";
  const normalizedDeviceId = normalizeNonEmptyString(deviceId);
  if (!normalizedDeviceId) {
    return `${normalizedPrefix}=[redacted]`;
  }

  return `${normalizedPrefix}#${hashLabel(normalizedDeviceId)}`;
}

export function redactRelayPathname(pathname) {
  if (typeof pathname !== "string" || !pathname.startsWith("/relay/")) {
    return pathname || "/";
  }

  const [, relayPrefix, ...rest] = pathname.split("/");
  const suffix = rest.length > 1 ? `/${rest.slice(1).join("/")}` : "";
  return `/${relayPrefix}/[session]${suffix}`;
}

function hashLabel(value) {
  let hash = 2166136261;
  for (const byte of new TextEncoder().encode(value)) {
    hash ^= byte;
    hash = Math.imul(hash, 16777619);
  }
  return (hash >>> 0).toString(16).padStart(8, "0");
}
