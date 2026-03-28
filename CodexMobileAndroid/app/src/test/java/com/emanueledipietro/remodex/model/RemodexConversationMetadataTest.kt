package com.emanueledipietro.remodex.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.charset.StandardCharsets

class RemodexConversationMetadataTest {
    @Test
    fun `conversation attachment prefers preview data url for rendering`() {
        val attachment = RemodexConversationAttachment(
            id = "attachment-1",
            uriString = "content://media/external/images/media/1",
            displayName = "photo.jpg",
            previewDataUrl = "data:image/jpeg;base64,AAAA",
        )

        assertEquals("data:image/jpeg;base64,AAAA", attachment.renderUriString)
    }

    @Test
    fun `composer attachment carries payload data url into conversation attachment preview`() {
        val attachment = RemodexComposerAttachment(
            id = "attachment-1",
            uriString = "content://media/external/images/media/1",
            displayName = "photo.jpg",
            payloadDataUrl = "data:image/jpeg;base64,BBBB",
        )

        val conversationAttachment = attachment.toConversationAttachment()

        assertEquals("content://media/external/images/media/1", conversationAttachment.uriString)
        assertEquals("data:image/jpeg;base64,BBBB", conversationAttachment.previewDataUrl)
        assertEquals("data:image/jpeg;base64,BBBB", conversationAttachment.renderUriString)
    }

    @Test
    fun `inline image data url uses stable fallback display name`() {
        assertEquals(
            "image-1",
            fallbackConversationImageDisplayName(
                uriString = "data:image/jpeg;base64,/9k=",
                attachmentIndex = 0,
            ),
        )
        assertEquals(
            "photo.jpg",
            fallbackConversationImageDisplayName(
                uriString = "content://media/external/images/media/1/photo.jpg",
                attachmentIndex = 0,
            ),
        )
    }

    @Test
    fun `inline image data url decodes payload bytes`() {
        val decoded = decodeInlineImageDataUrlBytes("data:image/jpeg;base64,SGk=")

        assertEquals("Hi", decoded?.toString(StandardCharsets.UTF_8))
    }

    @Test
    fun `inline image data url decoder rejects invalid payloads`() {
        assertNull(decodeInlineImageDataUrlBytes("data:image/jpeg;base64,%%%"))
        assertNull(decodeInlineImageDataUrlBytes("content://media/external/images/media/1"))
    }

    @Test
    fun `agent rows mark top level model as requested hint when receiver model is missing`() {
        val action = RemodexSubagentAction(
            tool = "spawnAgent",
            status = "completed",
            model = "gpt-5.4-mini",
            receiverThreadIds = listOf("child-thread-1"),
        )

        val row = action.agentRows.single()

        assertEquals("gpt-5.4-mini", row.model)
        assertTrue(row.modelIsRequestedHint)
    }

    @Test
    fun `agent rows prefer receiver model without requested hint`() {
        val action = RemodexSubagentAction(
            tool = "spawnAgent",
            status = "completed",
            model = "gpt-5.4-mini",
            receiverAgents = listOf(
                RemodexSubagentRef(
                    threadId = "child-thread-1",
                    model = "gpt-5.4",
                    nickname = "Locke",
                    role = "explorer",
                ),
            ),
        )

        val row = action.agentRows.single()

        assertEquals("gpt-5.4", row.model)
        assertFalse(row.modelIsRequestedHint)
        assertEquals("Locke [explorer]", row.displayLabel)
    }
}
