package com.emanueledipietro.remodex.model

data class RemodexCommandExecutionDetails(
    val fullCommand: String,
    val cwd: String? = null,
    val exitCode: Int? = null,
    val durationMs: Int? = null,
    val outputTail: String = "",
) {
    fun appendedOutput(chunk: String): RemodexCommandExecutionDetails {
        if (chunk.isEmpty()) {
            return this
        }
        val combined = outputTail + chunk
        val trimmed = combined
            .split('\n')
            .takeLast(MaxOutputLines)
            .joinToString(separator = "\n")
        return copy(outputTail = trimmed)
    }

    companion object {
        const val MaxOutputLines = 30
    }
}
