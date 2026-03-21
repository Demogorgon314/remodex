package com.emanueledipietro.remodex.feature.appshell

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.emanueledipietro.remodex.feature.onboarding.OnboardingScreen
import com.emanueledipietro.remodex.ui.theme.RemodexTheme
import org.junit.Rule
import org.junit.Test

class OnboardingScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun onboardingShowsQrPairingCallToAction() {
        composeRule.setContent {
            RemodexTheme {
                OnboardingScreen(onContinue = {})
            }
        }

        composeRule.onNodeWithText("Set Up QR Pairing").assertIsDisplayed()
        composeRule.onNodeWithText("Control Codex from your Android phone.").assertIsDisplayed()
    }
}
