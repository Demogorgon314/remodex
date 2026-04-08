import { normalizeNonEmptyString, safeParseJSON } from "../common.js";

const RELAY_TRAFFIC_TOP_LABEL_LIMIT = 6;

export function createHealthSnapshot({
  startedAt = Date.now(),
  updatedAt = startedAt,
  sessions = {},
  traffic = {},
} = {}) {
  return {
    startedAt: normalizeTimestamp(startedAt),
    updatedAt: normalizeTimestamp(updatedAt),
    sessions: normalizeSessions(sessions),
    traffic: normalizeTraffic(traffic, {
      fallbackStartedAt: normalizeTimestamp(startedAt),
      fallbackUpdatedAt: normalizeTimestamp(updatedAt),
    }),
  };
}

export function updateSessionSummary(
  snapshot,
  {
    sessionId,
    hasMac = false,
    clientCount = 0,
    macSocketCount = 0,
    iphoneSocketCount = 0,
    staleMacSocketCount = 0,
    staleIphoneSocketCount = 0,
  } = {},
  {
    now = Date.now(),
  } = {}
) {
  const normalizedSnapshot = createHealthSnapshot(snapshot);
  const normalizedSessionId = normalizeNonEmptyString(sessionId);
  if (!normalizedSessionId) {
    return normalizedSnapshot;
  }

  const normalizedClientCount = Math.max(0, Number(clientCount || 0));
  const normalizedMacSocketCount = Math.max(0, Number(macSocketCount || 0));
  const normalizedIphoneSocketCount = Math.max(0, Number(iphoneSocketCount || 0));
  const normalizedStaleMacSocketCount = Math.max(0, Number(staleMacSocketCount || 0));
  const normalizedStaleIphoneSocketCount = Math.max(0, Number(staleIphoneSocketCount || 0));
  const nextSessions = { ...normalizedSnapshot.sessions };
  if (
    !hasMac
    && normalizedClientCount === 0
    && normalizedMacSocketCount === 0
    && normalizedIphoneSocketCount === 0
  ) {
    delete nextSessions[normalizedSessionId];
  } else {
    nextSessions[normalizedSessionId] = {
      hasMac: Boolean(hasMac),
      clientCount: normalizedClientCount,
      macSocketCount: normalizedMacSocketCount,
      iphoneSocketCount: normalizedIphoneSocketCount,
      staleMacSocketCount: normalizedStaleMacSocketCount,
      staleIphoneSocketCount: normalizedStaleIphoneSocketCount,
    };
  }

  return {
    ...normalizedSnapshot,
    updatedAt: normalizeTimestamp(now),
    sessions: nextSessions,
  };
}

export function recordTraffic(
  snapshot,
  {
    channel,
    message,
    label = "",
  } = {},
  {
    now = Date.now(),
    topLabelLimit = RELAY_TRAFFIC_TOP_LABEL_LIMIT,
  } = {}
) {
  const normalizedSnapshot = createHealthSnapshot(snapshot);
  const normalizedChannel = normalizeNonEmptyString(channel);
  const bytes = byteLengthOfRelayTraffic(message);
  if (!normalizedChannel || bytes <= 0) {
    return normalizedSnapshot;
  }

  const nextChannels = structuredClone(normalizedSnapshot.traffic.channels);
  const nextChannel = nextChannels[normalizedChannel] || {
    messages: 0,
    bytes: 0,
    labels: {},
  };
  nextChannel.messages += 1;
  nextChannel.bytes += bytes;

  const normalizedLabel = normalizeNonEmptyString(label)
    || classifyRelayTrafficLabel(message);
  const nextLabel = nextChannel.labels[normalizedLabel] || { messages: 0, bytes: 0 };
  nextLabel.messages += 1;
  nextLabel.bytes += bytes;
  nextChannel.labels[normalizedLabel] = nextLabel;

  const sortedLabels = Object.entries(nextChannel.labels)
    .sort((left, right) => compareRelayTrafficEntries(
      { label: left[0], ...left[1] },
      { label: right[0], ...right[1] }
    ))
    .slice(0, Math.max(1, topLabelLimit));
  nextChannel.labels = Object.fromEntries(sortedLabels);
  nextChannels[normalizedChannel] = nextChannel;

  return {
    ...normalizedSnapshot,
    updatedAt: normalizeTimestamp(now),
    traffic: {
      ...normalizedSnapshot.traffic,
      updatedAt: normalizeTimestamp(now),
      channels: nextChannels,
    },
  };
}

export function mergeTrafficBatch(
  snapshot,
  {
    channels = {},
  } = {},
  {
    now = Date.now(),
    topLabelLimit = RELAY_TRAFFIC_TOP_LABEL_LIMIT,
  } = {}
) {
  const normalizedSnapshot = createHealthSnapshot(snapshot);
  const nextChannels = structuredClone(normalizedSnapshot.traffic.channels);

  for (const [channelName, channelEntry] of Object.entries(channels || {})) {
    const normalizedChannel = normalizeNonEmptyString(channelName);
    if (!normalizedChannel) {
      continue;
    }

    const nextChannel = nextChannels[normalizedChannel] || {
      messages: 0,
      bytes: 0,
      labels: {},
    };
    nextChannel.messages += Math.max(0, Number(channelEntry?.messages || 0));
    nextChannel.bytes += Math.max(0, Number(channelEntry?.bytes || 0));

    for (const [label, labelEntry] of Object.entries(channelEntry?.labels || {})) {
      const normalizedLabel = normalizeNonEmptyString(label);
      if (!normalizedLabel) {
        continue;
      }
      const nextLabel = nextChannel.labels[normalizedLabel] || { messages: 0, bytes: 0 };
      nextLabel.messages += Math.max(0, Number(labelEntry?.messages || 0));
      nextLabel.bytes += Math.max(0, Number(labelEntry?.bytes || 0));
      nextChannel.labels[normalizedLabel] = nextLabel;
    }

    const sortedLabels = Object.entries(nextChannel.labels)
      .sort((left, right) => compareRelayTrafficEntries(
        { label: left[0], ...left[1] },
        { label: right[0], ...right[1] }
      ))
      .slice(0, Math.max(1, topLabelLimit));
    nextChannel.labels = Object.fromEntries(sortedLabels);
    nextChannels[normalizedChannel] = nextChannel;
  }

  return {
    ...normalizedSnapshot,
    updatedAt: normalizeTimestamp(now),
    traffic: {
      ...normalizedSnapshot.traffic,
      updatedAt: normalizeTimestamp(now),
      channels: nextChannels,
    },
  };
}

export function buildRelayHealthStats(snapshot) {
  const normalizedSnapshot = createHealthSnapshot(snapshot);
  let sessionsWithMac = 0;
  let totalClients = 0;
  let totalMacSockets = 0;
  let totalIphoneSockets = 0;
  let totalStaleMacSockets = 0;
  let totalStaleIphoneSockets = 0;

  for (const session of Object.values(normalizedSnapshot.sessions)) {
    if (session.hasMac) {
      sessionsWithMac += 1;
    }
    totalClients += Number(session.clientCount || 0);
    totalMacSockets += Number(session.macSocketCount || 0);
    totalIphoneSockets += Number(session.iphoneSocketCount || 0);
    totalStaleMacSockets += Number(session.staleMacSocketCount || 0);
    totalStaleIphoneSockets += Number(session.staleIphoneSocketCount || 0);
  }

  const channels = {};
  for (const [channelName, channelEntry] of Object.entries(normalizedSnapshot.traffic.channels)) {
    channels[channelName] = {
      messages: channelEntry.messages,
      bytes: channelEntry.bytes,
      topLabels: Object.entries(channelEntry.labels)
        .map(([label, entry]) => ({
          label,
          messages: entry.messages,
          bytes: entry.bytes,
        }))
        .sort(compareRelayTrafficEntries),
    };
  }

  return {
    activeSessions: Object.keys(normalizedSnapshot.sessions).length,
    sessionsWithMac,
    totalClients,
    totalMacSockets,
    totalIphoneSockets,
    totalStaleMacSockets,
    totalStaleIphoneSockets,
    traffic: {
      startedAt: new Date(normalizedSnapshot.traffic.startedAt).toISOString(),
      updatedAt: new Date(normalizedSnapshot.traffic.updatedAt).toISOString(),
      channels,
    },
  };
}

export function classifyRelayTrafficLabel(rawMessage) {
  const parsed = safeParseJSON(rawMessage);
  if (!parsed || typeof parsed !== "object") {
    return "non_json";
  }

  const kind = normalizeNonEmptyString(parsed.kind);
  if (kind) {
    return `kind:${kind}`;
  }

  const method = normalizeNonEmptyString(parsed.method);
  if (method) {
    return method;
  }

  return parsed?.id != null ? "response" : "json";
}

function normalizeSessions(value) {
  if (!value || typeof value !== "object") {
    return {};
  }

  return Object.fromEntries(
    Object.entries(value)
      .map(([sessionId, session]) => {
        const normalizedSessionId = normalizeNonEmptyString(sessionId);
        if (!normalizedSessionId) {
          return null;
        }
        return [
          normalizedSessionId,
          {
            hasMac: Boolean(session?.hasMac),
            clientCount: Math.max(0, Number(session?.clientCount || 0)),
            macSocketCount: Math.max(0, Number(session?.macSocketCount || 0)),
            iphoneSocketCount: Math.max(0, Number(session?.iphoneSocketCount || 0)),
            staleMacSocketCount: Math.max(0, Number(session?.staleMacSocketCount || 0)),
            staleIphoneSocketCount: Math.max(0, Number(session?.staleIphoneSocketCount || 0)),
          },
        ];
      })
      .filter(Boolean)
  );
}

function normalizeTraffic(value, { fallbackStartedAt, fallbackUpdatedAt }) {
  const traffic = value && typeof value === "object" ? value : {};
  return {
    startedAt: normalizeTimestamp(traffic.startedAt, fallbackStartedAt),
    updatedAt: normalizeTimestamp(traffic.updatedAt, fallbackUpdatedAt),
    channels: Object.fromEntries(
      Object.entries(traffic.channels || {})
        .map(([channelName, channelEntry]) => {
          const normalizedChannelName = normalizeNonEmptyString(channelName);
          if (!normalizedChannelName) {
            return null;
          }
          return [
            normalizedChannelName,
            {
              messages: Math.max(0, Number(channelEntry?.messages || 0)),
              bytes: Math.max(0, Number(channelEntry?.bytes || 0)),
              labels: Object.fromEntries(
                Object.entries(channelEntry?.labels || {})
                  .map(([label, labelEntry]) => {
                    const normalizedLabel = normalizeNonEmptyString(label);
                    if (!normalizedLabel) {
                      return null;
                    }
                    return [
                      normalizedLabel,
                      {
                        messages: Math.max(0, Number(labelEntry?.messages || 0)),
                        bytes: Math.max(0, Number(labelEntry?.bytes || 0)),
                      },
                    ];
                  })
                  .filter(Boolean)
              ),
            },
          ];
        })
        .filter(Boolean)
    ),
  };
}

function normalizeTimestamp(value, fallback = Date.now()) {
  const normalized = Number(value);
  return Number.isFinite(normalized) ? normalized : Number(fallback);
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

function compareRelayTrafficEntries(left, right) {
  return right.bytes - left.bytes
    || right.messages - left.messages
    || left.label.localeCompare(right.label);
}
