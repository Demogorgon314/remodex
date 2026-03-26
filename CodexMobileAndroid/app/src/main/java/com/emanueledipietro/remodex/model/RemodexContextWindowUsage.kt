package com.emanueledipietro.remodex.model

data class RemodexContextWindowUsage(
    val tokensUsed: Int,
    val tokenLimit: Int,
) {
    val fractionUsed: Double
        get() = if (tokenLimit <= 0) 0.0 else tokensUsed.toDouble() / tokenLimit.toDouble()

    val percentRemaining: Int
        get() = (100 - (fractionUsed * 100).toInt()).coerceIn(0, 100)
}
