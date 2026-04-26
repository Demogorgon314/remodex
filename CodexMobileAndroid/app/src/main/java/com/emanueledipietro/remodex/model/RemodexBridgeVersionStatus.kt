package com.emanueledipietro.remodex.model

data class RemodexBridgeVersionStatus(
    val installedVersion: String? = null,
    val latestVersion: String? = null,
) {
    val statusLabel: String
        get() {
            if (installedVersion == null) {
                return "Unknown"
            }
            if (latestVersion == null) {
                return "Installed"
            }
            return when {
                installedVersion == latestVersion -> "Up to date"
                installedVersion.compareToVersion(latestVersion) < 0 -> "Update available"
                else -> "Different build"
            }
        }

    val guidanceText: String
        get() {
            if (installedVersion == null) {
                return "Connect to a computer bridge to read the installed package version."
            }
            if (latestVersion == null) {
                return "Installed version detected. The latest published package is unavailable right now."
            }
            return when {
                installedVersion == latestVersion -> "The installed bridge matches the latest published package."
                installedVersion.compareToVersion(latestVersion) < 0 -> "A newer Remodex package is available on npm."
                else -> "This computer is running a different build than the current npm latest."
            }
        }

    val shouldHighlightInstalledVersion: Boolean
        get() = installedVersion != null && latestVersion != null && installedVersion.compareToVersion(latestVersion) < 0
}

private fun String?.compareToVersion(other: String?): Int {
    val left = this?.trim().orEmpty()
    val right = other?.trim().orEmpty()
    if (left.isEmpty() || right.isEmpty()) {
        return left.compareTo(right)
    }
    return left.split('.').zipAll(right.split('.')).firstNotNullOfOrNull { (lhs, rhs) ->
        val lhsValue = lhs?.toIntOrNull() ?: 0
        val rhsValue = rhs?.toIntOrNull() ?: 0
        when {
            lhsValue < rhsValue -> -1
            lhsValue > rhsValue -> 1
            else -> null
        }
    } ?: 0
}

private fun <T> List<T>.zipAll(other: List<T>): List<Pair<T?, T?>> {
    val maxSize = maxOf(size, other.size)
    return List(maxSize) { index ->
        getOrNull(index) to other.getOrNull(index)
    }
}
