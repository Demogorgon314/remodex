package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.data.threads.TimelineMutation
import com.emanueledipietro.remodex.model.RemodexAssistantChangeSet
import com.emanueledipietro.remodex.model.RemodexAssistantChangeSetSource
import com.emanueledipietro.remodex.model.RemodexAssistantChangeSetStatus
import com.emanueledipietro.remodex.model.RemodexAssistantFileChange
import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TurnTimelineReducerTest {
    @Test
    fun `assistant deltas extend the existing item scoped response`() {
        val projected = TurnTimelineReducer.reduce(
            listOf(
                TimelineMutation.AssistantTextDelta(
                    messageId = "assistant-1",
                    turnId = "turn-1",
                    itemId = "assistant-item",
                    delta = "First chunk",
                    orderIndex = 1,
                ),
                TimelineMutation.AssistantTextDelta(
                    messageId = "assistant-1",
                    turnId = "turn-1",
                    itemId = "assistant-item",
                    delta = "Second chunk",
                    orderIndex = 1,
                ),
                TimelineMutation.Complete(
                    messageId = "assistant-1",
                ),
            ),
        )

        assertEquals(1, projected.size)
        assertEquals("assistant-1", projected.first().id)
        assertTrue(projected.first().text.contains("First chunk"))
        assertTrue(projected.first().text.contains("Second chunk"))
        assertFalse(projected.first().isStreaming)
    }

    @Test
    fun `late reasoning and activity updates merge into the existing item`() {
        val projected = TurnTimelineReducer.reduce(
            listOf(
                TimelineMutation.Upsert(
                    RemodexConversationItem(
                        id = "reasoning-1",
                        speaker = ConversationSpeaker.SYSTEM,
                        kind = ConversationItemKind.REASONING,
                        text = "Thinking...",
                        turnId = "turn-1",
                        itemId = "reasoning-item",
                        isStreaming = false,
                        orderIndex = 1,
                    ),
                ),
                TimelineMutation.ActivityLine(
                    messageId = "reasoning-1",
                    turnId = "turn-1",
                    itemId = "reasoning-item",
                    line = "Running ./gradlew :app:testDebugUnitTest",
                    orderIndex = 1,
                ),
                TimelineMutation.ReasoningTextDelta(
                    messageId = "reasoning-1",
                    turnId = "turn-1",
                    itemId = "reasoning-item",
                    delta = "Merged late reasoning detail",
                    orderIndex = 1,
                ),
            ),
        )

        assertEquals(1, projected.size)
        assertTrue(projected.first().text.contains("Running ./gradlew"))
        assertTrue(projected.first().text.contains("Merged late reasoning detail"))
        assertEquals("reasoning-item", projected.first().itemId)
        assertTrue(projected.first().isStreaming)
    }

    @Test
    fun `assistant revert metadata survives later deltas and completion`() {
        val projected = TurnTimelineReducer.reduce(
            listOf(
                TimelineMutation.Upsert(
                    RemodexConversationItem(
                        id = "assistant-1",
                        speaker = ConversationSpeaker.ASSISTANT,
                        kind = ConversationItemKind.CHAT,
                        text = "Started reply",
                        turnId = "turn-1",
                        itemId = "assistant-item",
                        orderIndex = 1,
                        assistantChangeSet = RemodexAssistantChangeSet(
                            id = "changeset-1",
                            repoRoot = "/tmp/remodex",
                            threadId = "thread-1",
                            turnId = "turn-1",
                            assistantMessageId = "assistant-1",
                            status = RemodexAssistantChangeSetStatus.READY,
                            source = RemodexAssistantChangeSetSource.TURN_DIFF,
                            forwardUnifiedPatch = """
                                diff --git a/src/App.kt b/src/App.kt
                                --- a/src/App.kt
                                +++ b/src/App.kt
                                @@ -1 +1 @@
                                -old
                                +new
                            """.trimIndent(),
                            fileChanges = listOf(
                                RemodexAssistantFileChange(
                                    path = "src/App.kt",
                                    additions = 1,
                                    deletions = 1,
                                ),
                            ),
                        ),
                    ),
                ),
                TimelineMutation.AssistantTextDelta(
                    messageId = "assistant-1",
                    turnId = "turn-1",
                    itemId = "assistant-item",
                    delta = "Finished reply",
                    orderIndex = 2,
                ),
                TimelineMutation.Complete(messageId = "assistant-1"),
            ),
        )

        assertEquals(1, projected.size)
        assertNotNull(projected.first().assistantChangeSet)
        assertEquals("src/App.kt", projected.first().assistantChangeSet?.fileChanges?.first()?.path)
        assertFalse(projected.first().isStreaming)
    }
}
