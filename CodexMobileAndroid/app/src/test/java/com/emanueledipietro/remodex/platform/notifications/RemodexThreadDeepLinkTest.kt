package com.emanueledipietro.remodex.platform.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemodexThreadDeepLinkTest {
    @Test
    fun `thread route round-trips the notification thread id`() {
        val route = buildThreadRoute("thread-android-client")

        assertEquals("thread-android-client", parseThreadIdFromRoute(route))
    }

    @Test
    fun `invalid routes do not produce a thread id`() {
        assertNull(parseThreadIdFromRoute("remodex://settings"))
    }
}
