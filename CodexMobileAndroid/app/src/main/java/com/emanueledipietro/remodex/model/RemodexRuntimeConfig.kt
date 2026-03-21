package com.emanueledipietro.remodex.model

import kotlinx.serialization.Serializable

@Serializable
enum class RemodexPlanningMode {
    AUTO,
    PLAN,
    ;

    val label: String
        get() = when (this) {
            AUTO -> "Auto"
            PLAN -> "Plan"
        }
}

@Serializable
enum class RemodexReasoningEffort {
    LOW,
    MEDIUM,
    HIGH,
    ;

    val label: String
        get() = when (this) {
            LOW -> "Low"
            MEDIUM -> "Medium"
            HIGH -> "High"
        }
}

@Serializable
enum class RemodexAccessMode {
    ON_REQUEST,
    FULL_ACCESS,
    ;

    val label: String
        get() = when (this) {
            ON_REQUEST -> "On-Request"
            FULL_ACCESS -> "Full Access"
        }

    val shortLabel: String
        get() = when (this) {
            ON_REQUEST -> "Ask"
            FULL_ACCESS -> "Full"
        }

    val approvalPolicyCandidates: List<String>
        get() = when (this) {
            ON_REQUEST -> listOf("on-request", "onRequest")
            FULL_ACCESS -> listOf("never")
        }
}

@Serializable
enum class RemodexServiceTier {
    FAST,
    ;

    val label: String
        get() = when (this) {
            FAST -> "Fast"
        }

    val wireValue: String
        get() = when (this) {
            FAST -> "fast"
        }
}

@Serializable
data class RemodexModelOption(
    val id: String,
    val model: String,
    val displayName: String,
    val description: String = "",
    val isDefault: Boolean = false,
    val supportedReasoningEfforts: List<RemodexReasoningEffort> = RemodexReasoningEffort.entries,
    val defaultReasoningEffort: RemodexReasoningEffort? = null,
)

@Serializable
data class RemodexRuntimeOverrides(
    val modelId: String? = null,
    val planningMode: RemodexPlanningMode? = null,
    val reasoningEffort: RemodexReasoningEffort? = null,
    val accessMode: RemodexAccessMode? = null,
    val serviceTier: RemodexServiceTier? = null,
)

@Serializable
data class RemodexRuntimeDefaults(
    val modelId: String? = null,
    val reasoningEffort: RemodexReasoningEffort? = null,
    val accessMode: RemodexAccessMode = RemodexAccessMode.ON_REQUEST,
    val serviceTier: RemodexServiceTier? = null,
) {
    fun asOverrides(): RemodexRuntimeOverrides {
        return RemodexRuntimeOverrides(
            modelId = modelId,
            reasoningEffort = reasoningEffort,
            accessMode = accessMode,
            serviceTier = serviceTier,
        )
    }
}

@Serializable
data class RemodexRuntimeConfig(
    val availableModels: List<RemodexModelOption> = emptyList(),
    val availablePlanningModes: List<RemodexPlanningMode> = RemodexPlanningMode.entries,
    val availableReasoningEfforts: List<RemodexReasoningEffort> = RemodexReasoningEffort.entries,
    val availableAccessModes: List<RemodexAccessMode> = RemodexAccessMode.entries,
    val availableServiceTiers: List<RemodexServiceTier> = emptyList(),
    val selectedModelId: String? = null,
    val planningMode: RemodexPlanningMode = RemodexPlanningMode.AUTO,
    val reasoningEffort: RemodexReasoningEffort = RemodexReasoningEffort.MEDIUM,
    val accessMode: RemodexAccessMode = RemodexAccessMode.ON_REQUEST,
    val serviceTier: RemodexServiceTier? = null,
) {
    fun apply(overrides: RemodexRuntimeOverrides?): RemodexRuntimeConfig {
        if (overrides == null) {
            return this
        }
        return copy(
            selectedModelId = overrides.modelId ?: selectedModelId,
            planningMode = overrides.planningMode ?: planningMode,
            reasoningEffort = overrides.reasoningEffort ?: reasoningEffort,
            accessMode = overrides.accessMode ?: accessMode,
            serviceTier = overrides.serviceTier ?: serviceTier,
        )
    }

    val runtimeLabel: String
        get() = buildList {
            selectedModelId?.takeIf { it.isNotBlank() }?.let(::add)
            add(planningMode.label)
            add("${reasoningEffort.label.lowercase()} reasoning")
            if (accessMode == RemodexAccessMode.FULL_ACCESS) {
                add(accessMode.label)
            }
            serviceTier?.let { add(it.label) }
        }.joinToString(", ")
}
