import { DurableObject } from "cloudflare:workers";
import { jsonResponse, readJSONBody } from "./common.js";
import {
  buildRelayHealthStats,
  classifyRelayTrafficLabel,
  createHealthSnapshot,
  mergeTrafficBatch,
  recordTraffic,
  updateSessionSummary,
} from "./health/state.js";

const SNAPSHOT_KEY = "healthSnapshot";

export class HealthStatsDurableObject extends DurableObject {
  constructor(ctx, env) {
    super(ctx, env);
    this.ctx = ctx;
    this.env = env;
    this.snapshot = createHealthSnapshot();
    this.ready = this.ctx.blockConcurrencyWhile(async () => {
      const storedSnapshot = await this.ctx.storage.get(SNAPSHOT_KEY);
      this.snapshot = createHealthSnapshot(storedSnapshot);
    });
  }

  async fetch(request) {
    await this.ready;
    const url = new URL(request.url);

    if (request.method === "GET" && url.pathname === "/internal/health") {
      return jsonResponse(200, {
        ok: true,
        relay: buildRelayHealthStats(this.snapshot),
        push: {
          enabled: false,
          registeredSessions: 0,
          deliveredDedupeKeys: 0,
          apnsConfigured: false,
          fcmConfigured: false,
        },
      });
    }

    if (request.method === "POST" && url.pathname === "/internal/session-summary") {
      const body = await readJSONBody(request);
      this.snapshot = updateSessionSummary(this.snapshot, body);
      await this.ctx.storage.put(SNAPSHOT_KEY, this.snapshot);
      return jsonResponse(200, { ok: true });
    }

    if (request.method === "POST" && url.pathname === "/internal/traffic-record") {
      const body = await readJSONBody(request);
      this.snapshot = recordTraffic(this.snapshot, {
        channel: body.channel,
        message: body.message,
        label: body.label || classifyRelayTrafficLabel(body.message),
      });
      await this.ctx.storage.put(SNAPSHOT_KEY, this.snapshot);
      return jsonResponse(200, { ok: true });
    }

    if (request.method === "POST" && url.pathname === "/internal/traffic-batch") {
      const body = await readJSONBody(request);
      this.snapshot = mergeTrafficBatch(this.snapshot, {
        channels: body.channels,
      });
      await this.ctx.storage.put(SNAPSHOT_KEY, this.snapshot);
      return jsonResponse(200, { ok: true });
    }

    return jsonResponse(404, {
      ok: false,
      error: "Not found",
    });
  }
}
