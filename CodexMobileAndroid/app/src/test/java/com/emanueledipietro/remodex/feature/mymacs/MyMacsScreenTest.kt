package com.emanueledipietro.remodex.feature.mymacs

import com.emanueledipietro.remodex.model.RemodexBridgeProfilePresentation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyMacsScreenTest {
    @Test
    fun `resolve current mac profile prefers active profile while offline`() {
        val activeOfflineProfile = bridgeProfile(
            profileId = "profile-active",
            isActive = true,
            isConnected = false,
        )
        val connectedInactiveProfile = bridgeProfile(
            profileId = "profile-connected",
            isActive = false,
            isConnected = true,
        )

        val currentProfile = resolveCurrentMacProfile(
            profiles = listOf(connectedInactiveProfile, activeOfflineProfile),
        )

        assertEquals("profile-active", currentProfile?.profileId)
    }

    @Test
    fun `current mac label reflects offline selected profile`() {
        assertEquals(
            "已选择",
            currentMacConnectionLabel(
                bridgeProfile(
                    profileId = "profile-active",
                    isActive = true,
                    isConnected = false,
                ),
            ),
        )
    }

    @Test
    fun `resolve current mac profile falls back to connected profile`() {
        val connectedProfile = bridgeProfile(
            profileId = "profile-connected",
            isActive = false,
            isConnected = true,
        )

        assertEquals(
            "profile-connected",
            resolveCurrentMacProfile(listOf(connectedProfile))?.profileId,
        )
        assertNull(resolveCurrentMacProfile(emptyList()))
    }

    private fun bridgeProfile(
        profileId: String,
        isActive: Boolean,
        isConnected: Boolean,
    ): RemodexBridgeProfilePresentation {
        return RemodexBridgeProfilePresentation(
            profileId = profileId,
            title = "Saved Pair",
            name = "Kai Mac",
            isActive = isActive,
            isConnected = isConnected,
            macDeviceId = "device-$profileId",
        )
    }
}
