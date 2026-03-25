package com.emanueledipietro.remodex.model

object RemodexRuntimeMetaMapper {
    private val preferredModelOrder = listOf(
        "gpt-5.1-codex-mini",
        "gpt-5.2",
        "gpt-5.1-codex-max",
        "gpt-5.2-codex",
        "gpt-5.3-codex",
    )

    fun orderedModels(models: List<RemodexModelOption>): List<RemodexModelOption> {
        val rankByModel = preferredModelOrder.withIndex().associate { (index, value) -> value to index }
        return models.sortedWith { lhs, rhs ->
            val lhsRank = rankByModel[lhs.model.lowercase()] ?: Int.MAX_VALUE
            val rhsRank = rankByModel[rhs.model.lowercase()] ?: Int.MAX_VALUE
            when {
                lhsRank != rhsRank -> lhsRank.compareTo(rhsRank)
                else -> modelTitle(rhs).compareTo(modelTitle(lhs))
            }
        }
    }

    fun orderedReasoningOptions(options: List<RemodexReasoningEffortOption>): List<RemodexReasoningEffortOption> {
        return options.sortedWith { lhs, rhs ->
            val lhsRank = reasoningRank(lhs.reasoningEffort)
            val rhsRank = reasoningRank(rhs.reasoningEffort)
            when {
                lhsRank != rhsRank -> rhsRank.compareTo(lhsRank)
                else -> reasoningTitle(rhs.reasoningEffort).compareTo(reasoningTitle(lhs.reasoningEffort))
            }
        }
    }

    fun modelTitle(model: RemodexModelOption): String {
        return when (model.model.lowercase()) {
            "gpt-5.3-codex" -> "GPT-5.3-Codex"
            "gpt-5.2-codex" -> "GPT-5.2-Codex"
            "gpt-5.1-codex-max" -> "GPT-5.1-Codex-Max"
            "gpt-5.4" -> "GPT-5.4"
            "gpt-5.2" -> "GPT-5.2"
            "gpt-5.1-codex-mini" -> "GPT-5.1-Codex-Mini"
            else -> model.displayName
        }
    }

    fun reasoningTitle(effort: String): String {
        return when (effort.trim().lowercase()) {
            "minimal", "low" -> "Low"
            "medium" -> "Medium"
            "high" -> "High"
            "xhigh", "extra_high", "extra-high", "very_high", "very-high" -> "Extra High"
            else -> effort.trim().split("_", "-")
                .filter(String::isNotBlank)
                .joinToString(" ") { part -> part.replaceFirstChar(Char::titlecase) }
        }
    }

    private fun reasoningRank(effort: String): Int {
        return when (reasoningTitle(effort)) {
            "Low" -> 0
            "Medium" -> 1
            "High" -> 2
            "Extra High" -> 3
            else -> 4
        }
    }
}
