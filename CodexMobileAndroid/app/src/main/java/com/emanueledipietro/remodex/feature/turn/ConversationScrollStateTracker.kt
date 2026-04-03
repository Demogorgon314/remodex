package com.emanueledipietro.remodex.feature.turn

enum class ConversationAutoScrollMode {
    FOLLOW_BOTTOM,
    ANCHOR_TURN,
    MANUAL,
}

object ConversationScrollStateTracker {
    const val userScrollCooldownMs: Long = 250L

    fun shouldShowScrollToLatestButton(
        itemCount: Int,
        isScrolledToBottom: Boolean,
    ): Boolean = itemCount > 0 && !isScrolledToBottom

    fun modeAfterUserDragBegan(
        currentMode: ConversationAutoScrollMode,
    ): ConversationAutoScrollMode {
        return if (currentMode == ConversationAutoScrollMode.ANCHOR_TURN) {
            currentMode
        } else {
            ConversationAutoScrollMode.MANUAL
        }
    }

    fun modeAfterUserDragEnded(
        currentMode: ConversationAutoScrollMode,
        isScrolledToBottom: Boolean,
    ): ConversationAutoScrollMode {
        return when {
            currentMode == ConversationAutoScrollMode.ANCHOR_TURN -> currentMode
            isScrolledToBottom -> ConversationAutoScrollMode.FOLLOW_BOTTOM
            else -> ConversationAutoScrollMode.MANUAL
        }
    }

    fun cooldownDeadline(
        nowMs: Long,
    ): Long = nowMs + userScrollCooldownMs
}
