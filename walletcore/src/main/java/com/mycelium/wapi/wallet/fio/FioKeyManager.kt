package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager

class FioKeyManager(private val secureStore: SecureKeyValueStore) {
    private val masterSeedManager = MasterSeedManager(secureStore)

    fun getFioKeyPair(accountIndex: Int): PublicKey {
        val masterSeed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())
        return HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP44)
                .createHardenedChildNode(235)
                .createHardenedChildNode(accountIndex).privateKey.publicKey
    }

    fun formatPubKey(publicKey: PublicKey) = "FIO" + Base58.encode(publicKey.publicKeyBytes)

}