package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.data.threads.TimelineMutation
import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexConversationItem

object TurnTimelineReducer {
    fun reduce(mutations: List<TimelineMutation>): List<RemodexConversationItem> {
        return mutations.fold(emptyList<RemodexConversationItem>()) { items, mutation ->
            reduce(items, mutation)
        }.sortedBy(RemodexConversationItem::orderIndex)
    }

    fun reduce(
        items: List<RemodexConversationItem>,
        mutation: TimelineMutation,
    ): List<RemodexConversationItem> {
        return when (mutation) {
            is TimelineMutation.Upsert -> upsert(items, mutation.item)
            is TimelineMutation.AssistantTextDelta -> mergeTextDelta(
                items = items,
                messageId = mutation.messageId,
                turnId = mutation.turnId,
                itemId = mutation.itemId,
                delta = mutation.delta,
                orderIndex = mutation.orderIndex,
                speaker = ConversationSpeaker.ASSISTANT,
                kind = ConversationItemKind.CHAT,
            )

            is TimelineMutation.ReasoningTextDelta -> mergeTextDelta(
                items = items,
                messageId = mutation.messageId,
                turnId = mutation.turnId,
                itemId = mutation.itemId,
                delta = mutation.delta,
                orderIndex = mutation.orderIndex,
                speaker = ConversationSpeaker.SYSTEM,
                kind = ConversationItemKind.REASONING,
            )

            is TimelineMutation.ActivityLine -> mergeActivityLine(
                items = items,
                messageId = mutation.messageId,
                turnId = mutation.turnId,
                itemId = mutation.itemId,
                line = mutation.line,
                orderIndex = mutation.orderIndex,
            )

            is TimelineMutation.SystemTextDelta -> mergeTextDelta(
                items = items,
                messageId = mutation.messageId,
                turnId = mutation.turnId,
                itemId = mutation.itemId,
                delta = mutation.delta,
                orderIndex = mutation.orderIndex,
                speaker = ConversationSpeaker.SYSTEM,
                kind = mutation.kind,
            )

            is TimelineMutation.Complete -> markComplete(
                items = items,
                messageId = mutation.messageId,
            )
        }
    }

    private fun upsert(
        items: List<RemodexConversationItem>,
        item: RemodexConversationItem,
    ): List<RemodexConversationItem> {
        val existingIndex = items.indexOfFirst { it.id == item.id }
        if (existingIndex == -1) {
            return items + item
        }
        return items.toMutableList().apply {
            this[existingIndex] = item
        }
    }

    private fun mergeTextDelta(
        items: List<RemodexConversationItem>,
        messageId: String,
        turnId: String,
        itemId: String?,
        delta: String,
        orderIndex: Long,
        speaker: ConversationSpeaker,
        kind: ConversationItemKind,
    ): List<RemodexConversationItem> {
        val trimmedDelta = delta.trim()
        val existingIndex = items.indexOfFirst { item ->
            item.id == messageId || (
                item.kind == kind &&
                    item.speaker == speaker &&
                    item.turnId == turnId &&
                    item.itemId == itemId
            )
        }
        val nextItem = if (existingIndex == -1) {
            RemodexConversationItem(
                id = messageId,
                speaker = speaker,
                kind = kind,
                text = trimmedDelta,
                turnId = turnId,
                itemId = itemId,
                isStreaming = true,
                orderIndex = orderIndex,
            )
        } else {
            val existing = items[existingIndex]
            existing.copy(
                text = mergeDeltaText(
                    existing = existing.text,
                    incoming = trimmedDelta,
                    speaker = speaker,
                    kind = kind,
                ),
                turnId = turnId,
                itemId = itemId ?: existing.itemId,
                isStreaming = true,
                orderIndex = maxOf(existing.orderIndex, orderIndex),
            )
        }

        return if (existingIndex == -1) {
            items + nextItem
        } else {
            items.toMutableList().apply {
                this[existingIndex] = nextItem
            }
        }
    }

    private fun mergeDeltaText(
        existing: String,
        incoming: String,
        speaker: ConversationSpeaker,
        kind: ConversationItemKind,
    ): String {
        if (speaker == ConversationSpeaker.ASSISTANT || kind == ConversationItemKind.REASONING) {
            return mergeText(existing, incoming)
        }
        return mergeSystemText(existing, incoming)
    }

    private fun mergeSystemText(
        existing: String,
        incoming: String,
    ): String {
        val existingTrimmed = existing.trim()
        val incomingTrimmed = incoming.trim()
        if (incomingTrimmed.isEmpty()) {
            return existingTrimmed
        }
        if (existingTrimmed.isEmpty()) {
            return incomingTrimmed
        }
        if (incomingTrimmed in existingTrimmed) {
            return existingTrimmed
        }
        if (existingTrimmed in incomingTrimmed) {
            return incomingTrimmed
        }
        if (!existing.contains('\n') && !incoming.startsWith('\n')) {
            return "$existingTrimmed\n$incoming"
        }
        return existing + incoming
    }

    private fun mergeActivityLine(
        items: List<RemodexConversationItem>,
        messageId: String,
        turnId: String,
        itemId: String?,
        line: String,
        orderIndex: Long,
    ): List<RemodexConversationItem> {
        val trimmedLine = line.trim()
        val existingIndex = items.indexOfFirst { item ->
            item.id == messageId || (
                item.turnId == turnId &&
                    item.itemId == itemId &&
                    item.kind == ConversationItemKind.COMMAND_EXECUTION
            )
        }
        val nextItem = if (existingIndex == -1) {
            RemodexConversationItem(
                id = messageId,
                speaker = ConversationSpeaker.SYSTEM,
                kind = ConversationItemKind.COMMAND_EXECUTION,
                text = trimmedLine,
                supportingText = "Activity",
                turnId = turnId,
                itemId = itemId,
                isStreaming = true,
                orderIndex = orderIndex,
            )
        } else {
            val existing = items[existingIndex]
            existing.copy(
                text = mergeActivityText(existing.text, trimmedLine),
                supportingText = existing.supportingText ?: "Activity",
                turnId = turnId,
                itemId = itemId ?: existing.itemId,
                isStreaming = true,
                orderIndex = maxOf(existing.orderIndex, orderIndex),
            )
        }

        return if (existingIndex == -1) {
            items + nextItem
        } else {
            items.toMutableList().apply {
                this[existingIndex] = nextItem
            }
        }
    }

    private fun mergeActivityText(
        existing: String,
        incoming: String,
    ): String {
        val existingTrimmed = existing.trim()
        val incomingTrimmed = incoming.trim()
        if (incomingTrimmed.isEmpty()) {
            return existingTrimmed
        }
        if (existingTrimmed.isEmpty()) {
            return incomingTrimmed
        }
        if (incomingTrimmed in existingTrimmed) {
            return existingTrimmed
        }
        if (existingTrimmed in incomingTrimmed) {
            return incomingTrimmed
        }
        return "$existingTrimmed\n$incomingTrimmed"
    }

    private fun markComplete(
        items: List<RemodexConversationItem>,
        messageId: String,
    ): List<RemodexConversationItem> {
        val existingIndex = items.indexOfFirst { it.id == messageId }
        if (existingIndex == -1) {
            return items
        }
        return items.toMutableList().apply {
            this[existingIndex] = this[existingIndex].copy(isStreaming = false)
        }
    }

    private fun mergeText(
        existing: String,
        incoming: String,
    ): String {
        if (incoming.isBlank()) {
            return existing
        }
        if (existing.isEmpty()) {
            return incoming
        }

        val placeholderValues = setOf("thinking...")
        val existingTrimmed = existing.trim()
        val incomingTrimmed = incoming.trim()
        val existingLower = existingTrimmed.lowercase()
        val incomingLower = incomingTrimmed.lowercase()
        if (placeholderValues.contains(incomingLower)) {
            return existing
        }
        if (placeholderValues.contains(existingLower)) {
            return incoming
        }
        if (incoming == existing) {
            return existing
        }
        if (existing.endsWith(incoming)) {
            return existing
        }
        if (incoming.length > existing.length && incoming.startsWith(existing)) {
            return incoming
        }
        if (existing.length > incoming.length && existing.startsWith(incoming)) {
            return existing
        }
        if (existingLower == incomingLower || incomingTrimmed in existingTrimmed) {
            return existing
        }
        if (existingTrimmed in incomingTrimmed) {
            return incoming
        }

        val maxOverlap = minOf(existing.length, incoming.length)
        if (maxOverlap > 0) {
            for (overlap in maxOverlap downTo 1) {
                if (existing.takeLast(overlap) == incoming.take(overlap)) {
                    return existing + incoming.drop(overlap)
                }
            }
        }

        return existing + incoming
    }
}
