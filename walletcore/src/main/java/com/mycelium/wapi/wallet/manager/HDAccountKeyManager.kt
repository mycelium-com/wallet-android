/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycelium.wapi.wallet.manager

import com.google.common.base.Preconditions
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.ByteReader
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

/**
 * Management functions for keys associated with a BIP44 account.
 *
 *
 * Allows you to get a private key, public key, or address for any index in a BIP44 account.
 *
 *
 * Private keys are calculated from the appropriate private chain root on demand.
 *
 *
 * Public keys are calculated from the public chain root on demand once, and then stored in plain text for fast
 * retrieval next time they are requested.
 *
 *
 * Addresses are calculated from the appropriate public key on demand once, and then stored in plain text for fast
 * retrieval next time they are requested.
 */
class HDAccountKeyManager<ADDRESS>(val accountIndex: Int,
                                   val network: CommonNetworkParameters,
                                   private val secureKeyValueStore: SecureKeyValueStore,
                                   val derivationType: BipDerivationType,
                                   val addressFactory: AddressFactory<ADDRESS>)
        where ADDRESS : Address {
    var publicAccountRoot: HdKeyNode = HdKeyNode.fromCustomByteformat(
            secureKeyValueStore.getPlaintextValue(getAccountNodeId(network, accountIndex, derivationType)))

    protected var _publicExternalChainRoot: HdKeyNode? = null
    protected var _publicChangeChainRoot: HdKeyNode? = null

//    protected constructor(secureKeyValueStore: SecureKeyValueStore, derivationType: BipDerivationType) {
//        _secureKeyValueStore = secureKeyValueStore
//        this.derivationType = derivationType
//    }

    init {

        // Make sure we have the private nodes in our encrypted store
        Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getAccountNodeId(network, accountIndex, derivationType)))
        Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, false, derivationType)))
        Preconditions.checkState(secureKeyValueStore.hasCiphertextValue(getChainNodeId(network, accountIndex, true, derivationType)))

        // Load the external and internal public nodes
        try {
            Preconditions.checkState(!publicAccountRoot.isPrivateHdKeyNode)
            _publicExternalChainRoot = HdKeyNode.fromCustomByteformat(
                    secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, false, derivationType)))
            Preconditions.checkState(!_publicExternalChainRoot!!.isPrivateHdKeyNode)
            _publicChangeChainRoot = HdKeyNode.fromCustomByteformat(
                    secureKeyValueStore.getPlaintextValue(getChainNodeId(network, accountIndex, true, derivationType)))
            Preconditions.checkState(!_publicChangeChainRoot!!.isPrivateHdKeyNode)
        } catch (e: ByteReader.InsufficientBytesException) {
            throw RuntimeException(e)
        }
    }

    val accountId: UUID
        get() = publicAccountRoot.uuid

    fun isValidEncryptionKey(userCipher: KeyCipher?): Boolean = secureKeyValueStore.isValidEncryptionKey(userCipher)

    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun getPrivateKey(isChangeChain: Boolean, index: Int, cipher: KeyCipher?): InMemoryPrivateKey {
        // Load the encrypted chain node from the secure storage
        val chainNodeId = getChainNodeId(network, accountIndex, isChangeChain, derivationType)
        val chainNodeBytes = secureKeyValueStore.getDecryptedValue(chainNodeId, cipher)
        val chainNode: HdKeyNode
        chainNode = try {
            HdKeyNode.fromCustomByteformat(chainNodeBytes)
        } catch (e: ByteReader.InsufficientBytesException) {
            throw RuntimeException(e)
        }
        // Create the private key with the appropriate index
        return chainNode.createChildPrivateKey(index)
    }

    fun getPublicKey(isChangeChain: Boolean, index: Int): PublicKey {
        // See if we have it in the store
        val id = getLeafNodeId(network, accountIndex, isChangeChain, index, true, derivationType)
        val publicLeafNodeBytes = secureKeyValueStore.getPlaintextValue(id)
        if (publicLeafNodeBytes != null) {
            // We have it already, no need to calculate it
            return try {
                val publicLeafNode = HdKeyNode.fromCustomByteformat(publicLeafNodeBytes)
                publicLeafNode.publicKey
            } catch (e: ByteReader.InsufficientBytesException) {
                throw RuntimeException(e)
            }
        }

        // Calculate it from the chain node
        val chainNode = if (isChangeChain) _publicChangeChainRoot else _publicExternalChainRoot
        val publicLeafNode = chainNode!!.createChildNode(index)

        // Store it for next time
        secureKeyValueStore.storePlaintextValue(id, publicLeafNode.toCustomByteFormat())
        return publicLeafNode.publicKey
    }

    @Throws(KeyCipher.InvalidKeyCipher::class)
    fun getPrivateAccountRoot(cipher: KeyCipher?, derivationType: BipDerivationType): HdKeyNode {
        return try {
            HdKeyNode.fromCustomByteformat(secureKeyValueStore.getDecryptedValue(getAccountNodeId(network,
                    accountIndex, derivationType), cipher))
        } catch (e: ByteReader.InsufficientBytesException) {
            throw RuntimeException(e)
        }
    }

    fun deleteSubKeyStore() = (secureKeyValueStore as? SecureSubKeyValueStore)?.deleteAllData()
            ?: throw RuntimeException("this is not a SubKeyValueStore")

    fun getAddress(isChangeChain: Boolean, index: Int): ADDRESS? {
        // See if we have it in the store
        val id = getLeafNodeId(network, accountIndex, isChangeChain, index, false, derivationType)
        val addressNodeBytes = secureKeyValueStore.getPlaintextValue(id)
        val purpose: HdKeyPath = when (derivationType) {
            BipDerivationType.BIP44 -> HdKeyPath.BIP44
            BipDerivationType.BIP49 -> HdKeyPath.BIP49
            BipDerivationType.BIP84 -> HdKeyPath.BIP84
            BipDerivationType.BIP86 -> return null
            else -> throw NotImplementedError()
        }
        val path = purpose
                .getHardenedChild(network.getBip44CoinType())
                .getAccount(accountIndex)
                .getChain(!isChangeChain)
                .getAddress(index)
        if (addressNodeBytes != null) {
            // We have it already, no need to calculate it
            return addressFactory.bytesToAddress(addressNodeBytes, path)
        }
        return addressFactory.getAddress(getPublicKey(isChangeChain, index), derivationType.addressType)?.apply {
            this.setBip32Path(path)
            // Store it for next time
            secureKeyValueStore.storePlaintextValue(id, addressFactory.addressToBytes(this))
        }
    }

    companion object {

        @Throws(KeyCipher.InvalidKeyCipher::class)
        fun <ADDRESS> createNew(bip32Root: HdKeyNode, coinType: CryptoCurrency, network: CommonNetworkParameters, accountIndex: Int,
                                secureKeyValueStore: SecureKeyValueStore, cipher: KeyCipher?,
                                derivationType: BipDerivationType, addressFactory: AddressFactory<ADDRESS>): HDAccountKeyManager<ADDRESS>
                where ADDRESS : Address {
            val bip44Root = bip32Root.createChildNode(derivationType.getHardenedPurpose())
            val coinTypeRoot = bip44Root.createHardenedChildNode(network.getBip44CoinType())

            // Create the account root.
            val accountRoot = coinTypeRoot.createChildNode(accountIndex or -0x80000000)
            return createFromAccountRoot(accountRoot, coinType, network, accountIndex, secureKeyValueStore, cipher, derivationType, addressFactory)
        }

        @Throws(KeyCipher.InvalidKeyCipher::class)
        fun <ADDRESS> createFromAccountRoot(accountRoot: HdKeyNode, coinType: CryptoCurrency, network: CommonNetworkParameters,
                                            accountIndex: Int, secureKeyValueStore: SecureKeyValueStore,
                                            cipher: KeyCipher?, derivationType: BipDerivationType, addressFactory: AddressFactory<ADDRESS>): HDAccountKeyManager<ADDRESS>
                where ADDRESS : Address {

            fun storePlainAndEncrypted(id: ByteArray, plainBytes: ByteArray, secretBytes: ByteArray) {
                secureKeyValueStore.encryptAndStoreValue(id, secretBytes, cipher)
                secureKeyValueStore.storePlaintextValue(id, plainBytes)
            }

            // Store the account root (xPub and xPriv) key
            storePlainAndEncrypted(getAccountNodeId(network, accountIndex, derivationType),
                    accountRoot.publicNode.toCustomByteFormat(), accountRoot.toCustomByteFormat())

            // Create the external chain root. Store the private node encrypted and the public node in plain text
            val externalChainRoot = accountRoot.createChildNode(0)
            storePlainAndEncrypted(getChainNodeId(network, accountIndex, false, derivationType),
                    externalChainRoot.publicNode.toCustomByteFormat(), externalChainRoot.toCustomByteFormat())

            // Create the change chain root. Store the private node encrypted and the public node in plain text
            val changeChainRoot = accountRoot.createChildNode(1)
            storePlainAndEncrypted(getChainNodeId(network, accountIndex, true, derivationType),
                    changeChainRoot.publicNode.toCustomByteFormat(), changeChainRoot.toCustomByteFormat())
            return HDAccountKeyManager(accountIndex, network, secureKeyValueStore, derivationType, addressFactory)
        }

        protected fun getAccountNodeId(network: CommonNetworkParameters, accountIndex: Int, derivationType: BipDerivationType): ByteArray {
            // Create a compact unique account ID
            val id = ByteArray(1 + 1 + 4)
            id[0] = derivationType.purpose
            id[1] = (network.getBip44CoinType()).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            return id
        }

        protected fun getChainNodeId(network: CommonNetworkParameters, accountIndex: Int, isChangeChain: Boolean,
                                     derivationType: BipDerivationType): ByteArray {
            // Create a compact unique chain node ID
            val id = ByteArray(1 + 1 + 4 + 1)
            id[0] = derivationType.purpose
            id[1] = (network.getBip44CoinType()).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            id[6] = (if (isChangeChain) 1 else 0).toByte() // external chain or change chain
            return id
        }

        private fun getLeafNodeId(network: CommonNetworkParameters, accountIndex: Int, isChangeChain: Boolean, index: Int,
                                  isHdNode: Boolean, derivationType: BipDerivationType): ByteArray {
            // Create a compact unique address or HD node ID
            val id = ByteArray(1 + 1 + 4 + 1 + 4 + 1)
            id[0] = derivationType.purpose
            id[1] = (network.getBip44CoinType()).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            id[6] = (if (isChangeChain) 1 else 0).toByte() // external chain or change chain
            BitUtils.uint32ToByteArrayLE(index.toLong(), id, 7) // address index
            id[11] = (if (isHdNode) 1 else 0).toByte() // is HD node or address
            return id
        }
    }
}
