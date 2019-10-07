package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.PrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager

class FioKeyManager(private val masterSeedManager: MasterSeedManager) {
    private fun getFioNode(): HdKeyNode {
        val masterSeed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())
        // following [SLIP44](https://github.com/satoshilabs/slips/blob/master/slip-0044.md)
        // m/44'/235'
        return HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP44)
                .createHardenedChildNode(235)
    }

    fun getFioPublicKey(accountIndex: Int) = getFioNode().createHardenedChildNode(accountIndex).privateKey.publicKey

    fun formatPubKey(publicKey: PublicKey) = "FIO" + Base58.encode(publicKey.publicKeyBytes)
}