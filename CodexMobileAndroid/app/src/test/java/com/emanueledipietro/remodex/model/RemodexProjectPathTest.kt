package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemodexProjectPathTest {
    @Test
    fun `normalize project path preserves filesystem roots`() {
        assertEquals("/", normalizeRemodexFilesystemProjectPath("/"))
        assertEquals("~/", normalizeRemodexFilesystemProjectPath("~////"))
        assertEquals("C:/", normalizeRemodexFilesystemProjectPath("c:\\\\"))
    }

    @Test
    fun `normalize project path trims trailing separators from real paths`() {
        assertEquals("/tmp/remodex", normalizeRemodexFilesystemProjectPath("/tmp/remodex/"))
        assertEquals(
            "/tmp/remodex/.codex/worktrees/feature-a",
            normalizeRemodexFilesystemProjectPath("/tmp/remodex/.codex/worktrees/feature-a///"),
        )
    }

    @Test
    fun `normalize project path rejects pseudo buckets`() {
        assertNull(normalizeRemodexFilesystemProjectPath("cloud"))
        assertNull(normalizeRemodexFilesystemProjectPath("_default"))
        assertNull(normalizeRemodexFilesystemProjectPath("server"))
    }
}
