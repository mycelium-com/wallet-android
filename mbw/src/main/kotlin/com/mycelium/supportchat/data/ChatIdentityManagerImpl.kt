package com.mycelium.supportchat.data

import com.mrd.bitlib.crypto.PrivateKey
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.supportchat.data.model.ChatIdentityManager
import kotlin.coroutines.cancellation.CancellationException

/**
 * Manages cryptographic identity for support chat.
 * Uses IdentityAccountKeyManager to derive deterministic keys for the support domain.
 */
//@Inject
class ChatIdentityManagerImpl(
    private val identityKeyManagerProvider: suspend () -> IdentityAccountKeyManager
) : ChatIdentityManager {
    companion object {
        private const val SUPPORT_WEBSITE = "support.mycelium.com"
    }

    private var publicKey: ByteArray? = null
    private var privateKey: PrivateKey? = null
    private var isInitialized = false

    /**
     * Initialize identity with the default cipher.
     * Must be called before using other methods.
     */
    @Throws(KeyCipher.InvalidKeyCipher::class, CancellationException::class)
    override suspend fun initialize() {
        if (isInitialized) return

        val identityKeyManager = identityKeyManagerProvider()
        val cipher = AesKeyCipher.defaultKeyCipher()
        publicKey = identityKeyManager.getPublicKeyForWebsite(SUPPORT_WEBSITE, cipher)
        privateKey = identityKeyManager.getPrivateKeyForWebsite(SUPPORT_WEBSITE, cipher)
        isInitialized = true
    }

    /**
     * Check if the identity manager is initialized.
     */
    override fun isInitialized(): Boolean = isInitialized

    /**
     * Get the chat ID (hex-encoded compressed public key).
     * This is a 33-byte compressed public key encoded as 66 hex characters.
     */
    override fun getChatId(): String {
        val key = publicKey ?: throw IllegalStateException("ChatIdentityManager not initialized. Call initialize() first.")
        return key.toHexString()
    }

    /**
     * Sign data for verification.
     * Returns the signature as a byte array.
     */
    fun sign(data: ByteArray): ByteArray {
        val key = privateKey ?: throw IllegalStateException("ChatIdentityManager not initialized. Call initialize() first.")
        return key.signMessage(data.decodeToString()).derEncodedSignature
            ?: throw IllegalStateException("Failed to sign data")
    }

    /**
     * Create a signature for a message with timestamp.
     * Format: SHA256(text|timestamp)
     * Returns hex-encoded signature.
     */
    override fun signMessage(text: String, timestamp: Long): String {
        val dataToSign = "$text|$timestamp".encodeToByteArray()
        return sign(dataToSign).toHexString()
    }

    /**
     * Clear cached keys (for security when locking the wallet).
     */
    fun clear() {
        publicKey = null
        privateKey = null
        isInitialized = false
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun ByteArray.toHexString(): String = toHexString(HexFormat.Default)
}