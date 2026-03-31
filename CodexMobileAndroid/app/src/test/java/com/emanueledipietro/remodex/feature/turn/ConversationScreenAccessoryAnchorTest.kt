package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem
import org.junit.Assert.assertEquals
import org.junit.Test

class ConversationScreenAccessoryAnchorTest {
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
