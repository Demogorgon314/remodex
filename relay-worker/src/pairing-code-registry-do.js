import { DurableObject } from "cloudflare:workers";
import { createHTTPError, jsonResponse, normalizeNonEmptyString, readJSONBody } from "./common.js";
import { normalizeShortPairingCode } from "./protocol/trusted-session.js";

const SNAPSHOT_KEY = "pairingCodes";

export class PairingCodeRegistryDurableObject extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.ctx = ctx;
    this.env = env;
    this.pairingCodes = {};
    this.ready = this.ctx.blockConcurrencyWhile(async () => {
      this.pairingCodes = (await this.ctx.storage.get(SNAPSHOT_KEY)) || {};
    });
  }

  async fetch(request) {
    await this.ready;
    const url = new URL(request.url);
    if (request.method === "POST" && url.pathname === "/internal/upsert") {
      const body = await readJSONBody(request);
      await this.upsert(body.registration);
      return jsonResponse(200, { ok: true });
    }
    if (request.method === "POST" && url.pathname === "/internal/remove") {
      const body = await readJSONBody(request);
      await this.remove(body.code, body.sessionId);
      return jsonResponse(200, { ok: true });
    }
    if (request.method === "POST" && url.pathname === "/v1/pairing/code/resolve") {
      const body = await readJSONBody(request);
      return jsonResponse(200, await this.resolve(body.code));
    }
    return jsonResponse(404, { ok: false, error: "Not found" });
  }

  async upsert(registration) {
    const code = normalizeShortPairingCode(registration?.pairingCode);
    if (!code) {
      return;
    }
    this.pairingCodes[code] = {
      sessionId: normalizeNonEmptyString(registration?.sessionId),
      macDeviceId: normalizeNonEmptyString(registration?.macDeviceId),
      macIdentityPublicKey: normalizeNonEmptyString(registration?.macIdentityPublicKey),
      pairingVersion: Number(registration?.pairingVersion),
      pairingExpiresAt: Number(registration?.pairingExpiresAt),
    };
    await this.ctx.storage.put(SNAPSHOT_KEY, this.pairingCodes);
  }

  async remove(code, sessionId) {
    const normalizedCode = normalizeShortPairingCode(code);
    if (!normalizedCode) {
      return;
    }
    const existing = this.pairingCodes[normalizedCode];
    if (!existing || (sessionId && existing.sessionId !== normalizeNonEmptyString(sessionId))) {
      return;
    }
    delete this.pairingCodes[normalizedCode];
    await this.ctx.storage.put(SNAPSHOT_KEY, this.pairingCodes);
  }

  async resolve(code) {
    const normalizedCode = normalizeShortPairingCode(code);
    if (!normalizedCode) {
      throw createHTTPError(400, "invalid_request", "The pairing code is missing or malformed.");
    }
    const registration = this.pairingCodes[normalizedCode];
    if (!registration) {
      throw createHTTPError(404, "pairing_code_unavailable", "This pairing code is unavailable.");
    }
    if (!Number.isFinite(registration.pairingExpiresAt) || Date.now() > registration.pairingExpiresAt) {
      delete this.pairingCodes[normalizedCode];
      await this.ctx.storage.put(SNAPSHOT_KEY, this.pairingCodes);
      throw createHTTPError(410, "pairing_code_expired", "This pairing code has expired.");
    }
    const isActive = await this.hasActiveMacSession(registration.sessionId);
    if (!isActive) {
      throw createHTTPError(404, "pairing_code_unavailable", "This pairing code is unavailable.");
    }
    if (!registration.macDeviceId || !registration.macIdentityPublicKey || !Number.isFinite(registration.pairingVersion)) {
      throw createHTTPError(409, "pairing_code_incomplete", "The bridge pairing metadata is incomplete.");
    }
    return {
      ok: true,
      v: registration.pairingVersion,
      sessionId: registration.sessionId,
      macDeviceId: registration.macDeviceId,
      macIdentityPublicKey: registration.macIdentityPublicKey,
      expiresAt: registration.pairingExpiresAt,
    };
  }

  async hasActiveMacSession(sessionId) {
    const normalizedSessionId = normalizeNonEmptyString(sessionId);
    if (!normalizedSessionId) {
      return false;
    }
    const stub = this.env.SESSION_RELAY_DO.get(this.env.SESSION_RELAY_DO.idFromName(normalizedSessionId));
    const response = await stub.fetch("https://session.internal/internal/status");
    const body = await response.json();
    return Boolean(body?.hasActiveMac);
  }
}
