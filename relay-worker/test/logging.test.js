import test from "node:test";
import assert from "node:assert/strict";

import {
  redactRelayPathname,
  relayDeviceLogLabel,
  relaySessionLogLabel,
} from "../src/logging.js";

test("relay logging redacts session path segments", () => {
  assert.equal(redactRelayPathname("/relay/session-123"), "/relay/[session]");
  assert.equal(redactRelayPathname("/relay/session-123/extra"), "/relay/[session]/extra");
  assert.equal(redactRelayPathname("/health"), "/health");
});

test("relay session log labels hash the raw session id", () => {
  const label = relaySessionLogLabel("session-sensitive");
  assert.match(label, /^session#[0-9a-f]{8}$/);
  assert.equal(label.includes("session-sensitive"), false);
});

test("relay device log labels hash the raw device id", () => {
  const label = relayDeviceLogLabel("mac", "mac-device-sensitive");
  assert.match(label, /^mac#[0-9a-f]{8}$/);
  assert.equal(label.includes("mac-device-sensitive"), false);
});
