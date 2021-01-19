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
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BtcvSegwitAddress
import com.mrd.bitlib.model.Script
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.ByteReader
import com.mrd.bitlib.util.ByteWriter
import com.mrd.bitlib.util.HashUtils
import com.mycelium.wapi.wallet.CommonNetworkParameters
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SecureKeyValueStore
import com.mycelium.wapi.wallet.SecureSubKeyValueStore
import com.mycelium.wapi.wallet.btcvault.BtcvAddress
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest
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
class HDAccountKeyManager(val accountIndex: Int,
                          val network: CommonNetworkParameters,
                          val secureKeyValueStore: SecureKeyValueStore,
                          val derivationType: BipDerivationType) {
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

    fun deleteSubKeyStore() {
        if (secureKeyValueStore is SecureSubKeyValueStore) {
            secureKeyValueStore.deleteAllData()
        } else {
            throw RuntimeException("this is not a SubKeyValueStore")
        }
    }

    fun getAddress(isChangeChain: Boolean, index: Int): BtcvAddress? {
        // See if we have it in the store
        val id = getLeafNodeId(network, accountIndex, isChangeChain, index, false, derivationType)
        val addressNodeBytes = secureKeyValueStore.getPlaintextValue(id)
        val purpose: HdKeyPath = when (derivationType) {
            BipDerivationType.BIP44 -> HdKeyPath.BIP44
            BipDerivationType.BIP49 -> HdKeyPath.BIP49
            BipDerivationType.BIP84 -> HdKeyPath.BIP84
            else -> throw NotImplementedError()
        }
        val path = purpose
                .getCoinTypeBitcoin(network.isTestnet())
                .getAccount(accountIndex)
                .getChain(!isChangeChain)
                .getAddress(index)
        if (addressNodeBytes != null) {
            // We have it already, no need to calculate it
            return bytesToAddress(addressNodeBytes, path)
        }

        // We don't have it, need to calculate it from the public key
        val publicKey = getPublicKey(isChangeChain, index)
        return publicKey.toAddress(network, derivationType.addressType)?.apply {
            this.bip32Path = path
            // Store it for next time
            secureKeyValueStore.storePlaintextValue(id, addressToBytes(this))
        }!!
    }

    companion object {

        fun PublicKey.toAddress(networkParameters: CommonNetworkParameters, addressType: AddressType, ignoreCompression: Boolean = false): BtcvAddress? {
            return when (addressType) {
                AddressType.P2PKH -> toP2PKHAddress(networkParameters)
                AddressType.P2SH_P2WPKH -> toNestedP2WPKH(networkParameters, ignoreCompression)
                AddressType.P2WPKH -> toP2WPKH(networkParameters, ignoreCompression)
            }
        }

        private fun PublicKey.toP2PKHAddress(network: CommonNetworkParameters): BtcvAddress? {
            if (publicKeyHash.size != 20) {
                return null
            }
            val all = ByteArray(BitcoinAddress.NUM_ADDRESS_BYTES)
            all[0] = (network.getStandardAddressHeader() and 0xFF).toByte()
            System.arraycopy(publicKeyHash, 0, all, 1, 20)
            return BtcvAddress(BitcoinVaultTest, all)
        }

        private fun PublicKey.toNestedP2WPKH(networkParameters: CommonNetworkParameters, ignoreCompression: Boolean = false): BtcvAddress? {
            if (ignoreCompression || isCompressed) {
                val hashedPublicKey = pubKeyHashCompressed
                val prefix = byteArrayOf(Script.OP_0.toByte(), hashedPublicKey.size.toByte())
                return fromP2SHBytes(HashUtils.addressHash(
                        BitUtils.concatenate(prefix, hashedPublicKey)), networkParameters)
            }
            throw IllegalStateException("Can't create segwit address from uncompressed key")
        }

        fun fromP2SHBytes(bytes: ByteArray, network: CommonNetworkParameters): BtcvAddress? {
            if (bytes.size != 20) {
                return null
            }
            val all = ByteArray(BitcoinAddress.NUM_ADDRESS_BYTES)
            all[0] = (network.getMultisigAddressHeader() and 0xFF).toByte()
            System.arraycopy(bytes, 0, all, 1, 20)
            return BtcvAddress(BitcoinVaultTest, all)
        }

        fun PublicKey.toP2WPKH(networkParameters: CommonNetworkParameters, ignoreCompression: Boolean = false): BtcvSegwitAddress =
                if (ignoreCompression || isCompressed) {
                    BtcvSegwitAddress(BitcoinVaultTest, networkParameters, 0x00, HashUtils.addressHash(pubKeyCompressed))
                } else {
                    throw IllegalStateException("Can't create segwit address from uncompressed key")
                }


        @Throws(KeyCipher.InvalidKeyCipher::class)
        fun createNew(bip32Root: HdKeyNode, network: CommonNetworkParameters, accountIndex: Int,
                      secureKeyValueStore: SecureKeyValueStore, cipher: KeyCipher?,
                      derivationType: BipDerivationType): HDAccountKeyManager {
            val bip44Root = bip32Root.createChildNode(derivationType.getHardenedPurpose())
            val coinTypeRoot = bip44Root.createChildNode(network.getBip44CoinType() or -0x80000000)

            // Create the account root.
            val accountRoot = coinTypeRoot.createChildNode(accountIndex or -0x80000000)
            return createFromAccountRoot(accountRoot, network, accountIndex, secureKeyValueStore, cipher, derivationType)
        }

        @Throws(KeyCipher.InvalidKeyCipher::class)
        fun createFromAccountRoot(accountRoot: HdKeyNode, network: CommonNetworkParameters,
                                  accountIndex: Int, secureKeyValueStore: SecureKeyValueStore,
                                  cipher: KeyCipher?, derivationType: BipDerivationType): HDAccountKeyManager {

            // Store the account root (xPub and xPriv) key
            secureKeyValueStore.encryptAndStoreValue(getAccountNodeId(network, accountIndex, derivationType),
                    accountRoot.toCustomByteFormat(), cipher)
            secureKeyValueStore.storePlaintextValue(getAccountNodeId(network, accountIndex, derivationType),
                    accountRoot.publicNode.toCustomByteFormat())

            // Create the external chain root. Store the private node encrypted and the public node in plain text
            val externalChainRoot = accountRoot.createChildNode(0)
            secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, false, derivationType),
                    externalChainRoot.toCustomByteFormat(), cipher)
            secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, false, derivationType),
                    externalChainRoot.publicNode.toCustomByteFormat())

            // Create the change chain root. Store the private node encrypted and the public node in plain text
            val changeChainRoot = accountRoot.createChildNode(1)
            secureKeyValueStore.encryptAndStoreValue(getChainNodeId(network, accountIndex, true, derivationType),
                    changeChainRoot.toCustomByteFormat(), cipher)
            secureKeyValueStore.storePlaintextValue(getChainNodeId(network, accountIndex, true, derivationType),
                    changeChainRoot.publicNode.toCustomByteFormat())
            return HDAccountKeyManager(accountIndex, network, secureKeyValueStore, derivationType)
        }

        protected fun getAccountNodeId(network: CommonNetworkParameters?, accountIndex: Int, derivationType: BipDerivationType): ByteArray {
            // Create a compact unique account ID
            val id = ByteArray(1 + 1 + 4)
            id[0] = derivationType.purpose
            id[1] = (if (network!!.isProdnet()) 0 else 1).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            return id
        }

        protected fun getChainNodeId(network: CommonNetworkParameters?, accountIndex: Int, isChangeChain: Boolean,
                                     derivationType: BipDerivationType): ByteArray {
            // Create a compact unique chain node ID
            val id = ByteArray(1 + 1 + 4 + 1)
            id[0] = derivationType.purpose
            id[1] = (if (network!!.isProdnet()) 0 else 1).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            id[6] = (if (isChangeChain) 1 else 0).toByte() // external chain or change chain
            return id
        }

        private fun getLeafNodeId(network: CommonNetworkParameters?, accountIndex: Int, isChangeChain: Boolean, index: Int,
                                  isHdNode: Boolean, derivationType: BipDerivationType): ByteArray {
            // Create a compact unique address or HD node ID
            val id = ByteArray(1 + 1 + 4 + 1 + 4 + 1)
            id[0] = derivationType.purpose
            id[1] = (if (network!!.isProdnet()) 0 else 1).toByte() // network
            BitUtils.uint32ToByteArrayLE(accountIndex.toLong(), id, 2) // account index
            id[6] = (if (isChangeChain) 1 else 0).toByte() // external chain or change chain
            BitUtils.uint32ToByteArrayLE(index.toLong(), id, 7) // address index
            id[11] = (if (isHdNode) 1 else 0).toByte() // is HD node or address
            return id
        }

        private fun addressToBytes(address: BitcoinAddress): ByteArray {
            val writer = ByteWriter(1024)
            // Add address as bytes
            writer.putBytes(address.allAddressBytes)
            // Add address as string
            val addressString = address.toString()
            writer.put(addressString.length.toByte())
            writer.putBytes(addressString.toByteArray())
            return writer.toBytes()
        }

        private fun bytesToAddress(bytes: ByteArray, path: HdKeyPath): BtcvAddress? {
            return try {
                val reader = ByteReader(bytes)
                // Address bytes
                reader.getBytes(21)
                // Read length encoded string
                val addressString = String(reader.getBytes(reader.get().toInt()))
                val address = BtcvAddress.fromString(BitcoinVaultTest, addressString)
                address?.bip32Path = path
                address
            } catch (e: ByteReader.InsufficientBytesException) {
                throw RuntimeException(e)
            }
        }
    }
}