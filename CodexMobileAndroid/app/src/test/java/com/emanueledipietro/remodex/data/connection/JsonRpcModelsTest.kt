package com.emanueledipietro.remodex.data.connection

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class JsonRpcModelsTest {
    @Test
    fun `first string trims surrounding whitespace`() {
        val payload = buildJsonObject {
            put("delta", JsonPrimitive("  line\n"))
        }

        assertEquals("line", payload.firstString("delta"))
    }

    @Test
    fun `first raw string preserves surrounding whitespace`() {
        val payload = buildJsonObject {
            put("delta", JsonPrimitive("  line\n"))
            put("blank", JsonPrimitive("   "))
            put("empty", JsonPrimitive(""))
        }

        assertEquals("  line\n", payload.firstRawString("delta"))
        assertEquals("   ", payload.firstRawString("blank"))
        assertEquals("", payload.firstRawString("empty"))
        assertNull(payload.firstRawString("missing"))
    }
}
