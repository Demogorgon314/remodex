package com.emanueledipietro.remodex.model

import java.util.Locale

fun RemodexAppLanguage.usesSimplifiedChineseUi(): Boolean {
    return when (this) {
        RemodexAppLanguage.SIMPLIFIED_CHINESE -> true
        RemodexAppLanguage.ENGLISH -> false
        RemodexAppLanguage.SYSTEM -> Locale.getDefault().language.equals("zh", ignoreCase = true)
    }
}

fun remodexLocalizedText(
    zh: String,
    en: String,
): String {
    return if (Locale.getDefault().language.equals("zh", ignoreCase = true)) zh else en
}

fun remodexLocalizedText(
    usesSimplifiedChineseUi: Boolean,
    zh: String,
    en: String,
): String {
    return if (usesSimplifiedChineseUi) zh else en
}
