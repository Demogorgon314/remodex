package com.emanueledipietro.remodex.platform.window

enum class RemodexWindowLayout {
    COMPACT,
    EXPANDED,
}

fun remodexWindowLayout(screenWidthDp: Int): RemodexWindowLayout {
    return if (screenWidthDp >= 840) {
        RemodexWindowLayout.EXPANDED
    } else {
        RemodexWindowLayout.COMPACT
    }
}
