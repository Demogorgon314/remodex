package com.emanueledipietro.remodex.model

data class RemodexUsageStatus(
    val contextWindowUsage: RemodexContextWindowUsage? = null,
    val rateLimitBuckets: List<RemodexRateLimitBucket> = emptyList(),
    val rateLimitsErrorMessage: String? = null,
)
