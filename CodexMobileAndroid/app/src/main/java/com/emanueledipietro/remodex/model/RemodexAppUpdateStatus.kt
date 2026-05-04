package com.emanueledipietro.remodex.model

enum class RemodexAppUpdateState {
    IDLE,
    CHECKING,
    UP_TO_DATE,
    UPDATE_AVAILABLE,
    FAILED,
}

data class RemodexAppUpdateStatus(
    val currentVersion: String? = null,
    val latestVersion: String? = null,
    val releaseUrl: String? = null,
    val apkAssetName: String? = null,
    val state: RemodexAppUpdateState = RemodexAppUpdateState.IDLE,
    val errorMessage: String? = null,
) {
    val statusLabel: String
        get() = when (state) {
            RemodexAppUpdateState.IDLE -> "Not checked"
            RemodexAppUpdateState.CHECKING -> "Checking"
            RemodexAppUpdateState.UP_TO_DATE -> "Up to date"
            RemodexAppUpdateState.UPDATE_AVAILABLE -> "Update available"
            RemodexAppUpdateState.FAILED -> "Check failed"
        }

    val guidanceText: String
        get() = when (state) {
            RemodexAppUpdateState.IDLE -> "Check GitHub Releases for the latest Android APK."
            RemodexAppUpdateState.CHECKING -> "Checking GitHub Releases..."
            RemodexAppUpdateState.UP_TO_DATE -> "This APK matches the latest GitHub release."
            RemodexAppUpdateState.UPDATE_AVAILABLE -> "A newer Android APK is available on GitHub Releases."
            RemodexAppUpdateState.FAILED -> errorMessage?.takeIf(String::isNotBlank)
                ?: "Unable to check GitHub Releases right now."
        }
}

fun remodexNormalizeReleaseVersion(value: String?): String? {
    return value
        ?.trim()
        ?.removePrefix("refs/tags/")
        ?.removePrefix("v")
        ?.removePrefix("V")
        ?.takeIf(String::isNotEmpty)
}

fun remodexCompareReleaseVersions(
    currentVersion: String?,
    latestVersion: String?,
): Int {
    val left = remodexNormalizeReleaseVersion(currentVersion).orEmpty()
    val right = remodexNormalizeReleaseVersion(latestVersion).orEmpty()
    if (left.isEmpty() || right.isEmpty()) {
        return left.compareTo(right)
    }

    val leftParts = left.split('.', '-', '_')
    val rightParts = right.split('.', '-', '_')
    val maxSize = maxOf(leftParts.size, rightParts.size)
    repeat(maxSize) { index ->
        val lhs = leftParts.getOrNull(index).orEmpty()
        val rhs = rightParts.getOrNull(index).orEmpty()
        val lhsNumber = lhs.toIntOrNull()
        val rhsNumber = rhs.toIntOrNull()
        val comparison = if (lhsNumber != null && rhsNumber != null) {
            lhsNumber.compareTo(rhsNumber)
        } else {
            lhs.compareTo(rhs, ignoreCase = true)
        }
        if (comparison != 0) {
            return comparison
        }
    }
    return 0
}
