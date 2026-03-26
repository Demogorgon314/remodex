package com.emanueledipietro.remodex.data.app

import com.emanueledipietro.remodex.data.connection.firstArray
import com.emanueledipietro.remodex.data.connection.firstBoolean
import com.emanueledipietro.remodex.data.connection.firstDouble
import com.emanueledipietro.remodex.data.connection.firstInt
import com.emanueledipietro.remodex.data.connection.firstObject
import com.emanueledipietro.remodex.data.connection.firstString
import com.emanueledipietro.remodex.data.connection.jsonObjectOrNull
import com.emanueledipietro.remodex.model.RemodexBridgeVersionStatus
import com.emanueledipietro.remodex.model.RemodexContextWindowUsage
import com.emanueledipietro.remodex.model.RemodexGptAccountSnapshot
import com.emanueledipietro.remodex.model.RemodexGptAccountStatus
import com.emanueledipietro.remodex.model.RemodexGptAuthMethod
import com.emanueledipietro.remodex.model.RemodexRateLimitBucket
import com.emanueledipietro.remodex.model.RemodexRateLimitWindow
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import java.time.Instant

internal fun decodeBridgeVersionStatus(payloadObject: JsonObject): RemodexBridgeVersionStatus {
    return RemodexBridgeVersionStatus(
        installedVersion = normalizeVersion(
            payloadObject.firstString(
                "bridgeVersion",
                "bridge_version",
                "bridgePackageVersion",
                "bridge_package_version",
            ),
        ),
        latestVersion = normalizeVersion(
            payloadObject.firstString(
                "bridgeLatestVersion",
                "bridge_latest_version",
                "bridgePublishedVersion",
                "bridge_published_version",
            ),
        ),
    )
}

internal fun decodeBridgeGptAccountSnapshot(
    payloadObject: JsonObject,
    previousSnapshot: RemodexGptAccountSnapshot,
): RemodexGptAccountSnapshot {
    val parsedStatus = decodeGptAccountStatus(payloadObject.firstString("status", "state"))
    val bridgeReportedPendingLogin = payloadObject.firstBoolean("loginInFlight", "login_in_flight") ?: false
    val baseNeedsReauth = payloadObject.firstBoolean("needsReauth", "needs_reauth") ?: false
    val authMethod = decodeGptAuthMethod(payloadObject.firstString("authMethod", "auth_mode"))
    val hasLegacyAuthToken = payloadObject.firstString("authToken", "auth_token") != null
    val resolvedStatus = when {
        parsedStatus == RemodexGptAccountStatus.AUTHENTICATED || parsedStatus == RemodexGptAccountStatus.EXPIRED ->
            parsedStatus
        parsedStatus == RemodexGptAccountStatus.UNKNOWN && hasLegacyAuthToken && authMethod != null && !baseNeedsReauth ->
            RemodexGptAccountStatus.AUTHENTICATED
        parsedStatus == RemodexGptAccountStatus.NOT_LOGGED_IN && bridgeReportedPendingLogin && !baseNeedsReauth ->
            RemodexGptAccountStatus.LOGIN_PENDING
        parsedStatus == RemodexGptAccountStatus.UNKNOWN && bridgeReportedPendingLogin ->
            RemodexGptAccountStatus.LOGIN_PENDING
        parsedStatus == RemodexGptAccountStatus.UNKNOWN ->
            RemodexGptAccountStatus.NOT_LOGGED_IN
        else -> parsedStatus
    }
    val tokenReady = payloadObject.firstBoolean("tokenReady", "token_ready")
        ?: (hasLegacyAuthToken && resolvedStatus == RemodexGptAccountStatus.AUTHENTICATED && !baseNeedsReauth)
        ?: (resolvedStatus == RemodexGptAccountStatus.AUTHENTICATED && !baseNeedsReauth)
    val tokenUnavailableSinceEpochMs = resolvedTokenUnavailableSinceEpochMs(
        previousSnapshot = previousSnapshot,
        status = resolvedStatus,
        needsReauth = baseNeedsReauth,
        tokenReady = tokenReady,
    )
    val needsReauth = resolvedNeedsReauth(
        previousSnapshot = previousSnapshot,
        baseNeedsReauth = baseNeedsReauth,
        status = resolvedStatus,
        tokenReady = tokenReady,
        tokenUnavailableSinceEpochMs = tokenUnavailableSinceEpochMs,
    )
    return RemodexGptAccountSnapshot(
        status = resolvedStatus,
        authMethod = authMethod,
        email = payloadObject.firstString("email"),
        displayName = null,
        planType = payloadObject.firstString("planType", "plan_type"),
        loginInFlight = bridgeReportedPendingLogin,
        needsReauth = needsReauth,
        expiresAtEpochMs = payloadObject.firstValueAsEpochMs("expiresAt", "expires_at"),
        tokenReady = tokenReady,
        tokenUnavailableSinceEpochMs = tokenUnavailableSinceEpochMs,
        updatedAtEpochMs = System.currentTimeMillis(),
    )
}

internal fun disconnectedGptAccountSnapshot(
    currentSnapshot: RemodexGptAccountSnapshot,
): RemodexGptAccountSnapshot {
    return RemodexGptAccountSnapshot(
        status = RemodexGptAccountStatus.UNAVAILABLE,
        authMethod = currentSnapshot.authMethod,
        email = currentSnapshot.email,
        displayName = currentSnapshot.displayName,
        planType = currentSnapshot.planType,
        loginInFlight = currentSnapshot.hasActiveLogin,
        needsReauth = false,
        expiresAtEpochMs = currentSnapshot.expiresAtEpochMs,
        tokenReady = currentSnapshot.tokenReady,
        tokenUnavailableSinceEpochMs = currentSnapshot.tokenUnavailableSinceEpochMs,
        updatedAtEpochMs = System.currentTimeMillis(),
    )
}

internal fun loggedOutGptAccountSnapshot(
    currentSnapshot: RemodexGptAccountSnapshot,
    status: RemodexGptAccountStatus = RemodexGptAccountStatus.NOT_LOGGED_IN,
    needsReauth: Boolean = false,
): RemodexGptAccountSnapshot {
    return RemodexGptAccountSnapshot(
        status = status,
        authMethod = null,
        email = if (needsReauth) currentSnapshot.email else null,
        displayName = if (needsReauth) currentSnapshot.displayName else null,
        planType = if (needsReauth) currentSnapshot.planType else null,
        loginInFlight = false,
        needsReauth = needsReauth,
        expiresAtEpochMs = null,
        tokenReady = false,
        tokenUnavailableSinceEpochMs = null,
        updatedAtEpochMs = System.currentTimeMillis(),
    )
}

internal fun decodeContextWindowUsage(resultObject: JsonObject): RemodexContextWindowUsage? {
    val usageObject = resultObject.firstObject("usage") ?: return null
    val tokensUsed = usageObject.firstInt("tokensUsed", "tokens_used", "totalTokens", "total_tokens") ?: return null
    val tokenLimit = usageObject.firstInt("tokenLimit", "token_limit", "maxTokens", "max_tokens") ?: return null
    if (tokenLimit <= 0) {
        return null
    }
    return RemodexContextWindowUsage(
        tokensUsed = tokensUsed,
        tokenLimit = tokenLimit,
    )
}

internal fun decodeRateLimitBuckets(payloadObject: JsonObject): List<RemodexRateLimitBucket> {
    val keyedBuckets = payloadObject.firstObject("rateLimitsByLimitId", "rate_limits_by_limit_id")
    if (keyedBuckets != null) {
        return keyedBuckets.mapNotNull { (limitId, value) ->
            decodeRateLimitBucket(limitId = limitId, value = value.jsonObjectOrNull)
        }
    }

    val nestedBuckets = payloadObject.firstObject("rateLimits", "rate_limits")
    if (nestedBuckets != null) {
        if (containsDirectRateLimitWindows(nestedBuckets)) {
            return decodeDirectRateLimitBuckets(nestedBuckets)
        }
        decodeRateLimitBucket(limitId = null, value = nestedBuckets)?.let { bucket ->
            return listOf(bucket)
        }
    }

    payloadObject.firstObject("result")?.let { nestedResult ->
        return decodeRateLimitBuckets(nestedResult)
    }

    if (containsDirectRateLimitWindows(payloadObject)) {
        return decodeDirectRateLimitBuckets(payloadObject)
    }

    return emptyList()
}

private fun decodeRateLimitBucket(
    limitId: String?,
    value: JsonObject?,
): RemodexRateLimitBucket? {
    val objectValue = value ?: return null
    val resolvedLimitId = normalizeVersion(
        limitId ?: objectValue.firstString("limitId", "limit_id", "id"),
    ) ?: return null
    val primary = decodeRateLimitWindow(objectValue.firstObject("primary", "primary_window"))
    val secondary = decodeRateLimitWindow(objectValue.firstObject("secondary", "secondary_window"))
    if (primary == null && secondary == null) {
        return null
    }
    return RemodexRateLimitBucket(
        limitId = resolvedLimitId,
        limitName = objectValue.firstString("limitName", "limit_name", "name"),
        primary = primary,
        secondary = secondary,
    )
}

private fun decodeDirectRateLimitBuckets(objectValue: JsonObject): List<RemodexRateLimitBucket> {
    return buildList {
        decodeRateLimitWindow(objectValue.firstObject("primary", "primary_window"))?.let { primary ->
            add(
                RemodexRateLimitBucket(
                    limitId = "primary",
                    limitName = objectValue.firstString("limitName", "limit_name", "name"),
                    primary = primary,
                    secondary = null,
                ),
            )
        }
        decodeRateLimitWindow(objectValue.firstObject("secondary", "secondary_window"))?.let { secondary ->
            add(
                RemodexRateLimitBucket(
                    limitId = "secondary",
                    limitName = objectValue.firstString("secondaryName", "secondary_name"),
                    primary = secondary,
                    secondary = null,
                ),
            )
        }
    }
}

private fun decodeRateLimitWindow(objectValue: JsonObject?): RemodexRateLimitWindow? {
    val payload = objectValue ?: return null
    val usedPercent = payload.firstInt("usedPercent", "used_percent") ?: 0
    return RemodexRateLimitWindow(
        usedPercent = usedPercent,
        windowDurationMins = payload.firstInt(
            "windowDurationMins",
            "window_duration_mins",
            "windowMinutes",
            "window_minutes",
        ),
        resetsAtEpochMs = payload.firstValueAsEpochMs("resetsAt", "resets_at", "resetAt", "reset_at"),
    )
}

private fun containsDirectRateLimitWindows(objectValue: JsonObject): Boolean {
    return objectValue["primary"] != null ||
        objectValue["secondary"] != null ||
        objectValue["primary_window"] != null ||
        objectValue["secondary_window"] != null
}

private fun decodeGptAccountStatus(value: String?): RemodexGptAccountStatus {
    return when (value?.trim()?.lowercase()) {
        "authenticated", "logged_in", "loggedin", "connected" -> RemodexGptAccountStatus.AUTHENTICATED
        "loginpending", "login_pending", "pending", "pending_login" -> RemodexGptAccountStatus.LOGIN_PENDING
        "expired", "needs_reauth", "needsreauth", "reauth_required" -> RemodexGptAccountStatus.EXPIRED
        "not_logged_in", "notloggedin", "signed_out", "logged_out", "unauthenticated" ->
            RemodexGptAccountStatus.NOT_LOGGED_IN
        "unavailable", "disconnected" -> RemodexGptAccountStatus.UNAVAILABLE
        else -> RemodexGptAccountStatus.UNKNOWN
    }
}

private fun decodeGptAuthMethod(value: String?): RemodexGptAuthMethod? {
    return when (value?.trim()?.lowercase()) {
        "chatgpt" -> RemodexGptAuthMethod.CHATGPT
        else -> null
    }
}

private fun resolvedTokenUnavailableSinceEpochMs(
    previousSnapshot: RemodexGptAccountSnapshot,
    status: RemodexGptAccountStatus,
    needsReauth: Boolean,
    tokenReady: Boolean,
): Long? {
    if (status != RemodexGptAccountStatus.AUTHENTICATED || needsReauth || tokenReady) {
        return null
    }
    if (
        previousSnapshot.status == RemodexGptAccountStatus.AUTHENTICATED &&
        previousSnapshot.tokenReady == false &&
        previousSnapshot.tokenUnavailableSinceEpochMs != null
    ) {
        return previousSnapshot.tokenUnavailableSinceEpochMs
    }
    return System.currentTimeMillis()
}

private fun resolvedNeedsReauth(
    previousSnapshot: RemodexGptAccountSnapshot,
    baseNeedsReauth: Boolean,
    status: RemodexGptAccountStatus,
    tokenReady: Boolean,
    tokenUnavailableSinceEpochMs: Long?,
): Boolean {
    if (baseNeedsReauth) {
        return true
    }
    if (previousSnapshot.needsReauth && !tokenReady) {
        return true
    }
    if (status != RemodexGptAccountStatus.AUTHENTICATED || tokenReady || tokenUnavailableSinceEpochMs == null) {
        return false
    }
    return System.currentTimeMillis() - tokenUnavailableSinceEpochMs >= RemodexGptAccountSnapshot.VoiceTokenGraceIntervalMs
}

private fun JsonObject.firstValueAsEpochMs(vararg keys: String): Long? {
    keys.forEach { key ->
        val value = this[key] ?: return@forEach
        value.doubleOrEpochMs()?.let { return it }
        value.stringAsEpochMs()?.let { return it }
    }
    return null
}

private fun kotlinx.serialization.json.JsonElement.doubleOrEpochMs(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    return primitive.doubleOrNull?.let { raw ->
        if (raw > 10_000_000_000.0) raw.toLong() else (raw * 1000.0).toLong()
    }
}

private fun kotlinx.serialization.json.JsonElement.stringAsEpochMs(): Long? {
    val primitive = this as? JsonPrimitive ?: return null
    val content = primitive.contentOrNull?.trim().orEmpty()
    if (content.isEmpty()) {
        return null
    }
    return content.toLongOrNull()?.let { raw ->
        if (raw > 10_000_000_000) raw else raw * 1000
    } ?: runCatching { Instant.parse(content).toEpochMilli() }.getOrNull()
}

private fun normalizeVersion(value: String?): String? {
    return value?.trim()?.takeIf(String::isNotEmpty)
}
