package com.mycelium.wapi.wallet.btcmasterseed

import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.IdentityAccountKeyManager
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore


class MasterSeedManager(private val secureKeyValueStore: SecureKeyValueStore) {

    var listener: Listener? = null

    private var identityAccountKeyManager: IdentityAccountKeyManager? = null

    /**
     * Configure the BIP32 master seed of this wallet manager
     *
     * @param masterSeed the master seed to use.
     * @param cipher     the cipher used to encrypt the master seed. Must be the same
     * cipher as the one used by the secure storage instance
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    @Throws(InvalidKeyCipher::class)
    fun configureBip32MasterSeed(masterSeed: Bip39.MasterSeed, cipher: KeyCipher) {
        if (hasBip32MasterSeed()) {
            throw RuntimeException("HD key store already loaded")
        }
        secureKeyValueStore.encryptAndStoreValue(MASTER_SEED_ID, masterSeed.toBytes(false), cipher)
        listener?.masterSeedConfigured()
    }

    /**
     * Get the master seed in plain text
     *
     * @param cipher the cipher used to decrypt the master seed
     * @return the master seed in plain text
     * @throws InvalidKeyCipher if the cipher is invalid
     */
    @Throws(InvalidKeyCipher::class)
    fun getMasterSeed(cipher: KeyCipher): Bip39.MasterSeed {
        val binaryMasterSeed = secureKeyValueStore.getDecryptedValue(MASTER_SEED_ID, cipher)
        val masterSeed = Bip39.MasterSeed.fromBytes(binaryMasterSeed, false)
        if (!masterSeed.isPresent) {
            throw RuntimeException()
        }
        return masterSeed.get()
    }

    /**
     * Determine whether the wallet manager has a master seed configured
     *
     * @return true if a master seed has been configured for this wallet manager
     */
    fun hasBip32MasterSeed(): Boolean {
        return secureKeyValueStore.hasCiphertextValue(MASTER_SEED_ID)
    }

    @Throws(InvalidKeyCipher::class)
    fun getIdentityAccountKeyManager(cipher: KeyCipher): IdentityAccountKeyManager {
        if (identityAccountKeyManager != null) {
            return identityAccountKeyManager!!
        }
        if (!hasBip32MasterSeed()) {
            throw RuntimeException("accessed identity account with no master seed configured")
        }
        val rootNode = HdKeyNode.fromSeed(getMasterSeed(cipher).bip32Seed, null)
        identityAccountKeyManager = IdentityAccountKeyManager.createNew(rootNode, secureKeyValueStore, cipher)
        return identityAccountKeyManager!!
    }

    companion object {
        val MASTER_SEED_ID = HexUtils.toBytes("D64CA2B680D8C8909A367F28EB47F990")
    }
}