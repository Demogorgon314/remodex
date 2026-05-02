package com.emanueledipietro.remodex.model

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RemodexLocalizationTest {
    @After
    fun tearDown() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
    }

    @Test
    fun `uses app locale override for localized text`() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("zh-CN"))

        assertEquals("中文", remodexLocalizedText("中文", "English"))
    }

    @Test
    fun `uses English when app locale override is English`() {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))

        assertEquals("English", remodexLocalizedText("中文", "English"))
    }

    @Test
    fun `explicit app language choices do not depend on system locale`() {
        assertTrue(RemodexAppLanguage.SIMPLIFIED_CHINESE.usesSimplifiedChineseUi())
        assertFalse(RemodexAppLanguage.ENGLISH.usesSimplifiedChineseUi())
    }
}
