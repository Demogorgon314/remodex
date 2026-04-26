package com.emanueledipietro.remodex.model

enum class RemodexAppLanguage(
    val title: String,
    val subtitle: String,
    val localeTag: String,
) {
    SYSTEM(
        title = "System",
        subtitle = "Follow Android language settings.",
        localeTag = "",
    ),
    ENGLISH(
        title = "English",
        subtitle = "Use English in app UI.",
        localeTag = "en",
    ),
    SIMPLIFIED_CHINESE(
        title = "简体中文",
        subtitle = "使用中文显示应用界面.",
        localeTag = "zh-CN",
    ),
}
