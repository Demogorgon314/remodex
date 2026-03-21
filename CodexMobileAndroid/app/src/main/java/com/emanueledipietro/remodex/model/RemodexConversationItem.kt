package com.emanueledipietro.remodex.model

enum class ConversationSpeaker {
    USER,
    ASSISTANT,
    SYSTEM,
}

enum class ConversationItemKind {
    CHAT,
    REASONING,
    ACTIVITY,
    PLAN,
}

data class RemodexConversationItem(
    val id: String,
    val speaker: ConversationSpeaker,
    val kind: ConversationItemKind = ConversationItemKind.CHAT,
    val text: String,
    val supportingText: String? = null,
    val turnId: String? = null,
    val itemId: String? = null,
    val isStreaming: Boolean = false,
    val orderIndex: Long = 0L,
    val assistantChangeSet: RemodexAssistantChangeSet? = null,
)
