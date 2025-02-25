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
package com.mycelium.wapi.wallet.btc.single

import com.google.common.base.Optional
import com.google.common.collect.Lists
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.BipDerivationType.Companion.getDerivationTypeByAddressType
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.BitcoinTransaction
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wapi.SyncStatus
import com.mycelium.wapi.SyncStatusInfo
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.api.request.QueryTransactionInventoryRequest
import com.mycelium.wapi.model.BalanceSatoshis
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.Reference
import java.util.*
import java.util.logging.Level


open class SingleAddressAccount @JvmOverloads constructor(private var _context: SingleAddressAccountContext, keyStore: PublicPrivateKeyStore,
                                                          network: NetworkParameters, private val _backing: SingleAddressBtcAccountBacking, wapi: Wapi,
                                                          private val changeAddressModeReference: Reference<ChangeAddressMode>, shouldPersistAddress: Boolean = true) :
    AbstractBtcAccount(_backing, network, wapi), ExportableAccount {
    private val _addressList: MutableList<BitcoinAddress>
    private val _keyStore: PublicPrivateKeyStore
    private val publicKey: PublicKey?
    var toRemove = false
    private fun persistAddresses() {
        try {
            getPrivateKey(AesKeyCipher.defaultKeyCipher())?.let { privateKey ->
                val allPossibleAddresses: Map<AddressType, BitcoinAddress> =
                        privateKey?.publicKey?.getAllSupportedAddresses(_network, true).orEmpty()
                if (allPossibleAddresses.size != _context.addresses.size) {
                    for (address in allPossibleAddresses.values) {
                        if (address != _context.addresses[address.type]) {
                            _keyStore.setPrivateKey(address.allAddressBytes, privateKey, AesKeyCipher.defaultKeyCipher())
                        }
                    }
                    _context.addresses = allPossibleAddresses
                    _context.persist(_backing)
                }
            }
        } catch (invalidKeyCipher: InvalidKeyCipher) {
            _logger.log(Level.SEVERE, invalidKeyCipher.message)
        }
    }

    fun markToRemove() {
        toRemove = true
    }

    @Synchronized
    override fun archiveAccount() {
        if (_context.isArchived()) {
            return
        }
        clearInternalStateInt(true)
        _context.persistIfNecessary(_backing)
    }

    @Synchronized
    override fun activateAccount() {
        if (!_context.isArchived()) {
            return
        }
        clearInternalStateInt(false)
        _context.persistIfNecessary(_backing)
    }

    /**
     * check canSign before call this method
     */
    override fun signMessage(message: String, address: Address?): String {
        return try {
            val key = getPrivateKey(AesKeyCipher.defaultKeyCipher())
            key!!.signMessage(message).base64Signature
        } catch (invalidKeyCipher: InvalidKeyCipher) {
            throw RuntimeException(invalidKeyCipher)
        }
    }

    override fun hasHadActivity(): Boolean = getTransactionHistory(0,1).isNotEmpty()

    override fun dropCachedData() {
        if (_context.isArchived()) {
            return
        }
        clearInternalStateInt(false)
        _context.persistIfNecessary(_backing)
    }

    override fun isValidEncryptionKey(cipher: KeyCipher): Boolean {
        return _keyStore.isValidEncryptionKey(cipher)
    }

    override fun isDerivedFromInternalMasterseed(): Boolean {
        return false
    }

    private fun clearInternalStateInt(isArchived: Boolean) {
        _backing.clear()
        _context = SingleAddressAccountContext(_context.id, _context.addresses, isArchived, 0,
                                               _context.defaultAddressType)
        _context.persist(_backing)
        _cachedBalance = null
        if (isActive) {
            _cachedBalance = calculateLocalBalance()
        }
    }

    override suspend fun doSynchronization(mode: SyncMode): Boolean {
        if (!maySync) {
            return false
        }
        checkNotArchived()
        syncTotalRetrievedTransactions = 0
        return try {
            if (synchronizeUnspentOutputs(_addressList) == -1) {
                return false
            }
            if (!maySync) {
                return false
            }
            // Monitor young transactions
            if (!monitorYoungTransactions()) {
                return false
            }
            if (!maySync) {
                return false
            }
            //lets see if there are any transactions we need to discover
            if (!mode.ignoreTransactionHistory) {
                if (!discoverTransactions()) {
                    return false
                }
            }
            if (!maySync) {
                return false
            }
            // recalculate cached Balance
            updateLocalBalance()
            if (!maySync) {
                return false
            }
            _context.persistIfNecessary(_backing)
            true
        } finally {
            syncTotalRetrievedTransactions = 0
        }
    }

    override val availableAddressTypes: List<AddressType>
        get() = if (publicKey != null && !publicKey.isCompressed) {
            listOf(AddressType.P2PKH)
        } else ArrayList(_context.addresses.keys)

    override fun getReceivingAddress(addressType: AddressType): BitcoinAddress? {
        return getAddress(addressType)
    }

    override fun setDefaultAddressType(addressType: AddressType) {
        _context.defaultAddressType = addressType
        _context.persistIfNecessary(_backing)
    }

    private suspend fun discoverTransactions(): Boolean {
        // Get the latest transactions
        val discovered: List<Sha256Hash>
        val txIds: MutableList<Sha256Hash> = ArrayList()
        for (address in _addressList) {
            try {
                val result =
                    _wapi.queryTransactionInventory(QueryTransactionInventoryRequest(Wapi.VERSION, listOf(address)))
                        .result
                txIds.addAll(result.txIds)
                setBlockChainHeight(result.height)
            } catch (e: WapiException) {
                lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
                if (e.errorCode == Wapi.ERROR_CODE_NO_SERVER_CONNECTION) {
                    _logger.log(Level.SEVERE, "Server connection failed with error code: " + e.errorCode, e)
                    postEvent(WalletManager.Event.SERVER_CONNECTION_ERROR)
                    return false
                } else if (e.errorCode == Wapi.ERROR_CODE_RESPONSE_TOO_LARGE) {
                    postEvent(WalletManager.Event.TOO_MANY_TRANSACTIONS)
                }
            }
        }

        // get out right there if there is nothing to work with
        if (txIds.size == 0) {
            return true
        }
        discovered = txIds

        // Figure out whether there are any transactions we need to fetch
        val toFetch: MutableList<Sha256Hash> = LinkedList()
        for (id in discovered) {
            if (!_backing.hasTransaction(id)) {
                toFetch.add(id)
            }
        }

        // Fetch any missing transactions
        val chunkSize = 50
        var fromIndex = 0
        while (fromIndex < toFetch.size) {
            if (!maySync) {
                return false
            }
            try {
                val toIndex = Math.min(fromIndex + chunkSize, toFetch.size)
                val response = getTransactionsBatched(toFetch.subList(fromIndex, toIndex)).result
                val transactionsEx: Collection<TransactionEx> =
                    Lists.newLinkedList<TransactionEx>(response.transactions)
                handleNewExternalTransactions(transactionsEx)
            } catch (e: WapiException) {
                _logger.log(Level.SEVERE, "Server connection failed with error code: " + e.errorCode, e)
                lastSyncInfo = SyncStatusInfo(SyncStatus.ERROR)
                postEvent(WalletManager.Event.SERVER_CONNECTION_ERROR)
                return false
            }
            fromIndex += chunkSize
        }
        return true
    }

    override fun getReceivingAddress(): Optional<BitcoinAddress> {
        //removed checkNotArchived, cause we wont to know the address for archived acc
        //to display them as archived accounts in "Accounts" tab
        return Optional.of(address)
    }

    override fun canSpend(): Boolean {
        return _keyStore.hasPrivateKey(address.allAddressBytes)
    }

    override fun canSign(): Boolean =
            _keyStore.hasPrivateKey(address.allAddressBytes)

    override fun isMine(address: BitcoinAddress): Boolean {
        return _addressList.contains(address)
    }

    override fun getBlockChainHeight(): Int {
        checkNotArchived()
        return _context.getBlockHeight()
    }

    override fun setBlockChainHeight(blockHeight: Int) {
        checkNotArchived()
        _context.setBlockHeight(blockHeight)
    }

    override fun persistContextIfNecessary() {
        _context.persistIfNecessary(_backing)
    }

    // public method that needs no synchronization
    override val isArchived: Boolean
        get() =
            _context.isArchived()

    // public method that needs no synchronization
    override val isActive: Boolean
        get() =
            !isArchived && !toRemove

    override fun onNewTransaction(t: BitcoinTransaction) {
        // Nothing to do for this account type
    }

    override fun isOwnInternalAddress(address: BitcoinAddress): Boolean {
        return isMine(address)
    }

    override fun isOwnExternalAddress(address: BitcoinAddress): Boolean {
        return isMine(address)
    }

    override val id: UUID
        get() = _context.id

    override val changeAddress: BitcoinAddress
        get() = address

    override fun getChangeAddress(destinationAddress: BitcoinAddress): BitcoinAddress {
        return when (changeAddressModeReference.get()) {
            ChangeAddressMode.P2WPKH -> getAddress(AddressType.P2WPKH)
            ChangeAddressMode.P2SH_P2WPKH -> getAddress(AddressType.P2SH_P2WPKH)
            ChangeAddressMode.P2TR -> getAddress(AddressType.P2TR)
            ChangeAddressMode.PRIVACY -> getAddress(destinationAddress.type)
            else -> throw IllegalStateException()
        } ?: address
    }

    override fun getChangeAddress(destinationAddresses: List<BitcoinAddress>): BitcoinAddress {
        val mostUsedTypesMap: MutableMap<AddressType, Int> = EnumMap(AddressType::class.java)
        for (address in destinationAddresses) {
            var currentValue = mostUsedTypesMap[address.type]
            if (currentValue == null) {
                currentValue = 0
            }
            mostUsedTypesMap[address.type] = currentValue + 1
        }
        var max = 0
        var maxedOn: AddressType? = null
        for (addressType in mostUsedTypesMap.keys) {
            if (mostUsedTypesMap[addressType]!! > max) {
                max = mostUsedTypesMap[addressType]!!
                maxedOn = addressType
            }
        }
        return when (changeAddressModeReference.get()) {
            ChangeAddressMode.P2WPKH -> getAddress(AddressType.P2WPKH)
            ChangeAddressMode.P2SH_P2WPKH -> getAddress(AddressType.P2SH_P2WPKH)
            ChangeAddressMode.P2TR -> getAddress(AddressType.P2TR)
            ChangeAddressMode.PRIVACY -> getAddress(maxedOn)
            else -> throw IllegalStateException()
        } ?: address
    }

    @Throws(InvalidKeyCipher::class)
    override fun getPrivateKey(publicKey: PublicKey, cipher: KeyCipher): InMemoryPrivateKey? {
        val privateKey = getPrivateKey(cipher)
        if ((getPublicKey() == publicKey || PublicKey(publicKey.pubKeyCompressed) == publicKey)
                && privateKey != null) {
            return if (publicKey.isCompressed) {
                InMemoryPrivateKey(privateKey.privateKeyBytes, true)
            } else {
                privateKey
            }
        }
        return null
    }

    @Throws(InvalidKeyCipher::class)
    override fun getPrivateKeyForAddress(address: BitcoinAddress, cipher: KeyCipher): InMemoryPrivateKey? {
        val privateKey = getPrivateKey(cipher)
        return if (_addressList.contains(address) && privateKey != null) {
            if (address.type in arrayOf(AddressType.P2SH_P2WPKH, AddressType.P2WPKH, AddressType.P2TR)) {
                InMemoryPrivateKey(privateKey.privateKeyBytes, true)
            } else {
                privateKey
            }
        } else {
            null
        }
    }

    override fun getPublicKeyForAddress(address: BitcoinAddress): PublicKey? {
        return if (_addressList.contains(address)) {
            getPublicKey()
        } else {
            null
        }
    }

    override fun toString(): String {
        val sb = StringBuilder()
        sb.append("Simple ID: ").append(id)
        if (isArchived) {
            sb.append(" Archived")
        } else {
            if (_cachedBalance == null) {
                sb.append(" Balance: not known")
            } else {
                sb.append(" Balance: ").append(_cachedBalance)
            }
            val receivingAddress = receivingAddress
            sb.append(" Receiving Address: ")
                .append(if (receivingAddress.isPresent) receivingAddress.get().toString() else "")
            sb.append(" Spendable Outputs: ").append(getSpendableOutputs(0).size)
        }
        return sb.toString()
    }

    @Throws(InvalidKeyCipher::class)
    fun forgetPrivateKey(cipher: KeyCipher?) {
        for (address in getPublicKey()?.getAllSupportedAddresses(_network, true)?.values
                ?: listOf()) {
            _keyStore.forgetPrivateKey(address.allAddressBytes, cipher)
        }
    }

    @Throws(InvalidKeyCipher::class)
    override fun getPrivateKey(cipher: KeyCipher): InMemoryPrivateKey? {
        return _keyStore.getPrivateKey(address.allAddressBytes, cipher)
    }

    /**
     * This method is used for Colu account, so method should NEVER persist addresses as only P2PKH addresses are used for Colu
     */
    @Throws(InvalidKeyCipher::class)
    fun setPrivateKey(privateKey: InMemoryPrivateKey?, cipher: KeyCipher?) {
        _keyStore.setPrivateKey(address.allAddressBytes, privateKey, cipher)
    }

    fun getPublicKey(): PublicKey? {
        return _keyStore.getPublicKey(address.allAddressBytes)
    }

    /**
     * @return default address
     */
    val address: BitcoinAddress
        get() {
            val defaultAddress = getAddress(_context.defaultAddressType)
            return defaultAddress ?: _context.addresses.values.iterator().next()
        }

    fun getAddress(type: AddressType?): BitcoinAddress? {
        if (publicKey != null && !publicKey.isCompressed) {
            if (type in arrayOf(AddressType.P2SH_P2WPKH, AddressType.P2WPKH, AddressType.P2TR)) {
                return null
            }
        }
        return _context.addresses[type]
    }

    override fun getExportData(cipher: KeyCipher): ExportableAccount.Data {
        var privKey = Optional.absent<String>()
        val publicDataMap: MutableMap<BipDerivationType, String> = EnumMap(BipDerivationType::class.java)
        if (canSpend()) {
            try {
                val privateKey = _keyStore.getPrivateKey(address.allAddressBytes, cipher)
                privKey = Optional.of(privateKey.getBase58EncodedPrivateKey(network))
            } catch (ignore: InvalidKeyCipher) {
            }
        }
        for (type in availableAddressTypes) {
            val address = getAddress(type)
            if (address != null) {
                publicDataMap[getDerivationTypeByAddressType(type)] = address.toString()
            }
        }
        return ExportableAccount.Data(privKey, publicDataMap)
    }

    override suspend fun doDiscoveryForAddresses(lookAhead: List<BitcoinAddress>): Set<BipDerivationType> {
        // not needed for SingleAddressAccount
        return emptySet()
    }

    override fun broadcastTx(tx: Transaction): BroadcastResult {
        val btcTransaction = tx as BtcTransaction
        return broadcastTransaction(btcTransaction.tx!!)
    }

    companion object {
        @JvmStatic
        fun calculateId(address: BitcoinAddress): UUID {
            return addressToUUID(address)
        }
    }

    init {
        _addressList = ArrayList(3)
        _keyStore = keyStore
        publicKey = _keyStore.getPublicKey(address.allAddressBytes)
        if (shouldPersistAddress) {
            persistAddresses()
        }
        _addressList.addAll(_context.addresses.values)
        _cachedBalance =
            if (_context.isArchived()) BalanceSatoshis(0, 0, 0, 0, 0, 0, false, _allowZeroConfSpending) else calculateLocalBalance()
    }
}