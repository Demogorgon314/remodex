package com.emanueledipietro.remodex.model

enum class RemodexGptAccountStatus {
    UNKNOWN,
    UNAVAILABLE,
    NOT_LOGGED_IN,
    LOGIN_PENDING,
    AUTHENTICATED,
    EXPIRED,
}

enum class RemodexGptAuthMethod {
    CHATGPT,
}

data class RemodexGptAccountSnapshot(
    val status: RemodexGptAccountStatus = RemodexGptAccountStatus.UNKNOWN,
    val authMethod: RemodexGptAuthMethod? = null,
    val email: String? = null,
    val displayName: String? = null,
    val planType: String? = null,
    val loginInFlight: Boolean = false,
    val needsReauth: Boolean = false,
    val expiresAtEpochMs: Long? = null,
    val tokenReady: Boolean? = null,
    val tokenUnavailableSinceEpochMs: Long? = null,
    val updatedAtEpochMs: Long = 0L,
) {
    val hasActiveLogin: Boolean
        get() = loginInFlight || status == RemodexGptAccountStatus.LOGIN_PENDING

    val isAuthenticated: Boolean
        get() = status == RemodexGptAccountStatus.AUTHENTICATED && !needsReauth

    val canLogout: Boolean
        get() = isAuthenticated || needsReauth

    val isVoiceTokenReady: Boolean
        get() = tokenReady ?: isAuthenticated

    val statusLabel: String
        get() = when (status) {
            RemodexGptAccountStatus.UNKNOWN -> "Unknown"
            RemodexGptAccountStatus.UNAVAILABLE -> "Unavailable"
            RemodexGptAccountStatus.NOT_LOGGED_IN -> "Not logged in"
            RemodexGptAccountStatus.LOGIN_PENDING -> "Login pending"
            RemodexGptAccountStatus.AUTHENTICATED -> if (needsReauth) "Needs reauth" else "Authenticated"
            RemodexGptAccountStatus.EXPIRED -> "Expired"
        }

    val detailText: String?
        get() = buildList {
            email?.trim()?.takeIf(String::isNotEmpty)?.let(::add)
            planType?.trim()?.takeIf(String::isNotEmpty)?.replaceFirstChar(Char::titlecase)?.let(::add)
            expiresAtEpochMs?.let { epochMs ->
                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(java.util.Date(epochMs))
            }?.let(::add)
            if (isAuthenticated && !isVoiceTokenReady) {
                add("Voice syncing")
            }
        }.takeIf(List<String>::isNotEmpty)?.joinToString(separator = " • ")

    companion object {
        const val VoiceTokenGraceIntervalMs: Long = 45_000L
    }
}

fun remodexInitialGptAccountSnapshot(): RemodexGptAccountSnapshot = RemodexGptAccountSnapshot()
