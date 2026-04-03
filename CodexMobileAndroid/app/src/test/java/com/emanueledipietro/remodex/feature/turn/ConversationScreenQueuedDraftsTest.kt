package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationScreenQueuedDraftsTest {
    @Test
    fun `composer placeholder switches to queue copy while thread is running`() {
        assertEquals("Queue a follow-up", resolveConversationComposerPlaceholder(canStop = true))
        assertEquals(
            "Ask anything... @files, \$skills, /commands",
            resolveConversationComposerPlaceholder(canStop = false),
        )
    }

    @Test
    fun `queued draft row actions mirror iOS steering and restore rules`() {
        val normalRowState = resolveQueuedDraftRowActionState(
            draftId = "draft-1",
            isThreadRunning = true,
            canRestoreDrafts = true,
            steeringDraftId = null,
        )

        assertTrue(normalRowState.showsSteer)
        assertTrue(normalRowState.restoreEnabled)
        assertTrue(normalRowState.steerEnabled)
        assertTrue(normalRowState.removeEnabled)

        val steeringRowState = resolveQueuedDraftRowActionState(
            draftId = "draft-2",
            isThreadRunning = true,
            canRestoreDrafts = false,
            steeringDraftId = "draft-2",
        )

        assertTrue(steeringRowState.showsSteer)
        assertFalse(steeringRowState.restoreEnabled)
        assertFalse(steeringRowState.steerEnabled)
        assertFalse(steeringRowState.removeEnabled)

        val idleRowState = resolveQueuedDraftRowActionState(
            draftId = "draft-3",
            isThreadRunning = false,
            canRestoreDrafts = true,
            steeringDraftId = null,
        )

        assertFalse(idleRowState.showsSteer)
        assertTrue(idleRowState.restoreEnabled)
        assertTrue(idleRowState.removeEnabled)
    }
}
