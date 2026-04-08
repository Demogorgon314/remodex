import { DurableObject } from "cloudflare:workers";
import {
  CLOSE_CODE_INVALID_SESSION,
  CLOSE_CODE_MAC_ABSENCE_BUFFER_FULL,
  bindSessionId,
  closeConnection,
  connectIphone,
  connectMac,
  createSessionSnapshot,
  expireMacAbsenceIfNeeded,
  hasActiveMacSession,
  reconcileSnapshotWithConnections,
  readMacRegistrationHeaders,
  updateMacRegistration,
} from "./session/state.js";
import { classifyRelayTrafficLabel } from "./health/state.js";
import {
  createHTTPError,
  isWebSocketUpgrade,
  jsonResponse,
  normalizeNonEmptyString,
  normalizeWireMessage,
  readJSONBody,
  safeParseJSON,
} from "./common.js";
import {
  logRelayError,
  logRelayInfo,
  relayDeviceLogLabel,
  relaySessionLogLabel,
} from "./logging.js";

const SNAPSHOT_KEY = "sessionSnapshot";
// Health stats are best-effort observability, so batch them more aggressively
// to keep `wrangler tail` readable during active relay sessions.
const PENDING_TRAFFIC_FLUSH_INTERVAL_MS = 30_000;
const PENDING_TRAFFIC_FLUSH_THRESHOLD = 256;

export class SessionRelayDurableObject extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.ctx = ctx;
    this.env = env;
    this.snapshot = createSessionSnapshot();
    this.pendingTraffic = createPendingTrafficState();
    this.ready = this.ctx.blockConcurrencyWhile(async () => {
      const storedSnapshot = await this.ctx.storage.get(SNAPSHOT_KEY);
      this.snapshot = createSessionSnapshot(storedSnapshot);
    });
  }

  async fetch(request) {
    await this.ready;
    await this.reconcileSnapshotWithSockets();

    const url = new URL(request.url);
    const pathname = url.pathname;
    if (request.method === "GET" && pathname === "/internal/status") {
      const inventory = this.socketInventory();
      return jsonResponse(200, {
        ok: true,
        sessionId: this.snapshot.sessionId,
        hasActiveMac: inventory.mac.hasActiveConnection,
        hasConnectedIphone: inventory.iphone.hasActiveConnection,
        macSocketCount: inventory.mac.totalCount,
        iphoneSocketCount: inventory.iphone.totalCount,
        staleMacSocketCount: inventory.mac.staleCount,
        staleIphoneSocketCount: inventory.iphone.staleCount,
      });
    }

    if (!isWebSocketUpgrade(request)) {
      return jsonResponse(404, {
        ok: false,
        error: "Not found",
      });
    }

    const match = pathname.match(/^\/relay\/([^/?]+)/);
    const sessionId = normalizeNonEmptyString(match?.[1]);
    const role = normalizeNonEmptyString(request.headers.get("x-role"));
    if (!sessionId || (role !== "mac" && role !== "iphone")) {
      throw createHTTPError(400, "invalid_upgrade", "Missing sessionId or invalid x-role header.");
    }

    this.snapshot = bindSessionId(this.snapshot, sessionId);
    const webSocketPair = new WebSocketPair();
    const [client, server] = Object.values(webSocketPair);
    const connectionId = crypto.randomUUID();

    server.serializeAttachment({
      role,
      connectionId,
      sessionId,
    });
    this.ctx.acceptWebSocket(server, [role]);

    let result;
    if (role === "mac") {
      result = connectMac(this.snapshot, {
        connectionId,
        registration: readMacRegistrationHeaders(request.headers, sessionId),
        notificationSecret: request.headers.get("x-notification-secret"),
      });
    } else {
      result = connectIphone(this.snapshot, {
        connectionId,
      });
    }

    await this.commit(result.snapshot, result.effects);
    await this.syncHealthSummary();
    if (result.reject) {
      logRelayInfo(
        `rejecting ${role} connection -> ${relaySessionLogLabel(sessionId)} `
        + `reason="${result.reject.reason}"`
      );
      server.close(result.reject.code, result.reject.reason);
    } else if (role === "mac") {
      logRelayInfo(`Mac connected -> ${relaySessionLogLabel(sessionId)}`);
    } else {
      logRelayInfo(
        `iPhone connected -> ${relaySessionLogLabel(sessionId)} `
        + `(${this.snapshot.iphoneConnectionId ? 1 : 0} client(s))`
      );
    }

    return new Response(null, {
      status: 101,
      webSocket: client,
    });
  }

  async webSocketMessage(ws, message) {
    await this.ready;
    const attachment = ws.deserializeAttachment() || {};
    const role = attachment.role;
    if (role !== "mac" && role !== "iphone") {
      ws.close(CLOSE_CODE_INVALID_SESSION, "Missing sessionId or invalid x-role header");
      return;
    }

    await this.reconcileSnapshotWithSockets();
    if (!this.isActiveConnection(role, attachment.connectionId)) {
      return;
    }

    const normalizedMessage = normalizeWireMessage(message);
    if (role === "mac") {
      const parsed = safeParseJSON(normalizedMessage);
      if (parsed?.kind === "relayMacRegistration" && typeof parsed.registration === "object") {
        const result = updateMacRegistration(this.snapshot, {
          ...parsed.registration,
          sessionId: this.snapshot.sessionId,
        });
        await this.commit(result.snapshot, result.effects);
        await this.flushPendingTrafficIfDue({ force: true });
        if (this.snapshot.registration?.macDeviceId) {
          logRelayInfo(
            `Mac registration updated -> ${relaySessionLogLabel(this.snapshot.sessionId)} `
            + `${relayDeviceLogLabel("mac", this.snapshot.registration.macDeviceId)}`
          );
        }
        return;
      }

      this.activeSocketForRole("iphone")?.send(normalizedMessage);
      await this.recordTraffic("macToIphone", normalizedMessage);
      return;
    }

    const macSocket = this.activeSocketForRole("mac");
    if (!macSocket || !hasActiveMacSession(this.snapshot)) {
      ws.close(CLOSE_CODE_MAC_ABSENCE_BUFFER_FULL, "Mac temporarily unavailable");
      return;
    }

    macSocket.send(normalizedMessage);
    await this.recordTraffic("iphoneToMac", normalizedMessage);
  }

  async webSocketClose(ws) {
    await this.ready;
    await this.reconcileSnapshotWithSockets();
    const attachment = ws.deserializeAttachment() || {};
    const result = closeConnection(this.snapshot, {
      role: attachment.role,
      connectionId: attachment.connectionId,
    });
    await this.commit(result.snapshot, result.effects);
    await this.flushPendingTrafficIfDue({ force: true });
    await this.syncHealthSummary();
    if (attachment.role === "mac") {
      logRelayInfo(`Mac disconnected -> ${relaySessionLogLabel(this.snapshot.sessionId)}`);
    } else if (attachment.role === "iphone") {
      logRelayInfo(
        `iPhone disconnected -> ${relaySessionLogLabel(this.snapshot.sessionId)} `
        + `(${this.snapshot.iphoneConnectionId ? 1 : 0} remaining)`
      );
    }
  }

  async alarm() {
    await this.ready;
    await this.reconcileSnapshotWithSockets();
    const hadIphone = Boolean(this.snapshot.iphoneConnectionId);
    const sessionLabel = relaySessionLogLabel(this.snapshot.sessionId);
    await this.flushPendingTrafficIfDue({ force: true });
    await this.syncHealthSummary();
    if (hadIphone && !this.snapshot.iphoneConnectionId) {
      logRelayInfo(`Mac absence grace expired -> ${sessionLabel}`);
    }
    if (!this.snapshot.macConnectionId && !this.snapshot.iphoneConnectionId) {
      logRelayInfo(`${sessionLabel} cleaned up`);
    }
  }

  async webSocketError(ws, error) {
    await this.ready;
    const attachment = ws.deserializeAttachment() || {};
    logRelayError(
      `WebSocket error (${attachment.role || "unknown"}, `
      + `${relaySessionLogLabel(attachment.sessionId || this.snapshot.sessionId)}): `
      + `${error?.message || error || "unknown error"}`
    );
  }

  async commit(nextSnapshot, effects = []) {
    this.snapshot = createSessionSnapshot(nextSnapshot);
    await this.ctx.storage.put(SNAPSHOT_KEY, this.snapshot);
    await this.runEffects(effects);
  }

  async runEffects(effects) {
    for (const effect of effects) {
      switch (effect.type) {
        case "close_role":
          this.closeRoleSockets(effect);
          break;
        case "registry_upsert":
          await this.upsertRegistry(effect.registration);
          break;
        case "registry_remove":
          await this.removeRegistry(effect.macDeviceId, effect.sessionId);
          break;
        case "set_alarm":
          await this.ctx.storage.setAlarm(effect.scheduledTime);
          break;
        case "delete_alarm":
          await this.ctx.storage.deleteAlarm();
          break;
        default:
          break;
      }
    }
  }

  closeRoleSockets({
    role,
    exceptConnectionId = "",
    code,
    reason,
  }) {
    let closedCount = 0;
    for (const socket of this.ctx.getWebSockets(role)) {
      const attachment = socket.deserializeAttachment() || {};
      if (attachment.connectionId === exceptConnectionId) {
        continue;
      }
      socket.close(code, reason);
      closedCount += 1;
    }

    if (closedCount > 0 && role === "iphone") {
      logRelayInfo(
        `Replaced older iPhone connection(s) -> ${relaySessionLogLabel(this.snapshot.sessionId)} `
        + `(${closedCount} closed)`
      );
    }
    if (closedCount > 0 && role === "mac") {
      logRelayInfo(
        `Replaced older Mac connection(s) -> ${relaySessionLogLabel(this.snapshot.sessionId)} `
        + `(${closedCount} closed)`
      );
    }
  }

  async upsertRegistry(registration) {
    const macDeviceId = normalizeNonEmptyString(registration?.macDeviceId);
    if (!macDeviceId) {
      return;
    }

    const stub = this.env.TRUSTED_REGISTRY_DO.get(
      this.env.TRUSTED_REGISTRY_DO.idFromName(macDeviceId)
    );
    await stub.fetch("https://registry.internal/internal/upsert", {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        registration,
      }),
    });
  }

  async removeRegistry(macDeviceId, sessionId) {
    const normalizedMacDeviceId = normalizeNonEmptyString(macDeviceId);
    if (!normalizedMacDeviceId) {
      return;
    }

    const stub = this.env.TRUSTED_REGISTRY_DO.get(
      this.env.TRUSTED_REGISTRY_DO.idFromName(normalizedMacDeviceId)
    );
    await stub.fetch("https://registry.internal/internal/remove", {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        sessionId,
      }),
    });
  }

  async syncHealthSummary() {
    const inventory = this.socketInventory();
    const stub = this.env.HEALTH_STATS_DO.get(this.env.HEALTH_STATS_DO.idFromName("global"));
    await stub.fetch("https://health.internal/internal/session-summary", {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        sessionId: this.snapshot.sessionId,
        hasMac: inventory.mac.hasActiveConnection,
        clientCount: inventory.iphone.hasActiveConnection ? 1 : 0,
        macSocketCount: inventory.mac.totalCount,
        iphoneSocketCount: inventory.iphone.totalCount,
        staleMacSocketCount: inventory.mac.staleCount,
        staleIphoneSocketCount: inventory.iphone.staleCount,
      }),
    });
  }

  isActiveConnection(role, connectionId) {
    const normalizedRole = normalizeNonEmptyString(role);
    const normalizedConnectionId = normalizeNonEmptyString(connectionId);
    if (!normalizedRole || !normalizedConnectionId) {
      return false;
    }

    if (normalizedRole === "mac") {
      return this.snapshot.macConnectionId === normalizedConnectionId;
    }

    if (normalizedRole === "iphone") {
      return this.snapshot.iphoneConnectionId === normalizedConnectionId;
    }

    return false;
  }

  activeSocketForRole(role) {
    const normalizedRole = normalizeNonEmptyString(role);
    if (!normalizedRole) {
      return null;
    }

    const activeConnectionId = normalizedRole === "mac"
      ? this.snapshot.macConnectionId
      : this.snapshot.iphoneConnectionId;
    if (!activeConnectionId) {
      return null;
    }

    for (const socket of this.ctx.getWebSockets(normalizedRole)) {
      const attachment = socket.deserializeAttachment() || {};
      if (attachment.connectionId === activeConnectionId) {
        return socket;
      }
    }

    return null;
  }

  countRoleSockets(role) {
    const normalizedRole = normalizeNonEmptyString(role);
    if (!normalizedRole) {
      return 0;
    }

    return this.ctx.getWebSockets(normalizedRole).length;
  }

  socketInventory() {
    const mac = this.roleSocketInventory("mac", this.snapshot.macConnectionId);
    const iphone = this.roleSocketInventory("iphone", this.snapshot.iphoneConnectionId);
    return { mac, iphone };
  }

  roleSocketInventory(role, activeConnectionId) {
    const sockets = this.ctx.getWebSockets(role);
    const normalizedActiveConnectionId = normalizeNonEmptyString(activeConnectionId);
    let activeCount = 0;
    let staleCount = 0;

    for (const socket of sockets) {
      const attachment = socket.deserializeAttachment() || {};
      if (
        normalizedActiveConnectionId
        && attachment.connectionId === normalizedActiveConnectionId
      ) {
        activeCount += 1;
      } else {
        staleCount += 1;
      }
    }

    return {
      totalCount: sockets.length,
      activeCount,
      staleCount,
      hasActiveConnection: activeCount > 0,
    };
  }

  async reconcileSnapshotWithSockets(now = Date.now()) {
    const result = reconcileSnapshotWithConnections(this.snapshot, {
      macConnectionIds: this.socketConnectionIds("mac"),
      iphoneConnectionIds: this.socketConnectionIds("iphone"),
      now,
    });

    if (JSON.stringify(result.snapshot) === JSON.stringify(this.snapshot) && result.effects.length === 0) {
      return;
    }

    await this.commit(result.snapshot, result.effects);
  }

  socketConnectionIds(role) {
    return this.ctx.getWebSockets(role)
      .map((socket) => normalizeNonEmptyString(socket.deserializeAttachment()?.connectionId))
      .filter(Boolean);
  }

  async recordTraffic(channel, message) {
    const normalizedChannel = normalizeNonEmptyString(channel);
    if (!normalizedChannel) {
      return;
    }

    const label = classifyRelayTrafficLabel(message);
    const bytes = byteLengthOfRelayTraffic(message);
    if (bytes <= 0) {
      return;
    }

    this.pendingTraffic = addPendingTrafficRecord(this.pendingTraffic, {
      channel: normalizedChannel,
      label,
      bytes,
    });
    await this.flushPendingTrafficIfDue();
  }

  async flushPendingTrafficIfDue({ force = false } = {}) {
    if (!this.pendingTraffic.totalMessages) {
      return;
    }

    const now = Date.now();
    const shouldFlush = force
      || this.pendingTraffic.totalMessages >= PENDING_TRAFFIC_FLUSH_THRESHOLD
      || now - this.pendingTraffic.startedAt >= PENDING_TRAFFIC_FLUSH_INTERVAL_MS;
    if (!shouldFlush) {
      return;
    }

    const payload = this.pendingTraffic;
    this.pendingTraffic = createPendingTrafficState();

    const stub = this.env.HEALTH_STATS_DO.get(this.env.HEALTH_STATS_DO.idFromName("global"));
    await stub.fetch("https://health.internal/internal/traffic-batch", {
      method: "POST",
      headers: {
        "content-type": "application/json",
      },
      body: JSON.stringify({
        channels: payload.channels,
      }),
    });
  }
}

function createPendingTrafficState(now = Date.now()) {
  return {
    startedAt: now,
    totalMessages: 0,
    channels: {},
  };
}

function addPendingTrafficRecord(state, { channel, label, bytes }) {
  const nextState = {
    startedAt: state?.startedAt || Date.now(),
    totalMessages: Number(state?.totalMessages || 0),
    channels: structuredClone(state?.channels || {}),
  };
  nextState.totalMessages += 1;

  const nextChannel = nextState.channels[channel] || {
    messages: 0,
    bytes: 0,
    labels: {},
  };
  nextChannel.messages += 1;
  nextChannel.bytes += bytes;

  const nextLabel = nextChannel.labels[label] || { messages: 0, bytes: 0 };
  nextLabel.messages += 1;
  nextLabel.bytes += bytes;
  nextChannel.labels[label] = nextLabel;

  nextState.channels[channel] = nextChannel;
  return nextState;
}

function byteLengthOfRelayTraffic(value) {
  if (typeof value === "string") {
    return new TextEncoder().encode(value).length;
  }

  if (value instanceof ArrayBuffer) {
    return value.byteLength;
  }

  if (ArrayBuffer.isView(value)) {
    return value.byteLength;
  }

  if (value == null) {
    return 0;
  }

  try {
    return new TextEncoder().encode(JSON.stringify(value)).length;
  } catch {
    return 0;
  }
}
