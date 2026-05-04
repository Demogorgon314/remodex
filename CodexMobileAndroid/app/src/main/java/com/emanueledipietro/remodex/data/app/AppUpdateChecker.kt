package com.emanueledipietro.remodex.data.app

import com.emanueledipietro.remodex.model.RemodexAppUpdateState
import com.emanueledipietro.remodex.model.RemodexAppUpdateStatus
import com.emanueledipietro.remodex.model.remodexCompareReleaseVersions
import com.emanueledipietro.remodex.model.remodexNormalizeReleaseVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

interface AppUpdateChecker {
    val currentVersion: String?

    suspend fun checkForUpdate(): RemodexAppUpdateStatus
}

class GitHubReleaseAppUpdateChecker(
    private val okHttpClient: OkHttpClient,
    override val currentVersion: String?,
    private val ownerRepo: String = "Demogorgon314/remodex-android",
) : AppUpdateChecker {
    override suspend fun checkForUpdate(): RemodexAppUpdateStatus = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url("https://api.github.com/repos/${ownerRepo}/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Remodex-Android")
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IllegalStateException("GitHub Releases returned HTTP ${response.code}.")
            }
            val body = response.body?.string().orEmpty()
            decodeGitHubLatestReleaseUpdateStatus(
                currentVersion = currentVersion,
                responseBody = body,
            )
        }
    }
}

internal fun decodeGitHubLatestReleaseUpdateStatus(
    currentVersion: String?,
    responseBody: String,
): RemodexAppUpdateStatus {
    val releaseObject = Json.parseToJsonElement(responseBody).jsonObject
    val tagName = releaseObject.stringValue("tag_name")
    val latestVersion = remodexNormalizeReleaseVersion(tagName)
    val releaseUrl = releaseObject.stringValue("html_url")
    if (latestVersion == null || releaseUrl == null) {
        throw IllegalStateException("GitHub latest release response is missing version details.")
    }
    val apkAssetName = releaseObject["assets"]
        ?.jsonArrayOrNull()
        ?.firstNotNullOfOrNull { asset ->
            asset.jsonObjectOrNull()
                ?.stringValue("name")
                ?.takeIf { name -> name.endsWith(".apk", ignoreCase = true) }
        }

    val state = if (
        currentVersion != null &&
        remodexCompareReleaseVersions(currentVersion, latestVersion) < 0
    ) {
        RemodexAppUpdateState.UPDATE_AVAILABLE
    } else {
        RemodexAppUpdateState.UP_TO_DATE
    }

    return RemodexAppUpdateStatus(
        currentVersion = currentVersion,
        latestVersion = latestVersion,
        releaseUrl = releaseUrl,
        apkAssetName = apkAssetName,
        state = state,
    )
}

private fun JsonObject.stringValue(key: String): String? {
    return this[key]?.jsonPrimitive?.contentOrNull?.trim()?.takeIf(String::isNotEmpty)
}

private fun kotlinx.serialization.json.JsonElement.jsonObjectOrNull(): JsonObject? {
    return runCatching { jsonObject }.getOrNull()
}

private fun kotlinx.serialization.json.JsonElement.jsonArrayOrNull(): JsonArray? {
    return runCatching { jsonArray }.getOrNull()
}
