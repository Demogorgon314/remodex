package com.emanueledipietro.remodex.model

data class RemodexRateLimitWindow(
    val usedPercent: Int,
    val windowDurationMins: Int?,
    val resetsAtEpochMs: Long?,
) {
    val clampedUsedPercent: Int
        get() = usedPercent.coerceIn(0, 100)

    val remainingPercent: Int
        get() = (100 - clampedUsedPercent).coerceAtLeast(0)
}

data class RemodexRateLimitDisplayRow(
    val id: String,
    val label: String,
    val window: RemodexRateLimitWindow,
)

data class RemodexRateLimitDisplaySection(
    val id: String,
    val title: String?,
    val rows: List<RemodexRateLimitDisplayRow>,
)

data class RemodexRateLimitBucket(
    val limitId: String,
    val limitName: String?,
    val primary: RemodexRateLimitWindow?,
    val secondary: RemodexRateLimitWindow?,
) {
    val primaryOrSecondary: RemodexRateLimitWindow?
        get() = primary ?: secondary

    val displayRows: List<RemodexRateLimitDisplayRow>
        get() = buildList {
            primary?.let { window ->
                add(
                    RemodexRateLimitDisplayRow(
                        id = "$limitId-primary",
                        label = labelFor(window, limitName ?: limitId),
                        window = window,
                    ),
                )
            }
            secondary?.let { window ->
                add(
                    RemodexRateLimitDisplayRow(
                        id = "$limitId-secondary",
                        label = labelFor(window, limitName ?: limitId),
                        window = window,
                    ),
                )
            }
        }

    val sortDurationMins: Int
        get() = primaryOrSecondary?.windowDurationMins ?: Int.MAX_VALUE

    val displayLabel: String
        get() = durationLabel(primaryOrSecondary?.windowDurationMins)
            ?: limitName?.trim().takeUnless(String?::isNullOrEmpty)
            ?: limitId

    companion object {
        fun visibleDisplayRows(buckets: List<RemodexRateLimitBucket>): List<RemodexRateLimitDisplayRow> {
            return visibleDisplaySections(buckets = buckets, availableModels = emptyList())
                .flatMap(RemodexRateLimitDisplaySection::rows)
        }

        fun visibleDisplaySections(
            buckets: List<RemodexRateLimitBucket>,
            availableModels: List<RemodexModelOption>,
        ): List<RemodexRateLimitDisplaySection> {
            val sparkModelAvailable = availableModels.any { model ->
                model.id.equals(SPARK_MODEL_ID, ignoreCase = true) ||
                    model.model.equals(SPARK_MODEL_ID, ignoreCase = true)
            }
            val rowsBySection = linkedMapOf<String, LinkedHashMap<String, RemodexRateLimitDisplayRow>>()
            val titlesBySection = linkedMapOf<String, String?>()

            buckets.forEach { bucket ->
                val sectionMetadata = displaySectionMetadata(bucket, sparkModelAvailable) ?: return@forEach
                val sectionRows = rowsBySection.getOrPut(sectionMetadata.id) { linkedMapOf() }
                titlesBySection.putIfAbsent(sectionMetadata.id, sectionMetadata.title)
                bucket.displayRows.forEach { row ->
                    val existing = sectionRows[row.label]
                    sectionRows[row.label] = if (existing == null) {
                        row
                    } else {
                        preferredDisplayRow(existing, row)
                    }
                }
            }

            return rowsBySection.mapNotNull { (sectionId, rowsByLabel) ->
                val rows = rowsByLabel.values.sortedWith(
                    compareBy<RemodexRateLimitDisplayRow> { it.window.windowDurationMins ?: Int.MAX_VALUE }
                        .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
                )
                if (rows.isEmpty()) {
                    null
                } else {
                    RemodexRateLimitDisplaySection(
                        id = sectionId,
                        title = titlesBySection[sectionId],
                        rows = rows,
                    )
                }
            }.sortedBy { section -> sectionOrder(section.id) }
        }

        private fun preferredDisplayRow(
            current: RemodexRateLimitDisplayRow,
            candidate: RemodexRateLimitDisplayRow,
        ): RemodexRateLimitDisplayRow {
            if (candidate.window.clampedUsedPercent != current.window.clampedUsedPercent) {
                return if (candidate.window.clampedUsedPercent > current.window.clampedUsedPercent) {
                    candidate
                } else {
                    current
                }
            }
            return when {
                current.window.resetsAtEpochMs == null && candidate.window.resetsAtEpochMs != null -> candidate
                current.window.resetsAtEpochMs != null && candidate.window.resetsAtEpochMs == null -> current
                current.window.resetsAtEpochMs != null &&
                    candidate.window.resetsAtEpochMs != null &&
                    candidate.window.resetsAtEpochMs < current.window.resetsAtEpochMs -> candidate
                else -> current
            }
        }

        private fun labelFor(window: RemodexRateLimitWindow, fallback: String): String {
            return durationLabel(window.windowDurationMins) ?: fallback
        }

        private fun displaySectionMetadata(
            bucket: RemodexRateLimitBucket,
            sparkModelAvailable: Boolean,
        ): DisplaySectionMetadata? {
            return if (bucket.isSparkBucket) {
                if (sparkModelAvailable) {
                    DisplaySectionMetadata(
                        id = "spark",
                        title = "GPT-5.3-Codex-Spark 额度",
                    )
                } else {
                    null
                }
            } else {
                DisplaySectionMetadata(
                    id = "default",
                    title = null,
                )
            }
        }

        private fun sectionOrder(sectionId: String): Int {
            return when (sectionId) {
                "default" -> 0
                "spark" -> 1
                else -> Int.MAX_VALUE
            }
        }

        fun durationLabel(minutes: Int?): String? {
            val value = minutes?.takeIf { it > 0 } ?: return null
            val weekMinutes = 7 * 24 * 60
            val dayMinutes = 24 * 60
            return when {
                value % weekMinutes == 0 -> "${value / weekMinutes} 周"
                value % dayMinutes == 0 -> "${value / dayMinutes} 天"
                value % 60 == 0 -> "${value / 60} 小时"
                else -> "${value} 分钟"
            }
        }

        private const val SPARK_MODEL_ID = "gpt-5.3-codex-spark"
    }
}

private data class DisplaySectionMetadata(
    val id: String,
    val title: String?,
)

private val RemodexRateLimitBucket.isSparkBucket: Boolean
    get() = buildList {
        add(limitId)
        limitName?.let(::add)
    }.any { value ->
        value.contains("spark", ignoreCase = true)
    }
