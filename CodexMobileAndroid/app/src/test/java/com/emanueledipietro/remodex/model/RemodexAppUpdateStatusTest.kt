package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Test

class RemodexAppUpdateStatusTest {
    @Test
    fun `release version normalization removes tag prefix`() {
        assertEquals("1.2.3", remodexNormalizeReleaseVersion("v1.2.3"))
        assertEquals("1.2.3", remodexNormalizeReleaseVersion("refs/tags/v1.2.3"))
    }

    @Test
    fun `release version comparison handles numeric segments`() {
        assertEquals(-1, remodexCompareReleaseVersions("1.2.9", "v1.2.10"))
        assertEquals(0, remodexCompareReleaseVersions("1.2.3", "v1.2.3"))
        assertEquals(1, remodexCompareReleaseVersions("1.3.0", "v1.2.9"))
    }
}
