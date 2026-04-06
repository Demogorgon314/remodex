package com.emanueledipietro.remodex.feature.appshell

import com.emanueledipietro.remodex.model.RemodexThreadSummary
import com.emanueledipietro.remodex.model.RemodexThreadSyncState
import com.emanueledipietro.remodex.model.normalizeRemodexFilesystemProjectPath
import java.io.File

internal object RemodexWorktreeRouting {
    fun canonicalProjectPath(rawPath: String): String? {
        val normalizedPath = normalizeRemodexFilesystemProjectPath(rawPath) ?: return null
        val standardizedPath = runCatching {
            File(normalizedPath).toPath().normalize().toString()
        }.getOrNull()?.let(::normalizeRemodexFilesystemProjectPath) ?: normalizedPath
        return runCatching {
            normalizeRemodexFilesystemProjectPath(File(standardizedPath).canonicalPath)
        }.getOrNull() ?: standardizedPath
    }

    fun comparableProjectPath(rawPath: String?): String? {
        rawPath ?: return null
        return canonicalProjectPath(rawPath) ?: normalizeRemodexFilesystemProjectPath(rawPath)
    }

    fun matchingLiveThread(
        threads: List<RemodexThreadSummary>,
        projectPath: String,
    ): RemodexThreadSummary? {
        val resolvedProjectPath = comparableProjectPath(projectPath) ?: return null
        return threads.firstOrNull { thread ->
            thread.syncState == RemodexThreadSyncState.LIVE &&
                comparableProjectPath(thread.projectPath) == resolvedProjectPath
        }
    }

    fun liveThreadForCheckedOutElsewhereBranch(
        projectPath: String?,
        currentThread: RemodexThreadSummary,
        threads: List<RemodexThreadSummary>,
    ): RemodexThreadSummary? {
        val resolvedProjectPath = comparableProjectPath(projectPath) ?: return null
        val currentComparablePath = comparableProjectPath(currentThread.projectPath)
        if (currentComparablePath == resolvedProjectPath) {
            return null
        }

        return matchingLiveThread(
            threads = threads,
            projectPath = resolvedProjectPath,
        )
    }
}
