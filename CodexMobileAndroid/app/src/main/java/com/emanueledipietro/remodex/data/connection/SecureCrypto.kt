package com.emanueledipietro.remodex.data.connection

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import org.bouncycastle.crypto.agreement.X25519Agreement
import org.bouncycastle.crypto.digests.SHA256Digest
import org.bouncycastle.crypto.generators.HKDFBytesGenerator
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters
import org.bouncycastle.crypto.params.Ed25519PublicKeyParameters
import org.bouncycastle.crypto.params.HKDFParameters
import org.bouncycastle.crypto.params.X25519PrivateKeyParameters
import org.bouncycastle.crypto.params.X25519PublicKeyParameters
import org.bouncycastle.crypto.signers.Ed25519Signer

object SecureCrypto {
    private val secureRandom = SecureRandom()
    private val base64Encoder = Base64.getEncoder()
    private val base64Decoder = Base64.getDecoder()

    fun phoneIdentityStateFromStore(store: SecureStore): PhoneIdentityState {
        val existing = store.readSerializable(
            SecureStoreKeys.PHONE_IDENTITY_STATE,
            PhoneIdentityState.serializer(),
        )
        if (existing != null) {
            return existing
        }

        val privateKey = Ed25519PrivateKeyParameters(secureRandom)
        val next = PhoneIdentityState(
            phoneDeviceId = UUID.randomUUID().toString(),
            phoneIdentityPrivateKey = encodeBase64(privateKey.encoded),
            phoneIdentityPublicKey = encodeBase64(privateKey.generatePublicKey().encoded),
        )
        store.writeSerializable(
            SecureStoreKeys.PHONE_IDENTITY_STATE,
            PhoneIdentityState.serializer(),
            next,
        )
        return next
    }

    fun trustedMacRegistryFromStore(store: SecureStore): TrustedMacRegistry {
        return store.readSerializable(
            SecureStoreKeys.TRUSTED_MAC_REGISTRY,
            TrustedMacRegistry.serializer(),
        ) ?: TrustedMacRegistry()
    }

    fun relayPairingStateFromStore(store: SecureStore): RelayPairingState? {
        val sessionId = store.readString(SecureStoreKeys.RELAY_SESSION_ID) ?: return null
        val relayUrl = store.readString(SecureStoreKeys.RELAY_URL) ?: return null
        val macDeviceId = store.readString(SecureStoreKeys.RELAY_MAC_DEVICE_ID) ?: return null
        val macIdentityPublicKey = store.readString(SecureStoreKeys.RELAY_MAC_IDENTITY_PUBLIC_KEY) ?: return null
        val protocolVersion = store.readString(SecureStoreKeys.RELAY_PROTOCOL_VERSION)?.toIntOrNull()
            ?: remodexSecureProtocolVersion
        val lastAppliedBridgeOutboundSeq =
            store.readString(SecureStoreKeys.RELAY_LAST_APPLIED_BRIDGE_OUTBOUND_SEQ)?.toIntOrNull() ?: 0
        val shouldForceQrBootstrap =
            store.readString(SecureStoreKeys.SHOULD_FORCE_QR_BOOTSTRAP)?.toBooleanStrictOrNull() ?: false

        return RelayPairingState(
            sessionId = sessionId,
            relayUrl = relayUrl,
            macDeviceId = macDeviceId,
            macIdentityPublicKey = macIdentityPublicKey,
            protocolVersion = protocolVersion,
            lastAppliedBridgeOutboundSeq = lastAppliedBridgeOutboundSeq,
            shouldForceQrBootstrap = shouldForceQrBootstrap,
        )
    }

    fun rememberRelayPairing(
        store: SecureStore,
        payload: PairingQrPayload,
    ) {
        store.writeString(SecureStoreKeys.RELAY_SESSION_ID, payload.sessionId)
        store.writeString(SecureStoreKeys.RELAY_URL, payload.relay)
        store.writeString(SecureStoreKeys.RELAY_MAC_DEVICE_ID, payload.macDeviceId)
        store.writeString(SecureStoreKeys.RELAY_MAC_IDENTITY_PUBLIC_KEY, payload.macIdentityPublicKey)
        store.writeString(SecureStoreKeys.RELAY_PROTOCOL_VERSION, remodexSecureProtocolVersion.toString())
        store.writeString(SecureStoreKeys.RELAY_LAST_APPLIED_BRIDGE_OUTBOUND_SEQ, "0")
        store.writeString(SecureStoreKeys.SHOULD_FORCE_QR_BOOTSTRAP, "true")
    }

    fun rememberResolvedTrustedSession(
        store: SecureStore,
        response: TrustedSessionResolveResponse,
        relayUrl: String,
        resetReplayCursor: Boolean,
    ) {
        if (resetReplayCursor) {
            store.writeString(SecureStoreKeys.RELAY_LAST_APPLIED_BRIDGE_OUTBOUND_SEQ, "0")
        }
        store.writeString(SecureStoreKeys.RELAY_SESSION_ID, response.sessionId)
        store.writeString(SecureStoreKeys.RELAY_URL, relayUrl)
        store.writeString(SecureStoreKeys.RELAY_MAC_DEVICE_ID, response.macDeviceId)
        store.writeString(SecureStoreKeys.RELAY_MAC_IDENTITY_PUBLIC_KEY, response.macIdentityPublicKey)
        store.writeString(SecureStoreKeys.RELAY_PROTOCOL_VERSION, remodexSecureProtocolVersion.toString())
        store.writeString(SecureStoreKeys.SHOULD_FORCE_QR_BOOTSTRAP, "false")
    }

    fun writeTrustedMacRegistry(
        store: SecureStore,
        registry: TrustedMacRegistry,
        lastTrustedMacDeviceId: String?,
    ) {
        store.writeSerializable(
            SecureStoreKeys.TRUSTED_MAC_REGISTRY,
            TrustedMacRegistry.serializer(),
            registry,
        )
        store.writeString(SecureStoreKeys.LAST_TRUSTED_MAC_DEVICE_ID, lastTrustedMacDeviceId)
    }

    fun secureTranscriptBytes(
        sessionId: String,
        protocolVersion: Int,
        handshakeMode: SecureHandshakeMode,
        keyEpoch: Int,
        macDeviceId: String,
        phoneDeviceId: String,
        macIdentityPublicKey: String,
        phoneIdentityPublicKey: String,
        macEphemeralPublicKey: String,
        phoneEphemeralPublicKey: String,
        clientNonce: ByteArray,
        serverNonce: ByteArray,
        expiresAtForTranscript: Long,
    ): ByteArray {
        return buildList<ByteArray> {
            add(lengthPrefixedUtf8(remodexSecureHandshakeTag))
            add(lengthPrefixedUtf8(sessionId))
            add(lengthPrefixedUtf8(protocolVersion.toString()))
            add(lengthPrefixedUtf8(handshakeMode.wireValue))
            add(lengthPrefixedUtf8(keyEpoch.toString()))
            add(lengthPrefixedUtf8(macDeviceId))
            add(lengthPrefixedUtf8(phoneDeviceId))
            add(lengthPrefixedData(decodeBase64(macIdentityPublicKey)))
            add(lengthPrefixedData(decodeBase64(phoneIdentityPublicKey)))
            add(lengthPrefixedData(decodeBase64(macEphemeralPublicKey)))
            add(lengthPrefixedData(decodeBase64(phoneEphemeralPublicKey)))
            add(lengthPrefixedData(clientNonce))
            add(lengthPrefixedData(serverNonce))
            add(lengthPrefixedUtf8(expiresAtForTranscript.toString()))
        }.fold(ByteArray(0)) { acc, part -> acc + part }
    }

    fun clientAuthTranscript(transcriptBytes: ByteArray): ByteArray {
        return transcriptBytes + lengthPrefixedUtf8(remodexSecureHandshakeLabel)
    }

    fun trustedSessionResolveTranscriptBytes(
        macDeviceId: String,
        phoneDeviceId: String,
        phoneIdentityPublicKey: String,
        nonce: String,
        timestamp: Long,
    ): ByteArray {
        return buildList<ByteArray> {
            add(lengthPrefixedUtf8(remodexTrustedSessionResolveTag))
            add(lengthPrefixedUtf8(macDeviceId))
            add(lengthPrefixedUtf8(phoneDeviceId))
            add(lengthPrefixedData(decodeBase64(phoneIdentityPublicKey)))
            add(lengthPrefixedUtf8(nonce))
            add(lengthPrefixedUtf8(timestamp.toString()))
        }.fold(ByteArray(0)) { acc, part -> acc + part }
    }

    fun secureNonce(
        sender: String,
        counter: Int,
    ): ByteArray {
        val nonce = ByteArray(12)
        nonce[0] = if (sender == "mac") 1 else 2
        var remaining = counter.toLong()
        for (index in 11 downTo 1) {
            nonce[index] = (remaining and 0xff).toByte()
            remaining = remaining shr 8
        }
        return nonce
    }

    fun randomNonce(): ByteArray = ByteArray(32).also(secureRandom::nextBytes)

    fun fingerprint(publicKeyBase64: String): String {
        return MessageDigest.getInstance("SHA-256")
            .digest(decodeBase64(publicKeyBase64))
            .joinToString(separator = "") { "%02x".format(it) }
            .take(12)
            .uppercase()
    }

    fun generateX25519KeyPair(): Pair<ByteArray, ByteArray> {
        val privateKey = X25519PrivateKeyParameters(secureRandom)
        return privateKey.encoded to privateKey.generatePublicKey().encoded
    }

    fun signEd25519(
        privateKeyBase64: String,
        payload: ByteArray,
    ): String {
        val signer = Ed25519Signer()
        signer.init(true, Ed25519PrivateKeyParameters(decodeBase64(privateKeyBase64), 0))
        signer.update(payload, 0, payload.size)
        return encodeBase64(signer.generateSignature())
    }

    fun verifyEd25519(
        publicKeyBase64: String,
        payload: ByteArray,
        signatureBase64: String,
    ): Boolean {
        return runCatching {
            val signer = Ed25519Signer()
            signer.init(false, Ed25519PublicKeyParameters(decodeBase64(publicKeyBase64), 0))
            signer.update(payload, 0, payload.size)
            signer.verifySignature(decodeBase64(signatureBase64))
        }.getOrDefault(false)
    }

    fun sharedSecret(
        privateKeyBase64: String,
        publicKeyBase64: String,
    ): ByteArray {
        val agreement = X25519Agreement()
        agreement.init(X25519PrivateKeyParameters(decodeBase64(privateKeyBase64), 0))
        val output = ByteArray(agreement.agreementSize)
        agreement.calculateAgreement(X25519PublicKeyParameters(decodeBase64(publicKeyBase64), 0), output, 0)
        return output
    }

    fun hkdfSha256(
        secret: ByteArray,
        salt: ByteArray,
        infoLabel: String,
        outputByteCount: Int = 32,
    ): ByteArray {
        val generator = HKDFBytesGenerator(SHA256Digest())
        generator.init(HKDFParameters(secret, salt, infoLabel.toByteArray(Charsets.UTF_8)))
        return ByteArray(outputByteCount).also { generator.generateBytes(it, 0, outputByteCount) }
    }

    fun encryptAesGcm(
        plaintext: ByteArray,
        key: ByteArray,
        nonce: ByteArray,
    ): Pair<String, String> {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(
            Cipher.ENCRYPT_MODE,
            SecretKeySpec(key, "AES"),
            GCMParameterSpec(128, nonce),
        )
        val sealed = cipher.doFinal(plaintext)
        val ciphertext = sealed.copyOf(sealed.size - 16)
        val tag = sealed.copyOfRange(sealed.size - 16, sealed.size)
        return encodeBase64(ciphertext) to encodeBase64(tag)
    }

    fun decryptAesGcm(
        ciphertextBase64: String,
        tagBase64: String,
        key: ByteArray,
        nonce: ByteArray,
    ): ByteArray? {
        return runCatching {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(
                Cipher.DECRYPT_MODE,
                SecretKeySpec(key, "AES"),
                GCMParameterSpec(128, nonce),
            )
            cipher.doFinal(decodeBase64(ciphertextBase64) + decodeBase64(tagBase64))
        }.getOrNull()
    }

    fun encodeBase64(bytes: ByteArray): String = base64Encoder.encodeToString(bytes)

    fun decodeBase64(value: String): ByteArray = runCatching {
        base64Decoder.decode(value)
    }.getOrDefault(ByteArray(0))

    private fun lengthPrefixedUtf8(value: String): ByteArray =
        lengthPrefixedData(value.toByteArray(Charsets.UTF_8))

    private fun lengthPrefixedData(value: ByteArray): ByteArray {
        val length = value.size
        return byteArrayOf(
            ((length shr 24) and 0xff).toByte(),
            ((length shr 16) and 0xff).toByte(),
            ((length shr 8) and 0xff).toByte(),
            (length and 0xff).toByte(),
        ) + value
    }
}
