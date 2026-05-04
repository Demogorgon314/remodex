package com.emanueledipietro.remodex.model

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class RemodexRateLimitStatusTest {
    @Before
    fun setUp() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))
    }

    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `visible display sections keep default and spark limits separate`() {
        val sections = RemodexRateLimitBucket.visibleDisplaySections(
            buckets = listOf(
                RemodexRateLimitBucket(
                    limitId = "codex",
                    limitName = "Codex",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 7,
                        windowDurationMins = 300,
                        resetsAtEpochMs = 1_742_000_000_000,
                    ),
                    secondary = RemodexRateLimitWindow(
                        usedPercent = 44,
                        windowDurationMins = 10_080,
                        resetsAtEpochMs = 1_742_500_000_000,
                    ),
                ),
                RemodexRateLimitBucket(
                    limitId = "codex_spark",
                    limitName = "GPT-5.3-Codex-Spark",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 0,
                        windowDurationMins = 300,
                        resetsAtEpochMs = 1_742_700_000_000,
                    ),
                    secondary = RemodexRateLimitWindow(
                        usedPercent = 1,
                        windowDurationMins = 10_080,
                        resetsAtEpochMs = 1_743_100_000_000,
                    ),
                ),
            ),
            availableModels = listOf(
                RemodexModelOption(
                    id = "gpt-5.3-codex-spark",
                    model = "gpt-5.3-codex-spark",
                    displayName = "GPT-5.3-Codex-Spark",
                ),
            ),
        )

        assertEquals(listOf("default", "spark"), sections.map(RemodexRateLimitDisplaySection::id))
        assertNull(sections.first().title)
        assertEquals(listOf("5 小时", "1 周"), sections.first().rows.map(RemodexRateLimitDisplayRow::label))
        assertEquals("GPT-5.3-Codex-Spark 额度", sections.last().title)
        assertEquals(listOf("5 小时", "1 周"), sections.last().rows.map(RemodexRateLimitDisplayRow::label))
    }

    @Test
    fun `visible display sections hide spark limits when spark model is unavailable`() {
        val sections = RemodexRateLimitBucket.visibleDisplaySections(
            buckets = listOf(
                RemodexRateLimitBucket(
                    limitId = "codex",
                    limitName = "Codex",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 7,
                        windowDurationMins = 300,
                        resetsAtEpochMs = null,
                    ),
                    secondary = null,
                ),
                RemodexRateLimitBucket(
                    limitId = "codex_spark",
                    limitName = "GPT-5.3-Codex-Spark",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 0,
                        windowDurationMins = 300,
                        resetsAtEpochMs = null,
                    ),
                    secondary = null,
                ),
            ),
            availableModels = listOf(
                RemodexModelOption(
                    id = "gpt-5.4",
                    model = "gpt-5.4",
                    displayName = "GPT-5.4",
                ),
            ),
        )

        assertEquals(1, sections.size)
        assertEquals("default", sections.single().id)
        assertEquals(listOf("5 小时"), sections.single().rows.map(RemodexRateLimitDisplayRow::label))
    }

    @Test
    fun `visible display sections still dedupe matching labels inside each section`() {
        val sections = RemodexRateLimitBucket.visibleDisplaySections(
            buckets = listOf(
                RemodexRateLimitBucket(
                    limitId = "codex_5h",
                    limitName = "Codex",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 10,
                        windowDurationMins = 300,
                        resetsAtEpochMs = 1_742_000_000_000,
                    ),
                    secondary = null,
                ),
                RemodexRateLimitBucket(
                    limitId = "codex_5h_duplicate",
                    limitName = "Codex",
                    primary = RemodexRateLimitWindow(
                        usedPercent = 25,
                        windowDurationMins = 300,
                        resetsAtEpochMs = 1_741_000_000_000,
                    ),
                    secondary = null,
                ),
            ),
            availableModels = emptyList(),
        )

        val defaultRows = sections.single().rows
        assertEquals(1, defaultRows.size)
        assertEquals("5 小时", defaultRows.single().label)
        assertEquals(75, defaultRows.single().window.remainingPercent)
        assertTrue(defaultRows.single().window.resetsAtEpochMs == 1_741_000_000_000)
    }
}
