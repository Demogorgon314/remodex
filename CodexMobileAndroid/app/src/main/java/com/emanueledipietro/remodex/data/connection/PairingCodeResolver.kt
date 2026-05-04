package com.emanueledipietro.remodex.data.connection

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

interface PairingCodeResolver {
    suspend fun resolve(
        relayUrl: String,
        code: String,
    ): PairingQrPayload
}

object UnavailablePairingCodeResolver : PairingCodeResolver {
    override suspend fun resolve(
        relayUrl: String,
        code: String,
    ): PairingQrPayload {
        throw SecureTransportException("Pairing code resolution is not configured.")
    }
}

class OkHttpPairingCodeResolver(
    private val client: OkHttpClient,
) : PairingCodeResolver {
    private val json = Json {
        ignoreUnknownKeys = true
    }

    override suspend fun resolve(
        relayUrl: String,
        code: String,
    ): PairingQrPayload = withContext(Dispatchers.IO) {
        val normalizedCode = normalizeShortPairingCode(code)
            ?: throw SecureTransportException("Enter a valid pairing code.")
        val resolveUrl = pairingCodeResolveUrl(relayUrl)
            ?: throw SecureTransportException(
                "This Android device does not know which relay to ask for that pairing code yet. Scan the QR code instead.",
            )

        val httpRequest = Request.Builder()
            .url(resolveUrl)
            .post(
                json.encodeToString(PairingCodeResolveRequest.serializer(), PairingCodeResolveRequest(normalizedCode))
                    .toRequestBody("application/json".toMediaType()),
            )
            .build()

        client.newCall(httpRequest).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.isSuccessful) {
                val resolved = json.decodeFromString<PairingCodeResolveResponse>(responseBody)
                if (resolved.ok) {
                    return@withContext PairingQrPayload(
                        v = resolved.v,
                        relay = relayUrl,
                        sessionId = resolved.sessionId,
                        macDeviceId = resolved.macDeviceId,
                        macIdentityPublicKey = resolved.macIdentityPublicKey,
                        expiresAt = resolved.expiresAt,
                    )
                }
            }

            val relayError = runCatching {
                json.decodeFromString(RelayErrorResponse.serializer(), responseBody)
            }.getOrNull()
            throw SecureTransportException(
                message = pairingCodeErrorMessage(response.code, relayError),
                code = relayError?.code,
            )
        }
    }
}

fun pairingCodeResolveUrl(relayUrl: String): String? {
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
        normalizedHttpBase.removeSuffix("/relay") + "/v1/pairing/code/resolve"
    } else {
        "$normalizedHttpBase/v1/pairing/code/resolve"
    }
}

private fun pairingCodeErrorMessage(
    statusCode: Int,
    relayError: RelayErrorResponse?,
): String {
    return when (relayError?.code) {
        "pairing_code_expired" -> "This pairing code has expired. Generate a new one from the computer bridge."
        "pairing_code_unavailable" -> "That pairing code is not available right now. Make sure your computer bridge is running and try again."
        else -> if (statusCode == 404) {
            "This relay does not support pairing codes yet. Scan the QR code instead."
        } else {
            relayError?.error ?: "The relay could not resolve that pairing code."
        }
    }
}
