package com.emanueledipietro.remodex.data.app

import com.emanueledipietro.remodex.model.RemodexAppUpdateState
import org.junit.Assert.assertEquals
import org.junit.Test

class AppUpdateCheckerTest {
    @Test
    fun `latest GitHub release response maps update available with apk asset`() {
        val status = decodeGitHubLatestReleaseUpdateStatus(
            currentVersion = "1.2.3",
            responseBody = """
                {
                  "tag_name": "v1.2.4",
                  "html_url": "https://github.com/Demogorgon314/remodex-android/releases/tag/v1.2.4",
                  "assets": [
                    { "name": "remodex-android-1.2.4.apk" },
                    { "name": "remodex-android-1.2.4.apk.sha256" }
                  ]
                }
            """.trimIndent(),
        )

        assertEquals(RemodexAppUpdateState.UPDATE_AVAILABLE, status.state)
        assertEquals("1.2.3", status.currentVersion)
        assertEquals("1.2.4", status.latestVersion)
        assertEquals("remodex-android-1.2.4.apk", status.apkAssetName)
        assertEquals(
            "https://github.com/Demogorgon314/remodex-android/releases/tag/v1.2.4",
            status.releaseUrl,
        )
    }

    @Test
    fun `latest GitHub release response maps matching version as up to date`() {
        val status = decodeGitHubLatestReleaseUpdateStatus(
            currentVersion = "1.2.4",
            responseBody = """
                {
                  "tag_name": "v1.2.4",
                  "html_url": "https://github.com/Demogorgon314/remodex-android/releases/tag/v1.2.4",
                  "assets": []
                }
            """.trimIndent(),
        )

        assertEquals(RemodexAppUpdateState.UP_TO_DATE, status.state)
        assertEquals("1.2.4", status.latestVersion)
    }
}
