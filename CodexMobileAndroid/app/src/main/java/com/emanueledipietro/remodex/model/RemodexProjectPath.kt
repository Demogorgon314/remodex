package com.emanueledipietro.remodex.model

fun normalizeRemodexFilesystemProjectPath(value: String?): String? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        return null
    }

    normalizedFilesystemRootPath(trimmed)?.let { return it }

    var normalized = trimmed
    while (normalized.endsWith("/") || normalized.endsWith("\\")) {
        normalized = normalized.dropLast(1)
    }

    if (normalized.isEmpty()) {
        return "/"
    }

    if (!isLikelyFilesystemPath(normalized)) {
        return null
    }

    return normalized
}

fun remodexProjectDisplayLabel(projectPath: String?): String {
    val normalizedPath = normalizeRemodexFilesystemProjectPath(projectPath) ?: return "Cloud"
    return normalizedPath
        .substringAfterLast('/')
        .ifBlank { normalizedPath }
}

fun isCodexManagedWorktreeProject(projectPath: String): Boolean {
    val normalizedPath = normalizeRemodexFilesystemProjectPath(projectPath) ?: return false
    val components = normalizedPath
        .replace('\\', '/')
        .split('/')
        .filter(String::isNotBlank)
    val worktreesIndex = components.indexOf("worktrees")
    return worktreesIndex > 0 && components.getOrNull(worktreesIndex - 1) == ".codex"
}

private fun normalizedFilesystemRootPath(value: String): String? {
    if (value == "/") {
        return "/"
    }
    if (value.firstOrNull() == '~' && value.drop(1).all { it == '/' || it == '\\' }) {
        return "~/"
    }
    if (value.length >= 3 &&
        value[0].isLetter() &&
        value[1] == ':' &&
        (value[2] == '/' || value[2] == '\\') &&
        value.drop(3).all { it == '/' || it == '\\' }
    ) {
        return value[0].uppercaseChar().toString() + ":/"
    }
    return null
}

private fun isLikelyFilesystemPath(value: String): Boolean {
    if (value == "/") {
        return true
    }
    if (value.startsWith("/") || value.startsWith("~/")) {
        return true
    }
    if (value.length >= 3 &&
        value[0].isLetter() &&
        value[1] == ':' &&
        (value[2] == '/' || value[2] == '\\')
    ) {
        return true
    }
    return value.startsWith("\\\\") || value.startsWith("//")
}
