package com.emanueledipietro.remodex.model

data class RemodexTrailingToken(
    val query: String,
    val startIndex: Int,
)

object RemodexComposerCommandLogic {
    fun trailingFileToken(text: String): RemodexTrailingToken? {
        if (text.isEmpty() || text.last().isWhitespace()) {
            return null
        }
        val triggerIndex = text.lastIndexOf('@')
        if (triggerIndex < 0) {
            return null
        }
        if (triggerIndex > 0 && !text[triggerIndex - 1].isWhitespace()) {
            return null
        }
        val rawQuery = text.substring(triggerIndex + 1)
        val query = rawQuery.trim()
        if (query.isEmpty() || query.any { character -> character == '\n' || character == '\r' }) {
            return null
        }
        if (query.any(Char::isWhitespace)) {
            val looksFileLike = query.contains('/') || query.contains('\\') || query.contains('.')
            if (!looksFileLike) {
                return null
            }
        }
        return RemodexTrailingToken(query = query, startIndex = triggerIndex)
    }

    fun trailingSkillToken(text: String): RemodexTrailingToken? {
        val token = trailingToken(text = text, trigger = '$') ?: return null
        return token.takeIf { candidate -> candidate.query.any(Char::isLetter) }
    }

    fun trailingSlashCommandToken(text: String): RemodexTrailingToken? {
        if (text.isEmpty()) {
            return null
        }
        val triggerIndex = text.lastIndexOf('/')
        if (triggerIndex < 0) {
            return null
        }
        if (triggerIndex > 0 && !text[triggerIndex - 1].isWhitespace()) {
            return null
        }
        val query = text.substring(triggerIndex + 1)
        if (query.any(Char::isWhitespace)) {
            return null
        }
        return RemodexTrailingToken(query = query, startIndex = triggerIndex)
    }

    fun replaceTrailingFileToken(text: String, selectedPath: String): String? {
        val trimmedPath = selectedPath.trim()
        val token = trailingFileToken(text) ?: return null
        if (trimmedPath.isEmpty()) {
            return null
        }
        return text.replaceRange(token.startIndex, text.length, "@$trimmedPath ")
    }

    fun replaceTrailingSkillToken(text: String, selectedSkill: String): String? {
        val trimmedSkill = selectedSkill.trim()
        val token = trailingSkillToken(text) ?: return null
        if (trimmedSkill.isEmpty()) {
            return null
        }
        return text.replaceRange(token.startIndex, text.length, "\$$trimmedSkill ")
    }

    fun removeTrailingSlashCommandToken(text: String): String? {
        val token = trailingSlashCommandToken(text) ?: return null
        return text.replaceRange(token.startIndex, text.length, "").trim()
    }

    fun applySubagentsSelection(
        text: String,
        isSelected: Boolean,
    ): String {
        val trimmed = text.trim()
        if (!isSelected) {
            return trimmed
        }
        val cannedPrompt = RemodexSlashCommand.SUBAGENTS.cannedPrompt ?: return trimmed
        if (trimmed.isEmpty()) {
            return cannedPrompt
        }
        return "$cannedPrompt\n\n$trimmed"
    }

    fun hasContentConflictingWithReview(
        trimmedInput: String,
        mentionedFileCount: Int,
        mentionedSkillCount: Int,
        attachmentCount: Int,
        hasSubagentsSelection: Boolean,
    ): Boolean {
        val draftText = removeTrailingSlashCommandToken(trimmedInput) ?: trimmedInput
        return draftText.isNotEmpty() ||
            mentionedFileCount > 0 ||
            mentionedSkillCount > 0 ||
            attachmentCount > 0 ||
            hasSubagentsSelection
    }

    fun canOfferForkSlashCommand(
        text: String,
        mentionedFileCount: Int = 0,
        mentionedSkillCount: Int = 0,
        attachmentCount: Int = 0,
        hasReviewSelection: Boolean = false,
        hasSubagentsSelection: Boolean = false,
        isPlanModeArmed: Boolean = false,
    ): Boolean {
        val token = trailingSlashCommandToken(text) ?: return false
        val remainingDraft = text.replaceRange(token.startIndex, text.length, "").trim()
        return remainingDraft.isEmpty() &&
            mentionedFileCount == 0 &&
            mentionedSkillCount == 0 &&
            attachmentCount == 0 &&
            !hasReviewSelection &&
            !hasSubagentsSelection &&
            !isPlanModeArmed
    }

    fun replaceFileMentionAliases(
        text: String,
        mention: RemodexComposerMentionedFile,
    ): String {
        return replaceBoundedToken(
            token = "@${mention.fileName}",
            replacement = "@${mention.path}",
            text = text,
            ignoreCase = true,
        )
    }

    fun removeFileMentionAliases(
        text: String,
        mention: RemodexComposerMentionedFile,
    ): String {
        return removeBoundedToken(
            token = "@${mention.fileName}",
            text = text,
            ignoreCase = true,
        )
    }

    private fun trailingToken(
        text: String,
        trigger: Char,
    ): RemodexTrailingToken? {
        if (text.isEmpty()) {
            return null
        }
        val lastWhitespaceIndex = text.indexOfLast(Char::isWhitespace)
        val tokenStart = if (lastWhitespaceIndex >= 0) lastWhitespaceIndex + 1 else 0
        if (tokenStart >= text.length || text[tokenStart] != trigger) {
            return null
        }
        val query = text.substring(tokenStart + 1)
        if (query.isEmpty() || query.any(Char::isWhitespace)) {
            return null
        }
        return RemodexTrailingToken(query = query, startIndex = tokenStart)
    }

    private fun replaceBoundedToken(
        token: String,
        replacement: String,
        text: String,
        ignoreCase: Boolean,
    ): String {
        val regex = Regex(
            pattern = Regex.escape(token) + "(?=[\\s,.;:!?)\\]}>]|$)",
            options = setOf(RegexOption.MULTILINE) + if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet(),
        )
        return regex.replace(text, replacement)
    }

    private fun removeBoundedToken(
        token: String,
        text: String,
        ignoreCase: Boolean,
    ): String {
        val regex = Regex(
            pattern = Regex.escape(token) + "(?:[\\s,.;:!?)\\]}>]|$)",
            options = setOf(RegexOption.MULTILINE) + if (ignoreCase) setOf(RegexOption.IGNORE_CASE) else emptySet(),
        )
        return regex.replace(text, "").replace(Regex("\\s{2,}"), " ").trim()
    }
}
