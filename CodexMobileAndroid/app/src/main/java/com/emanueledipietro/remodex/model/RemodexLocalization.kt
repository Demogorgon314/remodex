package com.emanueledipietro.remodex.model

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

fun RemodexAppLanguage.usesSimplifiedChineseUi(): Boolean {
    return when (this) {
        RemodexAppLanguage.SIMPLIFIED_CHINESE -> true
        RemodexAppLanguage.ENGLISH -> false
        RemodexAppLanguage.SYSTEM -> currentRemodexLocaleUsesSimplifiedChineseUi()
    }
}

fun remodexLocalizedText(
    zh: String,
    en: String,
): String {
    return if (currentRemodexLocaleUsesSimplifiedChineseUi()) zh else en
}

fun remodexLocalizedText(
    usesSimplifiedChineseUi: Boolean,
    zh: String,
    en: String,
): String {
    return if (usesSimplifiedChineseUi) zh else en
}

internal fun currentRemodexLocaleUsesSimplifiedChineseUi(): Boolean {
    val appLocales = AppCompatDelegate.getApplicationLocales()
    val locale = appLocales.firstLocaleOrNull()
        ?: LocaleListCompat.getAdjustedDefault().firstLocaleOrNull()
        ?: Locale.getDefault()
    return locale.language.equals("zh", ignoreCase = true)
}

private fun LocaleListCompat.firstLocaleOrNull(): Locale? {
    return if (isEmpty) null else get(0)
}
