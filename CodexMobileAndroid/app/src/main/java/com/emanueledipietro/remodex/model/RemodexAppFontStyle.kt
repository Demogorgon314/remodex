package com.emanueledipietro.remodex.model

enum class RemodexAppFontStyle(
    val title: String,
    val subtitle: String,
) {
    SYSTEM(
        title = "System",
        subtitle = "Use the native Android font for regular text. Code stays monospaced.",
    ),
    GEIST(
        title = "Geist",
        subtitle = "Use Geist for regular text. Code stays monospaced.",
    ),
    GEIST_MONO(
        title = "Geist Mono",
        subtitle = "Use Geist Mono for regular text and code.",
    ),
    JETBRAINS_MONO(
        title = "JetBrains Mono",
        subtitle = "Use JetBrains Mono for regular text and code.",
    ),
}
