package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationScrollStateTrackerTest {
    @Test
    fun `user drag immediately disarms follow bottom`() {
        val nextMode = ConversationScrollStateTracker.modeAfterUserDragBegan(
            currentMode = ConversationAutoScrollMode.FOLLOW_BOTTOM,
        )

        assertEquals(ConversationAutoScrollMode.MANUAL, nextMode)
    }

    @Test
    fun `user drag preserves one shot anchor mode`() {
        val nextMode = ConversationScrollStateTracker.modeAfterUserDragBegan(
            currentMode = ConversationAutoScrollMode.ANCHOR_TURN,
        )

        assertEquals(ConversationAutoScrollMode.ANCHOR_TURN, nextMode)
    }

    @Test
    fun `drag end restores follow bottom only when already back at bottom`() {
        val nextMode = ConversationScrollStateTracker.modeAfterUserDragEnded(
            currentMode = ConversationAutoScrollMode.MANUAL,
            isScrolledToBottom = true,
        )

        assertEquals(ConversationAutoScrollMode.FOLLOW_BOTTOM, nextMode)
    }

    @Test
    fun `drag end keeps manual mode when user stopped away from bottom`() {
        val nextMode = ConversationScrollStateTracker.modeAfterUserDragEnded(
            currentMode = ConversationAutoScrollMode.MANUAL,
            isScrolledToBottom = false,
        )

        assertEquals(ConversationAutoScrollMode.MANUAL, nextMode)
    }

    @Test
    fun `drag end keeps anchor mode intact`() {
        val nextMode = ConversationScrollStateTracker.modeAfterUserDragEnded(
            currentMode = ConversationAutoScrollMode.ANCHOR_TURN,
            isScrolledToBottom = false,
        )

        assertEquals(ConversationAutoScrollMode.ANCHOR_TURN, nextMode)
    }

    @Test
    fun `scroll to latest button only shows when timeline is away from bottom`() {
        assertFalse(
            ConversationScrollStateTracker.shouldShowScrollToLatestButton(
                itemCount = 0,
                isScrolledToBottom = true,
            ),
        )
        assertFalse(
            ConversationScrollStateTracker.shouldShowScrollToLatestButton(
                itemCount = 3,
                isScrolledToBottom = true,
            ),
        )
        assertTrue(
            ConversationScrollStateTracker.shouldShowScrollToLatestButton(
                itemCount = 3,
                isScrolledToBottom = false,
            ),
        )
    }

    @Test
    fun `cooldown deadline offsets by configured interval`() {
        val nowMs = 1_000L

        val deadline = ConversationScrollStateTracker.cooldownDeadline(nowMs)

        assertEquals(nowMs + ConversationScrollStateTracker.userScrollCooldownMs, deadline)
    }
}
