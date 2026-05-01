package com.emanueledipietro.remodex.model

object RemodexRuntimeMetaMapper {
    private val preferredModelOrder = listOf(
        "gpt-5.5",
        "gpt-5.4",
        "gpt-5.4-mini",
        "gpt-5.3-codex",
        "gpt-5.2",
    )

    private val codexReferenceModels = listOf(
        codexReferenceModel(
            id = "gpt-5.5",
            displayName = "GPT-5.5",
            description = "Frontier model for complex coding, research, and real-world work.",
            defaultReasoningEffort = "medium",
        ),
        codexReferenceModel(
            id = "gpt-5.4",
            displayName = "GPT-5.4",
            description = "Strong model for everyday coding.",
            defaultReasoningEffort = "xhigh",
        ),
        codexReferenceModel(
            id = "gpt-5.4-mini",
            displayName = "GPT-5.4-Mini",
            description = "Small, fast, and cost-efficient model for simpler coding tasks.",
            defaultReasoningEffort = "medium",
        ),
        codexReferenceModel(
            id = "gpt-5.3-codex",
            displayName = "GPT-5.3-Codex",
            description = "Coding-optimized model.",
            defaultReasoningEffort = "medium",
        ),
        codexReferenceModel(
            id = "gpt-5.2",
            displayName = "GPT-5.2",
            description = "Optimized for professional work and long-running agents.",
            defaultReasoningEffort = "medium",
        ),
    )

    fun mergedWithCodexReferenceModels(models: List<RemodexModelOption>): List<RemodexModelOption> {
        val normalizedModels = models.mapNotNull(RemodexModelOption::normalizedOrNull)
        val existingModels = normalizedModels
            .flatMap { option -> listOf(option.id.lowercase(), option.model.lowercase()) }
            .toSet()
        return normalizedModels + codexReferenceModels.filter { reference ->
            reference.id.lowercase() !in existingModels && reference.model.lowercase() !in existingModels
        }
    }

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
            "gpt-5.5" -> "GPT-5.5"
            "gpt-5.3-codex" -> "GPT-5.3-Codex"
            "gpt-5.4" -> "GPT-5.4"
            "gpt-5.4-mini" -> "GPT-5.4-Mini"
            "gpt-5.2" -> "GPT-5.2"
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

private fun codexReferenceModel(
    id: String,
    displayName: String,
    description: String,
    defaultReasoningEffort: String,
): RemodexModelOption {
    return RemodexModelOption(
        id = id,
        model = id,
        displayName = displayName,
        description = description,
        isDefault = id == "gpt-5.5",
        supportedReasoningEfforts = listOf(
            RemodexReasoningEffortOption(
                reasoningEffort = "low",
                description = "Fast responses with lighter reasoning",
            ),
            RemodexReasoningEffortOption(
                reasoningEffort = "medium",
                description = "Balances speed and reasoning depth for everyday tasks",
            ),
            RemodexReasoningEffortOption(
                reasoningEffort = "high",
                description = "Greater reasoning depth for complex problems",
            ),
            RemodexReasoningEffortOption(
                reasoningEffort = "xhigh",
                description = "Extra high reasoning depth for complex problems",
            ),
        ),
        defaultReasoningEffort = defaultReasoningEffort,
    )
}
