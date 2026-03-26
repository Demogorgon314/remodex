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
            val dedupedByLabel = linkedMapOf<String, RemodexRateLimitDisplayRow>()
            buckets.flatMap(RemodexRateLimitBucket::displayRows).forEach { row ->
                val existing = dedupedByLabel[row.label]
                if (existing == null) {
                    dedupedByLabel[row.label] = row
                } else {
                    dedupedByLabel[row.label] = preferredDisplayRow(existing, row)
                }
            }
            return dedupedByLabel.values.sortedWith(
                compareBy<RemodexRateLimitDisplayRow> { it.window.windowDurationMins ?: Int.MAX_VALUE }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.label },
            )
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

        fun durationLabel(minutes: Int?): String? {
            val value = minutes?.takeIf { it > 0 } ?: return null
            val weekMinutes = 7 * 24 * 60
            val dayMinutes = 24 * 60
            return when {
                value % weekMinutes == 0 -> if (value == weekMinutes) "Weekly" else "${value / weekMinutes}w"
                value % dayMinutes == 0 -> "${value / dayMinutes}d"
                value % 60 == 0 -> "${value / 60}h"
                else -> "${value}m"
            }
        }
    }
}
