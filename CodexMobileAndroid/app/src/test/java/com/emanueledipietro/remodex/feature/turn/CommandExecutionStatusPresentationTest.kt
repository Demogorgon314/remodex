package com.emanueledipietro.remodex.feature.turn

import com.emanueledipietro.remodex.model.ConversationItemKind
import com.emanueledipietro.remodex.model.ConversationSpeaker
import com.emanueledipietro.remodex.model.RemodexCommandExecutionDetails
import com.emanueledipietro.remodex.model.RemodexConversationItem
import org.junit.Assert.assertEquals
import org.junit.Test

class CommandExecutionStatusPresentationTest {
    @Test
    fun `resolved command status falls back to completed once row stops streaming`() {
        val item = RemodexConversationItem(
            id = "command-1",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.COMMAND_EXECUTION,
            text = "running pwd",
            isStreaming = false,
            orderIndex = 1L,
        )

        val resolved = resolvedCommandExecutionStatusPresentations(
            item = item,
            details = RemodexCommandExecutionDetails(
                fullCommand = "pwd",
                durationMs = 120,
            ),
        ).single()

        assertEquals("completed", resolved.statusLabel)
        assertEquals(CommandExecutionStatusAccent.COMPLETED, resolved.accent)
        assertEquals("pwd", resolved.command)
    }

    @Test
    fun `resolved command status prefers failed when exit code is non zero`() {
        val item = RemodexConversationItem(
            id = "command-2",
            speaker = ConversationSpeaker.SYSTEM,
            kind = ConversationItemKind.COMMAND_EXECUTION,
            text = "running ./gradlew test",
            isStreaming = false,
            orderIndex = 1L,
        )

        val resolved = resolvedCommandExecutionStatusPresentations(
            item = item,
            details = RemodexCommandExecutionDetails(
                fullCommand = "./gradlew test",
                exitCode = 1,
                durationMs = 4200,
            ),
        ).single()

        assertEquals("failed", resolved.statusLabel)
        assertEquals(CommandExecutionStatusAccent.FAILED, resolved.accent)
        assertEquals("./gradlew test", resolved.command)
    }
}
