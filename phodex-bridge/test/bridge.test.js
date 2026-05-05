// FILE: bridge.test.js
// Purpose: Verifies relay watchdog helpers used to recover from stale sleep/wake sockets.
// Layer: Unit test
// Exports: node:test suite
// Depends on: node:test, node:assert/strict, ../src/bridge

const test = require("node:test");
const assert = require("node:assert/strict");
const {
  buildHeartbeatBridgeStatus,
  buildPublishedBridgeStatus,
  classifyBridgeTrafficMessageLabel,
  createBridgeTrafficStats,
  fetchAdaptiveThreadTurnsListForRelay,
  hasRelayConnectionGoneStale,
  rememberTrafficRequestMethod,
  sanitizeLiveGeneratedImageMessageForRelay,
  sanitizeThreadHistoryImagesForRelay,
} = require("../src/bridge");

test("hasRelayConnectionGoneStale returns true once the relay silence crosses the timeout", () => {
  assert.equal(
    hasRelayConnectionGoneStale(1_000, {
      now: 71_000,
      staleAfterMs: 70_000,
    }),
    true
  );
});

test("hasRelayConnectionGoneStale returns false for fresh or missing activity timestamps", () => {
  assert.equal(
    hasRelayConnectionGoneStale(1_000, {
      now: 70_999,
      staleAfterMs: 70_000,
    }),
    false
  );
  assert.equal(hasRelayConnectionGoneStale(Number.NaN), false);
});

test("hasRelayConnectionGoneStale default threshold tolerates a full quiet minute", () => {
  assert.equal(
    hasRelayConnectionGoneStale(1_000, {
      now: 61_000,
    }),
    false
  );
  assert.equal(
    hasRelayConnectionGoneStale(1_000, {
      now: 72_000,
    }),
    true
  );
});

test("buildHeartbeatBridgeStatus downgrades stale connected snapshots", () => {
  assert.deepEqual(
    buildHeartbeatBridgeStatus(
      {
        state: "running",
        connectionStatus: "connected",
        pid: 123,
        lastError: "",
      },
      1_000,
      {
        now: 26_500,
        staleAfterMs: 25_000,
        staleMessage: "Relay heartbeat stalled; reconnect pending.",
      }
    ),
    {
      state: "running",
      connectionStatus: "disconnected",
      pid: 123,
      lastError: "Relay heartbeat stalled; reconnect pending.",
    }
  );
});

test("buildHeartbeatBridgeStatus leaves fresh or already-disconnected snapshots unchanged", () => {
  const freshStatus = {
    state: "running",
    connectionStatus: "connected",
    pid: 123,
    lastError: "",
  };
  assert.deepEqual(
    buildHeartbeatBridgeStatus(freshStatus, 1_000, {
      now: 20_000,
      staleAfterMs: 25_000,
    }),
    freshStatus
  );

  const disconnectedStatus = {
    state: "running",
    connectionStatus: "disconnected",
    pid: 123,
    lastError: "",
  };
  assert.deepEqual(buildHeartbeatBridgeStatus(disconnectedStatus, 1_000), disconnectedStatus);
});

test("buildPublishedBridgeStatus refreshes traffic snapshots during heartbeats", () => {
  const published = buildPublishedBridgeStatus(
    {
      state: "running",
      connectionStatus: "connected",
      pid: 123,
      lastError: "",
      traffic: {
        startedAt: "1970-01-01T00:00:00.000Z",
        updatedAt: "1970-01-01T00:00:00.000Z",
        channels: {},
      },
    },
    {
      startedAt: "1970-01-01T00:00:01.000Z",
      updatedAt: "1970-01-01T00:00:02.000Z",
      channels: {
        relayInboundWire: {
          messages: 2,
          bytes: 256,
          topLabels: [
            {
              label: "kind:encryptedEnvelope",
              messages: 2,
              bytes: 256,
            },
          ],
        },
      },
    },
    {
      lastActivityAt: 5_000,
      heartbeatOptions: {
        now: 20_000,
        staleAfterMs: 25_000,
      },
    }
  );

  assert.equal(published.connectionStatus, "connected");
  assert.equal(published.traffic.channels.relayInboundWire.bytes, 256);
  assert.equal(published.traffic.updatedAt, "1970-01-01T00:00:02.000Z");
});

function makeTurns(start, count) {
  return Array.from({ length: count }, (_, index) => ({
    id: `turn-${start + index}`,
    items: [
      {
        id: `item-${start + index}`,
        type: "assistant_message",
        text: `message ${start + index}`,
      },
    ],
  }));
}

test("fetchAdaptiveThreadTurnsListForRelay expands small turns-list pages to the requested limit", async () => {
  const request = {
    id: "req-turns-list",
    method: "thread/turns/list",
    params: {
      threadId: "thread-small",
      limit: 20,
      sortDirection: "desc",
    },
  };
  const fetches = [];
  const pages = [
    { data: makeTurns(1, 1), nextCursor: "cursor-after-1", stableMeta: "first-page" },
    { data: makeTurns(2, 4), nextCursor: "cursor-after-5", stableMeta: "second-page" },
    { data: makeTurns(6, 15), nextCursor: "cursor-after-20", stableMeta: "third-page" },
  ];

  const response = await fetchAdaptiveThreadTurnsListForRelay(request, {
    fetchPage: async (params) => {
      fetches.push(params);
      return pages.shift();
    },
  });

  assert.equal(response.id, "req-turns-list");
  assert.equal(response.result.data.length, 20);
  assert.deepEqual(
    response.result.data.map((turn) => turn.id),
    makeTurns(1, 20).map((turn) => turn.id)
  );
  assert.equal(response.result.stableMeta, "first-page");
  assert.equal(response.result.nextCursor, "cursor-after-20");
  assert.deepEqual(
    fetches.map((params) => ({ limit: params.limit, cursor: params.cursor })),
    [
      { limit: 1, cursor: undefined },
      { limit: 4, cursor: "cursor-after-1" },
      { limit: 15, cursor: "cursor-after-5" },
    ]
  );
});

test("fetchAdaptiveThreadTurnsListForRelay stops at one turn for a huge first turns-list page", async () => {
  const fetches = [];
  const response = await fetchAdaptiveThreadTurnsListForRelay({
    id: "req-turns-list-large-first",
    method: "thread/turns/list",
    params: {
      threadId: "thread-large",
      limit: 20,
      sortDirection: "desc",
    },
  }, {
    fetchPage: async (params) => {
      fetches.push(params);
      return {
        data: [
          {
            id: "turn-1",
            items: [
              {
                id: "item-1",
                type: "function_call_output",
                text: "A".repeat(4 * 1024 * 1024),
              },
            ],
          },
        ],
        nextCursor: "cursor-after-1",
      };
    },
  });

  assert.deepEqual(response.result.data.map((turn) => turn.id), ["turn-1"]);
  assert.equal(response.result.nextCursor, "cursor-after-1");
  assert.equal(fetches.length, 1);
});

test("fetchAdaptiveThreadTurnsListForRelay stops after a huge second turns-list batch", async () => {
  const fetches = [];
  const pages = [
    { data: makeTurns(1, 1), nextCursor: "cursor-after-1" },
    {
      data: makeTurns(2, 4).map((turn) => ({
        ...turn,
        items: [
          {
            id: `${turn.id}-item`,
            type: "function_call_output",
            text: "B".repeat(1024 * 1024),
          },
        ],
      })),
      nextCursor: "cursor-after-5",
    },
  ];

  const response = await fetchAdaptiveThreadTurnsListForRelay({
    id: "req-turns-list-large-second",
    method: "thread/turns/list",
    params: {
      threadId: "thread-mixed",
      limit: 20,
      sortDirection: "desc",
    },
  }, {
    fetchPage: async (params) => {
      fetches.push(params);
      return pages.shift();
    },
  });

  assert.deepEqual(
    response.result.data.map((turn) => turn.id),
    makeTurns(1, 5).map((turn) => turn.id)
  );
  assert.equal(response.result.nextCursor, "cursor-after-5");
  assert.deepEqual(
    fetches.map((params) => params.limit),
    [1, 4]
  );
});

test("fetchAdaptiveThreadTurnsListForRelay forwards input and returned cursors", async () => {
  const fetches = [];
  const pages = [
    { items: makeTurns(1, 1), nextCursor: "cursor-after-first" },
    { items: makeTurns(2, 4), nextCursor: "cursor-after-second" },
    { items: makeTurns(6, 1), nextCursor: "cursor-after-third" },
  ];

  const response = await fetchAdaptiveThreadTurnsListForRelay({
    id: "req-turns-list-older",
    method: "thread/turns/list",
    params: {
      threadId: "thread-large",
      limit: 6,
      sortDirection: "desc",
      cursor: "cursor-before-page",
    },
  }, {
    fetchPage: async (params) => {
      fetches.push(params);
      return pages.shift();
    },
  });

  assert.equal(response.result.items.length, 6);
  assert.equal(response.result.nextCursor, "cursor-after-third");
  assert.deepEqual(
    fetches.map((params) => ({ limit: params.limit, cursor: params.cursor })),
    [
      { limit: 1, cursor: "cursor-before-page" },
      { limit: 4, cursor: "cursor-after-first" },
      { limit: 1, cursor: "cursor-after-second" },
    ]
  );
});

test("fetchAdaptiveThreadTurnsListForRelay preserves turns-list response array shapes", async () => {
  for (const turnsKey of ["data", "items", "turns"]) {
    const response = await fetchAdaptiveThreadTurnsListForRelay({
      id: `req-${turnsKey}`,
      method: "thread/turns/list",
      params: {
        threadId: `thread-${turnsKey}`,
        limit: 1,
      },
    }, {
      fetchPage: async () => ({
        [turnsKey]: makeTurns(1, 1),
        nextCursor: `cursor-${turnsKey}`,
      }),
    });

    assert.equal(Array.isArray(response.result[turnsKey]), true);
    assert.equal(response.result[turnsKey][0].id, "turn-1");
    for (const otherKey of ["data", "items", "turns"].filter((key) => key !== turnsKey)) {
      assert.equal(response.result[otherKey], undefined);
    }
    assert.equal(response.result.nextCursor, `cursor-${turnsKey}`);
  }
});

test("fetchAdaptiveThreadTurnsListForRelay returns fetched turns when a later batch fails", async () => {
  const response = await fetchAdaptiveThreadTurnsListForRelay({
    id: "req-turns-list-later-error",
    method: "thread/turns/list",
    params: {
      threadId: "thread-later-error",
      limit: 5,
    },
  }, {
    fetchPage: async (params) => {
      if (params.cursor === "cursor-after-first") {
        throw new Error("app-server failed");
      }
      return {
        data: makeTurns(1, 1),
        nextCursor: "cursor-after-first",
      };
    },
  });

  assert.deepEqual(response.result.data.map((turn) => turn.id), ["turn-1"]);
  assert.equal(response.result.nextCursor, "cursor-after-first");
});

test("sanitizeThreadHistoryImagesForRelay replaces inline history images with lightweight references", () => {
  const rawMessage = JSON.stringify({
    id: "req-thread-read",
    result: {
      thread: {
        id: "thread-images",
        turns: [
          {
            id: "turn-1",
            items: [
              {
                id: "item-user",
                type: "user_message",
                content: [
                  {
                    type: "input_text",
                    text: "Look at this screenshot",
                  },
                  {
                    type: "image",
                    image_url: "data:image/png;base64,AAAA",
                  },
                ],
              },
            ],
          },
        ],
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/read")
  );
  const content = sanitized.result.thread.turns[0].items[0].content;

  assert.deepEqual(content[0], {
    type: "input_text",
    text: "Look at this screenshot",
  });
  assert.deepEqual(content[1], {
    type: "image",
    url: "remodex://history-image-elided",
  });
});

test("sanitizeThreadHistoryImagesForRelay leaves unrelated RPC payloads unchanged", () => {
  const rawMessage = JSON.stringify({
    id: "req-other",
    result: {
      ok: true,
    },
  });

  assert.equal(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "turn/start"),
    rawMessage
  );
});

test("sanitizeThreadHistoryImagesForRelay can return only the first user message for title regeneration", () => {
  const rawMessage = JSON.stringify({
    id: "rpc-title-seed",
    result: {
      thread: {
        id: "thread-title-seed",
        title: "Manual title",
        turns: [
          {
            id: "turn-system",
            items: [
              {
                id: "system-1",
                type: "system_message",
                text: "System setup",
              },
            ],
          },
          {
            id: "turn-first-user",
            items: [
              {
                id: "user-1",
                type: "user_message",
                text: "Summarize this first request.",
              },
              {
                id: "assistant-1",
                type: "agent_message",
                text: "Sure.",
              },
            ],
          },
          {
            id: "turn-later-user",
            items: [
              {
                id: "user-2",
                type: "user_message",
                text: "Later request.",
              },
            ],
          },
        ],
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/read", {
      remodexTitleSeedOnly: true,
    })
  );

  assert.equal(sanitized.result.thread.remodexTitleSeedOnly, true);
  assert.equal(sanitized.result.thread.turns.length, 1);
  assert.equal(sanitized.result.thread.turns[0].id, "turn-first-user");
  assert.deepEqual(
    sanitized.result.thread.turns[0].items.map((item) => item.id),
    ["user-1"]
  );
});

test("bridge traffic classification attributes responses back to the original request method", () => {
  const applicationRequestMethodsById = new Map();
  rememberTrafficRequestMethod(
    applicationRequestMethodsById,
    JSON.stringify({
      id: "req-thread-read",
      method: "thread/read",
      params: {
        threadId: "thread-1",
      },
    })
  );

  assert.equal(
    classifyBridgeTrafficMessageLabel(JSON.stringify({
      id: "req-thread-read",
      result: {
        thread: {
          id: "thread-1",
        },
      },
    }), {
      applicationRequestMethodsById,
    }),
    "thread/read:response"
  );

  assert.equal(
    classifyBridgeTrafficMessageLabel(JSON.stringify({
      id: "bridge-managed-1",
      error: {
        message: "expired",
      },
    }), {
      bridgeManagedCodexRequestWaiters: new Map([
        [
          "bridge-managed-1",
          {
            method: "getAuthStatus",
          },
        ],
      ]),
    }),
    "bridge/getAuthStatus:error"
  );
});

test("bridge traffic stats keep per-channel totals plus top labels", () => {
  let currentTime = 1_000;
  const trafficStats = createBridgeTrafficStats({
    now() {
      return currentTime;
    },
  });

  trafficStats.record("relayOutboundApplication", JSON.stringify({
    id: "req-thread-read",
    result: {
      thread: {
        id: "thread-1",
      },
    },
  }), {
    label: "thread/read:response",
  });
  currentTime = 2_000;
  trafficStats.record("relayOutboundApplication", JSON.stringify({
    method: "turn/update",
    params: {
      delta: "ok",
    },
  }), {
    label: "turn/update",
  });
  currentTime = 3_000;
  trafficStats.record("codexInbound", JSON.stringify({
    kind: "encryptedEnvelope",
  }), {
    label: "kind:encryptedEnvelope",
  });

  const snapshot = trafficStats.snapshot();
  assert.equal(snapshot.startedAt, "1970-01-01T00:00:01.000Z");
  assert.equal(snapshot.updatedAt, "1970-01-01T00:00:03.000Z");
  assert.equal(snapshot.channels.relayOutboundApplication.messages, 2);
  assert.equal(
    snapshot.channels.relayOutboundApplication.topLabels[0].label,
    "thread/read:response"
  );
  assert.equal(snapshot.channels.codexInbound.topLabels[0].label, "kind:encryptedEnvelope");
});

test("sanitizeThreadHistoryImagesForRelay strips bulky compaction replacement history", () => {
  const rawMessage = JSON.stringify({
    id: "req-thread-resume",
    result: {
      thread: {
        id: "thread-compaction",
        turns: [
          {
            id: "turn-1",
            items: [
              {
                id: "item-compaction",
                type: "context_compaction",
                payload: {
                  message: "",
                  replacement_history: [
                    {
                      type: "message",
                      role: "assistant",
                      content: [{ type: "output_text", text: "very old transcript" }],
                    },
                  ],
                },
              },
              {
                id: "item-compaction-camel",
                type: "contextCompaction",
                replacementHistory: [
                  {
                    type: "message",
                    role: "user",
                    content: [{ type: "input_text", text: "older prompt" }],
                  },
                ],
              },
            ],
          },
        ],
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/resume")
  );
  const items = sanitized.result.thread.turns[0].items;

  assert.deepEqual(items[0], {
    id: "item-compaction",
    type: "context_compaction",
    payload: {
      message: "",
    },
  });
  assert.deepEqual(items[1], {
    id: "item-compaction-camel",
    type: "contextCompaction",
  });
});

test("sanitizeThreadHistoryImagesForRelay sanitizes turns-list inline images", () => {
  const rawMessage = JSON.stringify({
    id: "req-turns-list-images",
    result: {
      data: [
        {
          id: "turn-1",
          items: [
            {
              id: "item-user",
              type: "user_message",
              content: [
                {
                  type: "input_text",
                  text: "Look",
                },
                {
                  type: "image",
                  image_url: "data:image/png;base64,AAAA",
                },
              ],
            },
          ],
        },
      ],
      nextCursor: "cursor-2",
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/turns/list")
  );

  assert.equal(sanitized.result.data[0].items[0].content[1].url, "remodex://history-image-elided");
  assert.equal(sanitized.result.data[0].items[0].content[1].image_url, undefined);
  assert.equal(sanitized.result.nextCursor, "cursor-2");
});

test("sanitizeThreadHistoryImagesForRelay strips turns-list compaction replacement history", () => {
  const rawMessage = JSON.stringify({
    id: "req-turns-list-compacted",
    result: {
      data: [
        {
          id: "turn-1",
          items: [
            {
              id: "item-compacted",
              type: "compacted",
              message: "",
              replacement_history: [
                {
                  type: "message",
                  role: "assistant",
                  content: [{ type: "output_text", text: "A".repeat(1024) }],
                },
              ],
            },
          ],
        },
      ],
      nextCursor: "cursor-3",
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/turns/list")
  );

  assert.deepEqual(sanitized.result.data[0].items[0], {
    id: "item-compacted",
    type: "compacted",
    message: "",
  });
  assert.equal(sanitized.result.nextCursor, "cursor-3");
});

test("sanitizeThreadHistoryImagesForRelay annotates turns-list generated image history", () => {
  const rawMessage = JSON.stringify({
    id: "req-turns-list-generated-image",
    result: {
      data: [
        {
          id: "turn-1",
          threadId: "thread-images",
          items: [
            {
              id: "call-1",
              type: "image_generation",
              result: "data:image/png;base64,AAAA",
            },
          ],
        },
      ],
      nextCursor: "cursor-4",
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/turns/list")
  );
  const item = sanitized.result.data[0].items[0];

  assert.equal(item.saved_path.endsWith("/thread-images/call-1.png"), true);
  assert.equal(item.result, undefined);
  assert.equal(item.result_elided_for_relay, true);
  assert.equal(sanitized.result.nextCursor, "cursor-4");
});

test("sanitizeThreadHistoryImagesForRelay compacts oversized turns pages", () => {
  const rawMessage = JSON.stringify({
    id: "req-turns-list-large",
    result: {
      items: [
        {
          id: "turn-1",
          items: [
            {
              id: "item-1",
              type: "assistant_message",
              text: "B".repeat(4 * 1024 * 1024),
            },
          ],
        },
      ],
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/turns/list")
  );
  const item = sanitized.result.items[0].items[0];

  assert.equal(sanitized.result.remodexPageCompactedForRelay, true);
  assert.deepEqual(
    sanitized.result.items.map((turn) => turn.id),
    ["turn-1"]
  );
  assert.equal(
    sanitized.result.items.some((turn) => turn.id.startsWith("remodex-history-compacted-")),
    false
  );
  assert.equal(sanitized.result.items[0].remodexPageCompactedForRelay, true);
  assert.equal(item.relayPayloadTruncated, true);
  assert.equal(item.text.startsWith("…\n"), true);
  assert.equal(item.text.length < 120_000, true);
});

test("sanitizeThreadHistoryImagesForRelay preserves oversized turns pages instead of replacing them with a marker", () => {
  const turns = Array.from({ length: 5 }, (_, turnIndex) => ({
    id: `turn-${turnIndex + 1}`,
    items: Array.from({ length: 900 }, (_, itemIndex) => ({
      id: `item-${turnIndex + 1}-${itemIndex + 1}`,
      type: "function_call_output",
      role: "tool",
      itemId: `call-${turnIndex + 1}-${itemIndex + 1}`,
      text: "C".repeat(1_500),
      payload: {
        blob: "D".repeat(1_200),
      },
    })),
  }));
  const rawMessage = JSON.stringify({
    id: "req-turns-list-impossible",
    result: {
      data: turns,
      nextCursor: "cursor-after-huge-page",
    },
  });

  const sanitizedRaw = sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/turns/list");
  const sanitized = JSON.parse(sanitizedRaw);

  assert.equal(Buffer.byteLength(sanitizedRaw, "utf8") <= 4 * 1024 * 1024, true);
  assert.deepEqual(
    sanitized.result.data.map((turn) => turn.id),
    turns.map((turn) => turn.id)
  );
  assert.equal(
    sanitized.result.data.some((turn) => turn.id.startsWith("remodex-history-compacted-")),
    false
  );
  assert.equal(sanitized.result.nextCursor, "cursor-after-huge-page");
  assert.equal(sanitized.result.data.every((turn) => turn.items.length === 900), true);
  assert.equal(
    sanitized.result.data.every((turn) => turn.items.every((item) => item.relayPayloadTruncated === true)),
    true
  );
});

test("sanitizeThreadHistoryImagesForRelay compacts oversized history before the newest turn tail", () => {
  const largeText = "A".repeat(4 * 1024 * 1024);
  const rawMessage = JSON.stringify({
    id: "req-thread-tail",
    result: {
      thread: {
        id: "thread-large-history",
        turns: [
          {
            id: "turn-old",
            items: [
              {
                id: "item-old",
                type: "assistant_message",
                text: largeText,
              },
            ],
          },
          {
            id: "turn-new",
            items: [
              {
                id: "item-new",
                type: "assistant_message",
                text: "latest reply",
              },
            ],
          },
        ],
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/read")
  );

  assert.equal(sanitized.result.thread.historyTailTruncatedForRelay, true);
  assert.equal(sanitized.result.thread.remodexHistoryCompacted, true);
  assert.equal(sanitized.result.thread.remodexOmittedTurnCount, 1);
  assert.equal(sanitized.result.thread.remodexKeptTurnCount, 1);
  assert.deepEqual(
    sanitized.result.thread.turns.map((turn) => turn.id),
    ["remodex-history-compacted-turn-old", "turn-new"]
  );
  assert.equal(
    sanitized.result.thread.turns[0].items[0].text.includes("Older turns omitted: 1"),
    true
  );
});

test("sanitizeThreadHistoryImagesForRelay keeps the newest forty turns when compacting", () => {
  const largeText = "A".repeat(900 * 1024);
  const turns = Array.from({ length: 45 }, (_, index) => ({
    id: `turn-${index + 1}`,
    items: [
      {
        id: `item-${index + 1}`,
        type: "assistant_message",
        text: index < 5 ? largeText : `reply ${index + 1}`,
      },
    ],
  }));
  const rawMessage = JSON.stringify({
    id: "req-thread-recent-window",
    result: {
      thread: {
        id: "thread-recent-window",
        turns,
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/read")
  );

  assert.equal(sanitized.result.thread.remodexHistoryCompacted, true);
  assert.equal(sanitized.result.thread.remodexOmittedTurnCount, 5);
  assert.equal(sanitized.result.thread.remodexKeptTurnCount, 40);
  assert.deepEqual(
    sanitized.result.thread.turns.map((turn) => turn.id),
    [
      "remodex-history-compacted-turn-1",
      ...turns.slice(5).map((turn) => turn.id),
    ]
  );
});

test("sanitizeThreadHistoryImagesForRelay truncates the newest oversized text item to its tail", () => {
  const largeText = `header\n${"B".repeat(4 * 1024 * 1024)}`;
  const rawMessage = JSON.stringify({
    id: "req-thread-text-tail",
    result: {
      thread: {
        id: "thread-large-item",
        turns: [
          {
            id: "turn-1",
            items: [
              {
                id: "item-1",
                type: "assistant_message",
                text: largeText,
              },
            ],
          },
        ],
      },
    },
  });

  const sanitized = JSON.parse(
    sanitizeThreadHistoryImagesForRelay(rawMessage, "thread/read")
  );
  const item = sanitized.result.thread.turns[0].items[0];

  assert.equal(sanitized.result.thread.historyTailTruncatedForRelay, true);
  assert.equal(item.relayTextTailTruncated, true);
  assert.equal(item.text.startsWith("…\n"), true);
  assert.equal(item.text.includes("header"), false);
});

test("sanitizeLiveGeneratedImageMessageForRelay annotates generated image payloads", () => {
  const rawMessage = JSON.stringify({
    method: "codex/event/item_completed",
    params: {
      threadId: "thread-images",
      item: {
        id: "call-1",
        type: "image_generation",
        result: "data:image/png;base64,AAAA",
      },
    },
  });

  const sanitized = JSON.parse(sanitizeLiveGeneratedImageMessageForRelay(rawMessage));

  assert.equal(sanitized.params.item.saved_path.endsWith("/thread-images/call-1.png"), true);
  assert.equal(sanitized.params.item.result, undefined);
  assert.equal(sanitized.params.item.result_elided_for_relay, true);
});
