package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem
import com.emanueledipietro.remodex.model.RemodexTurnTerminalState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationScreenAccessoryAnchorTest {
    @Test
    fun `bottom correction stays idle while bottom anchor remains visible`() {
        val shouldCorrect = shouldCorrectTimelineBottomAfterLayoutChange(
            previous = TimelineBottomLayoutSnapshot(
                totalItemsCount = 4,
                lastVisibleItemIndex = 3,
                lastVisibleItemOffset = 498,
                lastVisibleItemSize = 1,
                viewportEndOffset = 500,
            ),
            current = TimelineBottomLayoutSnapshot(
                totalItemsCount = 4,
                lastVisibleItemIndex = 3,
                lastVisibleItemOffset = 498,
                lastVisibleItemSize = 1,
                viewportEndOffset = 500,
            ),
            bottomAnchorIndex = 3,
            thresholdPx = 12,
        )

        assertFalse(shouldCorrect)
    }

    @Test
    fun `bottom correction triggers when content growth pushes anchor below viewport`() {
        val shouldCorrect = shouldCorrectTimelineBottomAfterLayoutChange(
            previous = TimelineBottomLayoutSnapshot(
                totalItemsCount = 4,
                lastVisibleItemIndex = 3,
                lastVisibleItemOffset = 498,
                lastVisibleItemSize = 1,
                viewportEndOffset = 500,
            ),
            current = TimelineBottomLayoutSnapshot(
                totalItemsCount = 4,
                lastVisibleItemIndex = 2,
                lastVisibleItemOffset = 452,
                lastVisibleItemSize = 38,
                viewportEndOffset = 500,
            ),
            bottomAnchorIndex = 3,
            thresholdPx = 12,
        )

        assertTrue(shouldCorrect)
    }

    @Test
    fun `accessory anchor skips trailing context compaction separators`() {
        val items = listOf(
            assistantItem(id = "assistant"),
            systemItem(
                id = "compact",
                kind = ConversationItemKind.CONTEXT_COMPACTION,
                text = "Context compacted",
            ),
        )

        val anchorIndex = resolveConversationBlockAccessoryAnchorIndex(
            items = items,
            blockStart = 0,
            blockEnd = items.lastIndex,
        )

        assertEquals(0, anchorIndex)
    }

    @Test
    fun `accessory anchor falls back to block end when block only contains compaction separators`() {
        val items = listOf(
            systemItem(
                id = "compact",
                kind = ConversationItemKind.CONTEXT_COMPACTION,
                text = "Context compacted",
            ),
        )

        val anchorIndex = resolveConversationBlockAccessoryAnchorIndex(
            items = items,
            blockStart = 0,
            blockEnd = 0,
        )

        assertEquals(0, anchorIndex)
    }

    @Test
    fun `running indicator anchor follows trailing context compaction separator`() {
        val items = listOf(
            assistantItem(id = "assistant"),
            systemItem(
                id = "compact",
                kind = ConversationItemKind.CONTEXT_COMPACTION,
                text = "Compacting context...",
            ),
        )

        val runningIndicatorIndex = resolveConversationBlockRunningIndicatorIndex(
            items = items,
            accessoryAnchorIndex = 0,
            blockEnd = items.lastIndex,
        )

        assertEquals(1, runningIndicatorIndex)
    }

    @Test
    fun `running indicator anchor stays with content when block has no compaction separator`() {
        val items = listOf(
            assistantItem(id = "assistant"),
        )

        val runningIndicatorIndex = resolveConversationBlockRunningIndicatorIndex(
            items = items,
            accessoryAnchorIndex = 0,
            blockEnd = 0,
        )

        assertEquals(0, runningIndicatorIndex)
    }

    @Test
    fun `running indicator stays visible for active thread even with stale completed state`() {
        val showsRunningIndicator = shouldShowConversationBlockRunningIndicator(
            blockTurnId = "turn-1",
            activeTurnId = "turn-1",
            isThreadRunning = true,
            isLatestBlock = true,
            latestTurnTerminalState = RemodexTurnTerminalState.COMPLETED,
            stoppedTurnIds = emptySet(),
        )

        assertTrue(showsRunningIndicator)
    }

    @Test
    fun `running indicator hides for latest stopped block even if thread still looks running`() {
        val showsRunningIndicator = shouldShowConversationBlockRunningIndicator(
            blockTurnId = "turn-1",
            activeTurnId = "turn-1",
            isThreadRunning = true,
            isLatestBlock = true,
            latestTurnTerminalState = RemodexTurnTerminalState.STOPPED,
            stoppedTurnIds = emptySet(),
        )

        assertFalse(showsRunningIndicator)
    }

    private fun assistantItem(
        id: String,
        text: String = "Done.",
    ): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.ASSISTANT,
            text = text,
        )
    }

    private fun systemItem(
        id: String,
        kind: ConversationItemKind,
        text: String,
    ): RemodexConversationItem {
        return RemodexConversationItem(
            id = id,
            speaker = ConversationSpeaker.SYSTEM,
            kind = kind,
            text = text,
        )
    }
}
