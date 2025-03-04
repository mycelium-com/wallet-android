package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.bitcoinj.Base58
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.crypto.digest.RIPEMD160Digest
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.BitUtils
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.btc.bip44.HDAccountKeyManager
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import java.util.*


class FioKeyManager(
    private val masterSeedManager: MasterSeedManager,
    private val secureKeyValueStore: SecureKeyValueStore
) {

    val hardenedChildNode = 235
    val hdKeyPath = HdKeyPath.valueOf("m/44'/${hardenedChildNode}'")

    private fun getFioNode(): HdKeyNode {
        val masterSeed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())
        // following [SLIP44](https://github.com/satoshilabs/slips/blob/master/slip-0044.md)
        // m/44'/235'
        return HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP44)
                .createChildNode(hdKeyPath)
    }

    fun getLegacyFioNode(): HdKeyNode {
        val masterSeed = masterSeedManager.getMasterSeed(AesKeyCipher.defaultKeyCipher())
        return HdKeyNode.fromSeed(masterSeed.bip32Seed, BipDerivationType.BIP44)
                .createHardenedChildNode(hardenedChildNode).createHardenedChildNode(0)
    }


    fun getFioPrivateKey(accountIndex: Int): InMemoryPrivateKey {
        val privateKeyLeaf = privateKeyLeaf(accountIndex)
        return secureKeyValueStore.getPlaintextValue(privateKeyLeaf)?.let { priKeyBytes ->
                InMemoryPrivateKey(priKeyBytes, false)
        } ?: getFioNode().createHardenedChildNode(accountIndex)
            .createChildNode(0).createChildNode(0).privateKey.apply {
                secureKeyValueStore.storePlaintextValue(privateKeyLeaf, privateKeyBytes)
            }
    }

    fun getFioPublicKey(accountIndex: Int): PublicKey {
        val publicKeyLeaf = publicKeyLeaf(accountIndex)
        return secureKeyValueStore.getPlaintextValue(publicKeyLeaf)?.let {
            PublicKey(it)
        } ?: getFioPrivateKey(accountIndex).publicKey.apply {
            secureKeyValueStore.storePlaintextValue(publicKeyLeaf, publicKeyBytes)
        }
    }

    private fun publicKeyLeaf(accountIndex: Int) =
        HDAccountKeyManager.getLeafNodeId(
            hardenedChildNode, accountIndex, false, 0, false,
            BipDerivationType.BIP44
        )

    private fun privateKeyLeaf(accountIndex: Int) =
        HDAccountKeyManager.getLeafNodeId(
            hardenedChildNode, accountIndex, false, 0, true,
            BipDerivationType.BIP44
        )

    fun formatPubKey(publicKey: PublicKey): String {
        val out = ByteArray(20)
        val ripeMD160 = RIPEMD160Digest()
        ripeMD160.update(publicKey.publicKeyBytes, 0, publicKey.publicKeyBytes.size)
        ripeMD160.doFinal(out, 0)
        val slicedHash = out.slice(IntRange(0, 3))
        return "FIO" + Base58.encode(publicKey.publicKeyBytes + slicedHash)
    }

    fun getUUID(accountIndex: Int): UUID {
        val pubkeyBytes = getFioPublicKey(accountIndex).publicKeyBytes
        return UUID(BitUtils.uint64ToLong(pubkeyBytes, 8), BitUtils.uint64ToLong(
                pubkeyBytes, 16))
    }
}