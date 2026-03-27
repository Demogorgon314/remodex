package com.emanueledipietro.remodex.feature.appshell

import org.junit.Assert.assertEquals
import org.junit.Test

class ShellTopBarTitleLayoutTest {
    @Test
    fun `content route centers title when no thread is selected`() {
        assertEquals(
            ShellTopBarTitleLayout.CENTERED,
            resolveShellTopBarTitleLayout(
                shellRoute = ShellRoute.CONTENT,
                hasSelectedThread = false,
            ),
        )
    }

    @Test
    fun `content route keeps thread titles leading aligned when a thread is selected`() {
        assertEquals(
            ShellTopBarTitleLayout.LEADING,
            resolveShellTopBarTitleLayout(
                shellRoute = ShellRoute.CONTENT,
                hasSelectedThread = true,
            ),
        )
    }

    @Test
    fun `secondary routes center their titles like ios navigation chrome`() {
        assertEquals(
            ShellTopBarTitleLayout.CENTERED,
            resolveShellTopBarTitleLayout(
                shellRoute = ShellRoute.SETTINGS,
                hasSelectedThread = true,
            ),
        )
        assertEquals(
            ShellTopBarTitleLayout.CENTERED,
            resolveShellTopBarTitleLayout(
                shellRoute = ShellRoute.ABOUT_REMODEX,
                hasSelectedThread = false,
            ),
        )
        assertEquals(
            ShellTopBarTitleLayout.CENTERED,
            resolveShellTopBarTitleLayout(
                shellRoute = ShellRoute.ARCHIVED_CHATS,
                hasSelectedThread = false,
            ),
        )
    }
}
