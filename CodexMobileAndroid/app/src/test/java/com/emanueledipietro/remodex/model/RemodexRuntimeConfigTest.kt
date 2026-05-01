package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemodexRuntimeConfigTest {
    @Test
    fun `normalize selections exposes fast speed by default`() {
        val config = RemodexRuntimeConfig().normalizeSelections()

        assertEquals(listOf(RemodexServiceTier.FAST), config.availableServiceTiers)
    }

    @Test
    fun `normalize selections keeps xhigh when selected model supports it`() {
        val config = RemodexRuntimeConfig(
            availableModels = listOf(gpt55Model()),
            selectedModelId = "gpt-5.5",
            reasoningEffort = "xhigh",
        ).normalizeSelections()

        assertEquals("gpt-5.5", config.selectedModelId)
        assertEquals("xhigh", config.reasoningEffort)
        assertEquals("Extra High", RemodexRuntimeMetaMapper.reasoningTitle(config.reasoningEffort.orEmpty()))
        assertTrue(config.availableReasoningEfforts.any { option -> option.reasoningEffort == "xhigh" })
    }

    @Test
    fun `normalize selections falls back to model default when reasoning is unsupported`() {
        val config = RemodexRuntimeConfig(
            availableModels = listOf(gpt55Model(), gpt53CodexModel()),
            selectedModelId = "gpt-5.3-codex",
            reasoningEffort = "xhigh",
        ).normalizeSelections()

        assertEquals("gpt-5.3-codex", config.selectedModelId)
        assertEquals("medium", config.reasoningEffort)
    }

    @Test
    fun `defaults apply global model while thread override still controls reasoning`() {
        val config = RemodexRuntimeConfig(
            availableModels = listOf(gpt55Model(), gpt53CodexModel()),
            selectedModelId = "gpt-5.3-codex",
            reasoningEffort = "medium",
        ).applyDefaults(
            RemodexRuntimeDefaults(modelId = "gpt-5.5", reasoningEffort = "high"),
        ).applyThreadOverrides(
            RemodexRuntimeOverrides(reasoningEffort = "xhigh"),
        )

        assertEquals("gpt-5.5", config.selectedModelId)
        assertEquals("xhigh", config.reasoningEffort)
    }

    @Test
    fun `model metadata recognizes and prioritizes gpt 5_5`() {
        val ordered = RemodexRuntimeMetaMapper.orderedModels(
            listOf(
                gpt53CodexModel(),
                gpt54Model(),
                gpt55Model(),
            ),
        )

        assertEquals("gpt-5.5", ordered.first().id)
        assertEquals("GPT-5.5", RemodexRuntimeMetaMapper.modelTitle(gpt55Model()))
    }

    @Test
    fun `normalize selections merges codex reference models when bridge list is stale`() {
        val config = RemodexRuntimeConfig(
            availableModels = listOf(gpt54Model()),
        ).normalizeSelections()

        assertTrue(config.availableModels.any { option -> option.id == "gpt-5.5" })
        assertTrue(config.availableModels.any { option -> option.id == "gpt-5.4-mini" })
    }

    @Test
    fun `thread override can reset speed back to normal`() {
        val config = RemodexRuntimeConfig(
            serviceTier = RemodexServiceTier.FAST,
        ).applyThreadOverrides(
            RemodexRuntimeOverrides(
                serviceTier = null,
                hasServiceTierOverride = true,
            ),
        )

        assertNull(config.serviceTier)
    }

    @Test
    fun `defaults can reset speed back to normal`() {
        val config = RemodexRuntimeConfig(
            serviceTier = RemodexServiceTier.FAST,
        ).applyDefaults(
            RemodexRuntimeDefaults(
                serviceTier = null,
                hasServiceTierPreference = true,
            ),
        )

        assertNull(config.serviceTier)
    }

    @Test
    fun `access mode exposes sandbox values expected by current bridge runtimes`() {
        assertEquals("workspace-write", RemodexAccessMode.ON_REQUEST.sandboxLegacyValue)
        assertEquals("danger-full-access", RemodexAccessMode.FULL_ACCESS.sandboxLegacyValue)
    }

    private fun gpt55Model(): RemodexModelOption {
        return RemodexModelOption(
            id = "gpt-5.5",
            model = "gpt-5.5",
            displayName = "GPT-5.5",
            isDefault = true,
            supportedReasoningEfforts = listOf(
                RemodexReasoningEffortOption("low", "Low"),
                RemodexReasoningEffortOption("medium", "Medium"),
                RemodexReasoningEffortOption("high", "High"),
                RemodexReasoningEffortOption("xhigh", "Extra High"),
            ),
            defaultReasoningEffort = "medium",
        )
    }

    private fun gpt54Model(): RemodexModelOption {
        return RemodexModelOption(
            id = "gpt-5.4",
            model = "gpt-5.4",
            displayName = "GPT-5.4",
            isDefault = true,
            supportedReasoningEfforts = listOf(
                RemodexReasoningEffortOption("low", "Low"),
                RemodexReasoningEffortOption("medium", "Medium"),
                RemodexReasoningEffortOption("high", "High"),
                RemodexReasoningEffortOption("xhigh", "Extra High"),
            ),
            defaultReasoningEffort = "medium",
        )
    }

    private fun gpt53CodexModel(): RemodexModelOption {
        return RemodexModelOption(
            id = "gpt-5.3-codex",
            model = "gpt-5.3-codex",
            displayName = "GPT-5.3-Codex",
            supportedReasoningEfforts = listOf(
                RemodexReasoningEffortOption("low", "Low"),
                RemodexReasoningEffortOption("medium", "Medium"),
                RemodexReasoningEffortOption("high", "High"),
            ),
            defaultReasoningEffort = "medium",
        )
    }
}
