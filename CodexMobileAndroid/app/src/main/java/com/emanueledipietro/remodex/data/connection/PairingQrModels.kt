package com.emanueledipietro.remodex.data.connection

import kotlinx.serialization.Serializable

const val remodexPairingQrVersion = 2
const val remodexSecureClockSkewToleranceSeconds = 60L
const val remodexBridgeUpdateCommand = "npm install -g remodex@latest"

@Serializable
data class PairingQrPayload(
    val v: Int,
    val relay: String,
    val sessionId: String,
    val macDeviceId: String,
    val macIdentityPublicKey: String,
    val expiresAt: Long,
)

data class PairingBridgeUpdatePrompt(
    val title: String,
    val message: String,
    val command: String,
)

sealed interface PairingQrValidationResult {
    data class Success(val payload: PairingQrPayload) : PairingQrValidationResult
    data class ScanError(val message: String) : PairingQrValidationResult
    data class BridgeUpdateRequired(val prompt: PairingBridgeUpdatePrompt) : PairingQrValidationResult
}
