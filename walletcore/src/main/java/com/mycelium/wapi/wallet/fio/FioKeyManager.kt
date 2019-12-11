package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager

class FioKeyManager(private val masterSeedManager: MasterSeedManager) {
    private fun getFioNode(): HdKeyNode {
        val masterSeed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())
        // following [SLIP44](https://github.com/satoshilabs/slips/blob/master/slip-0044.md)
        // m/44'/235'
        return HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP44)
                .createChildNode(HdKeyPath.valueOf("m/44'/235'/0'/0/0"))
    }

    fun getFioPublicKey() = InMemoryPrivateKey(getFioNode().privateKey.privateKeyBytes).publicKey

    fun getFioPublicKey(accountIndex: Int) = getFioNode().createHardenedChildNode(accountIndex).privateKey.publicKey

    fun formatPubKey(publicKey: PublicKey) = "FIO" + Base58.encode(publicKey.publicKeyBytes)
}