package com.emanueledipietro.remodex.data.connection

import com.emanueledipietro.remodex.model.RemodexBridgeUpdatePrompt
import kotlinx.serialization.Serializable

const val remodexPairingQrVersion = 2
const val remodexSecureClockSkewToleranceSeconds = 60L

@Serializable
data class PairingQrPayload(
    val v: Int,
    val relay: String,
    val sessionId: String,
    val macDeviceId: String,
    val macIdentityPublicKey: String,
    val expiresAt: Long,
)

sealed interface PairingQrValidationResult {
    data class Success(val payload: PairingQrPayload) : PairingQrValidationResult
    data class ShortCode(val code: String) : PairingQrValidationResult
    data class ScanError(val message: String) : PairingQrValidationResult
    data class BridgeUpdateRequired(val prompt: RemodexBridgeUpdatePrompt) : PairingQrValidationResult
}

@Serializable
data class PairingCodeResolveRequest(
    val code: String,
)

@Serializable
data class PairingCodeResolveResponse(
    val ok: Boolean,
    val v: Int,
    val sessionId: String,
    val macDeviceId: String,
    val macIdentityPublicKey: String,
    val expiresAt: Long,
)
