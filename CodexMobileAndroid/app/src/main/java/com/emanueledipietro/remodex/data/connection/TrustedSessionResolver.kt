package com.emanueledipietro.remodex.data.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface TrustedSessionResolver {
    suspend fun resolve(
        relayUrl: String,
        request: TrustedSessionResolveRequest,
    ): TrustedSessionResolveResponse
}

class OkHttpTrustedSessionResolver(
    private val client: OkHttpClient,
) : TrustedSessionResolver {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun resolve(
        relayUrl: String,
        request: TrustedSessionResolveRequest,
    ): TrustedSessionResolveResponse = withContext(Dispatchers.IO) {
        val resolveUrl = trustedSessionResolveUrl(relayUrl)
            ?: throw TrustedSessionResolveException(
                code = "invalid_relay_url",
                message = "The trusted Mac relay URL is invalid.",
            )

        val httpRequest = Request.Builder()
            .url(resolveUrl)
            .post(
                json.encodeToString(TrustedSessionResolveRequest.serializer(), request)
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()

            if (response.isSuccessful) {
                return@withContext json.decodeFromString(
                    TrustedSessionResolveResponse.serializer(),
                    responseBody,
                )
            }

            val relayError = runCatching {
                json.decodeFromString(RelayErrorResponse.serializer(), responseBody)
            }.getOrNull()

            throw TrustedSessionResolveException(
                code = relayError?.code ?: "network_error",
                message = relayError?.error
                    ?: relayError?.code
                    ?: "The trusted Mac relay could not resolve the current bridge session.",
            )
        }
    }
}

fun trustedSessionResolveUrl(relayUrl: String): String? {
    val normalizedRelayUrl = relayUrl.trim()
    if (normalizedRelayUrl.isEmpty()) {
        return null
    }

    val normalizedHttpBase = when {
        normalizedRelayUrl.startsWith("wss://") -> "https://${normalizedRelayUrl.removePrefix("wss://")}"
        normalizedRelayUrl.startsWith("ws://") -> "http://${normalizedRelayUrl.removePrefix("ws://")}"
        normalizedRelayUrl.startsWith("https://") || normalizedRelayUrl.startsWith("http://") -> normalizedRelayUrl
        else -> return null
    }.trimEnd('/')

    return if (normalizedHttpBase.endsWith("/relay")) {
        normalizedHttpBase.removeSuffix("/relay") + "/v1/trusted/session/resolve"
    } else {
        normalizedHttpBase + "/v1/trusted/session/resolve"
    }
}
