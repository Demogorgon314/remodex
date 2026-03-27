package com.emanueledipietro.remodex.feature.turn

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationDiffRendererTest {
    @Test
    fun classify_recognizes_supported_diff_line_types() {
        assertEquals(ConversationDiffLineKind.META, ConversationDiffLineKind.classify("diff --git a/src/A.kt b/src/A.kt"))
        assertEquals(ConversationDiffLineKind.META, ConversationDiffLineKind.classify("deleted file mode 100644"))
        assertEquals(ConversationDiffLineKind.HUNK, ConversationDiffLineKind.classify("@@ -1 +1 @@"))
        assertEquals(ConversationDiffLineKind.ADDITION, ConversationDiffLineKind.classify("+after"))
        assertEquals(ConversationDiffLineKind.DELETION, ConversationDiffLineKind.classify("-before"))
        assertEquals(ConversationDiffLineKind.NEUTRAL, ConversationDiffLineKind.classify(" context"))
    }

    @Test
    fun cleanDisplayText_strips_diff_prefixes_for_visible_rows() {
        assertEquals("added", ConversationDiffLineKind.ADDITION.cleanDisplayText("+added"))
        assertEquals("removed", ConversationDiffLineKind.DELETION.cleanDisplayText("-removed"))
        assertEquals("context", ConversationDiffLineKind.NEUTRAL.cleanDisplayText(" context"))
        assertEquals("plain", ConversationDiffLineKind.NEUTRAL.cleanDisplayText("plain"))
    }

    @Test
    fun shouldRenderMarkdownCodeBlockAsDiff_only_accepts_explicit_diff_language() {
        assertTrue(shouldRenderMarkdownCodeBlockAsDiff("diff"))
        assertTrue(shouldRenderMarkdownCodeBlockAsDiff(" Diff "))
        assertFalse(shouldRenderMarkdownCodeBlockAsDiff("patch"))
        assertFalse(shouldRenderMarkdownCodeBlockAsDiff("kotlin"))
        assertFalse(shouldRenderMarkdownCodeBlockAsDiff(null))
    }
}
