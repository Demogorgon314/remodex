package com.emanueledipietro.remodex.data.connection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrustedSessionResolverTest {
    @Test
    fun `trusted session resolve url normalizes relay endpoint`() {
        assertEquals(
            "http://127.0.0.1:7777/v1/trusted/session/resolve",
            trustedSessionResolveUrl("ws://127.0.0.1:7777/relay"),
        )
        assertEquals(
            "https://example.com/v1/trusted/session/resolve",
            trustedSessionResolveUrl("wss://example.com/relay"),
        )
    }

    @Test
    fun `trusted session resolve url rejects unsupported relay schemes`() {
        assertNull(trustedSessionResolveUrl("ftp://example.com/relay"))
        assertNull(trustedSessionResolveUrl(""))
    }
}
