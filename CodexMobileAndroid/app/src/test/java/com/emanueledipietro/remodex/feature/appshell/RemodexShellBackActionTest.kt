package com.emanueledipietro.remodex.feature.appshell

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemodexShellBackActionTest {
    @Test
    fun `scanner dismissal wins over every other shell back action`() {
        assertEquals(
            ShellBackAction.DISMISS_SCANNER,
            resolveShellBackAction(
                isScannerPresented = true,
                isCompactSidebarOpen = true,
                shellRoute = ShellRoute.ABOUT_REMODEX,
            ),
        )
    }

    @Test
    fun `compact sidebar closes before secondary routes`() {
        assertEquals(
            ShellBackAction.CLOSE_SIDEBAR,
            resolveShellBackAction(
                isScannerPresented = false,
                isCompactSidebarOpen = true,
                shellRoute = ShellRoute.SETTINGS,
            ),
        )
    }

    @Test
    fun `about route falls back to settings before content`() {
        assertEquals(
            ShellBackAction.NAVIGATE_TO_SETTINGS,
            resolveShellBackAction(
                isScannerPresented = false,
                isCompactSidebarOpen = false,
                shellRoute = ShellRoute.ABOUT_REMODEX,
            ),
        )
    }

    @Test
    fun `settings route falls back to content before exiting`() {
        assertEquals(
            ShellBackAction.NAVIGATE_TO_CONTENT,
            resolveShellBackAction(
                isScannerPresented = false,
                isCompactSidebarOpen = false,
                shellRoute = ShellRoute.SETTINGS,
            ),
        )
    }

    @Test
    fun `returns null when the shell has nothing left to consume`() {
        assertNull(
            resolveShellBackAction(
                isScannerPresented = false,
                isCompactSidebarOpen = false,
                shellRoute = ShellRoute.CONTENT,
            ),
        )
    }
}
