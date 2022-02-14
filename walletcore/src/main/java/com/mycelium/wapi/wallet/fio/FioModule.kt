package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.fio.FioAccountContext.Companion.ACCOUNT_TYPE_FROM_MASTERSEED
import com.mycelium.wapi.wallet.fio.FioAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PRIV
import com.mycelium.wapi.wallet.fio.FioAccountContext.Companion.ACCOUNT_TYPE_UNRELATED_X_PUB
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import java.text.DateFormat
import java.util.*

class FioModule(
        private val serverFioEventsPublisher: IServerFioEventsPublisher,
        private val serializationProviderWrapper: IAbiFioSerializationProviderWrapper,
        private val apiEndpoints: FioApiEndpoints,
        private val historyEndpoints: FioHistoryEndpoints,
        private val secureStore: SecureKeyValueStore,
        private val backing: Backing<FioAccountContext>,
        private val walletDB: WalletDB,
        private val networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val fioKeyManager: FioKeyManager,
        private val accountListener: AccountListener?,
        private val walletManager: WalletManager,
        private val tpid: String = ""
) : WalletModule(metaDataStorage) {

    private val coinType = if (networkParameters.isProdnet) FIOMain else FIOTest
    private val accounts = mutableMapOf<UUID, FioAccount>()
    private var fioServerLogsList = Collections.synchronizedList(LinkedList<String>())
    override val id = ID

    init {
        assetsList.add(coinType)
    }

    fun addFioServerLog(log: String) {
        fioServerLogsList.add(log)
    }

    fun getFioServerLogsListAndClear(): List<String> {
        synchronized(fioServerLogsList) {
            val result = LinkedList<String>()
            result.addAll(fioServerLogsList)
            fioServerLogsList.clear()
            return result
        }
    }

    fun getAllRegisteredFioNames() = accounts.values.map { it.registeredFIONames }.flatten()

    fun getAllRegisteredFioDomains() = accounts.values.map { it.registeredFIODomains }.flatten()

    fun getFioAccountByFioName(fioName: String): UUID? = accounts.values.firstOrNull { fioAccount ->
        fioName in fioAccount.registeredFIONames.map { it.name }
    }?.id

    fun getFioAccountByFioDomain(fioDomain: String): UUID? = accounts.values.firstOrNull { fioAccount ->
        fioDomain in fioAccount.registeredFIODomains.map { it.domain }
    }?.id

    fun getFIONames(domainName: String): List<RegisteredFIOName> {
        return getAllRegisteredFioNames().filter { it.name.split("@")[1] == domainName }
    }

    fun getFIONames(account: WalletAccount<*>): List<RegisteredFIOName> {
        if (account is FioAccount) {
            return account.registeredFIONames
        }

        val fioNames = walletDB.fioNameAccountMappingsQueries.selectFioNamesByAccountUuid(account.id).executeAsList()
        return getAllRegisteredFioNames().filter { fioNames.contains(it.name) }
    }

    fun getFIONameInfo(name: String): RegisteredFIOName = getAllRegisteredFioNames().first { it.name == name }

    fun getFIODomainInfo(domain: String): FIODomain = getAllRegisteredFioDomains().first { it.domain == domain }

    fun getKnownNames(): List<FioName> = walletDB.fioKnownNamesQueries.selectAllFioKnownNames()
            .executeAsList().sortedBy { "${it.name}@${it.domain}" }

    fun addKnownName(fioName: FioName) = walletDB.fioKnownNamesQueries.insert(fioName)

    fun deleteKnownName(fioName: FioName) = walletDB.fioKnownNamesQueries.delete(fioName)

    fun getConnectedAccounts(fioName: String): List<WalletAccount<*>> {
        val connected = ArrayList<WalletAccount<*>>()
        val accountsList = walletDB.fioNameAccountMappingsQueries.selectAccountsUuidByFioName(fioName).executeAsList()
        accountsList.forEach {
            val account = walletManager.getAccount(it)
            if (account != null) {
                connected.add(account)
            }
        }
        return connected
    }

    /**
     * This function takes a full list of mapped accounts. Any additional accounts that were
     * previously mapped are un-mapped using a mapping of "0" as address as per
     * <a href="https://developers.fioprotocol.io/wallet-integration-guide/mapping-pub-addresses#changing-or-removing-nbpas">the FIO doc's recommendation</a>.
     */
    fun mapFioNameToAccounts(fioName: String, accounts: List<WalletAccount<*>>) {
        val fioAccount = walletManager.getAccount(getFioAccountByFioName(fioName)!!) as FioAccount
        val tokenPublicAddresses = ArrayList<TokenPublicAddress>()
        val oldMappings = walletDB
                .fioNameAccountMappingsQueries
                .selectPublicAddressesByFioName(fioName).executeAsList().map {
                    "${it.tokenCode}-${it.chainCode}" to TokenPublicAddress(it.pubAddress, it.chainCode, it.tokenCode)
                }.toMap().toMutableMap()
        // We begin with creating a list of addresses for FIO blockchain mapping transaction
        accounts.forEach {
            val chainCode = it.basedOnCoinType.symbol.toUpperCase(Locale.US)
            val tokenCode = it.coinType.symbol.toUpperCase(Locale.US)
            val currentTokenAddress = oldMappings.remove("$tokenCode-$chainCode")
            if (currentTokenAddress?.publicAddress != it.receiveAddress.toString()) {
                tokenPublicAddresses.add(TokenPublicAddress(it.receiveAddress.toString(),
                        chainCode, tokenCode))
            }
        }
        // remaining "current" tokens in oldMappings were not requested to be mapped so we unmap those.
        // TODO: 10/11/20 once FIP4 is available through the sdk, replace `= "0"` with the adequate call.
        tokenPublicAddresses.addAll(oldMappings.map { it.value.apply { publicAddress = "0" } })
        if (tokenPublicAddresses.isEmpty()) {
            // nothing changed
            return
        }
        // 5 is the maximum for tokens to be set in one call. Apparently the sdk doesn't know, so we
        // have to chunk here.
        tokenPublicAddresses.chunked(5).forEach {
            if (!fioAccount.addPubAddress(fioName, it)) {
                // TODO reconsider, probably should throw an error
                return
            }
        }

        // Refresh mappings in the database
        walletDB.fioNameAccountMappingsQueries.deleteAllMappings(fioName)
        accounts.forEach {
            walletDB.fioNameAccountMappingsQueries.insertMapping(fioName, it.receiveAddress.toString(),
                    it.basedOnCoinType.symbol.toUpperCase(Locale.US), it.coinType.symbol.toUpperCase(Locale.US), it.id)
        }
    }

    fun getFioTxMetadata(txid: String) =
            walletDB.fioOtherBlockchainTransactionsQueries.selectTxById(txid).executeAsOneOrNull()?.run {
                FIOOBTransaction(obtId, payerFioAddress, payeeFioAddress, deserializedContent?.memo?:"")
            }

    private fun getPrivkeyStringByIndex(accountIndex: Int) = fioKeyManager.getFioPrivateKey(accountIndex).getBase58EncodedPrivateKey(networkParameters)

    private fun getPrivkeyStringByHdNode(node: HdKeyNode) = node.createChildNode(0).createChildNode(0).privateKey.getBase58EncodedPrivateKey(networkParameters)

    private fun getPrivkeyString(privateKey: InMemoryPrivateKey) = privateKey.getBase58EncodedPrivateKey(networkParameters)

    private fun getFioAddressByPrivkey(privateKey: InMemoryPrivateKey): FioAddress {
        val privkeyString = privateKey.getBase58EncodedPrivateKey(networkParameters)
        return FioAddress(coinType, FioAddressData(FIOSDK.derivedPublicKey(privkeyString)))
    }

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts()
                    .associateBy({ it.uuid }, { accountFromUUID(it) })

    private fun accountFromUUID(context: FioAccountContext): WalletAccount<*> {
        return if (secureStore.getPlaintextValue(context.uuid.toString().toByteArray()) != null) {
            val fioAddress = FioAddress(coinType, FioAddressData(String(secureStore.getPlaintextValue(context.uuid.toString().toByteArray()))))
            val accountContext = createAccountContext(context.uuid, context.accountType)
            val account = createAccount(accountContext, address = fioAddress, isRestore = true)
            accounts[account.id] = account
            account
        } else {
            val accountContext = createAccountContext(context.uuid, context.accountType)
            val account = createAccount(accountContext, privkeyString = getPrivkeyStringByIndex(accountContext.accountIndex), isRestore = true)
            accounts[account.id] = account
            account
        }
    }

    private fun createAccount(accountContext: FioAccountContext,
                              privkeyString: String? = null,
                              address: FioAddress? = null,
                              isRestore: Boolean): FioAccount {
        val fioEndpoints = FioEndpoints(apiEndpoints, historyEndpoints)
        serverFioEventsPublisher.setFioServerListChangedListeners(fioEndpoints, fioEndpoints)
        val fioBlockchainService = FioBlockchainService(coinType, fioEndpoints, tpid, serializationProviderWrapper)
        serverFioEventsPublisher.setFioTpidChangedListener(fioBlockchainService)
        val fioServerLogsListWrapper = FioServerLogsListWrapper(fioServerLogsList)
        if (!isRestore) {
            backing.createAccountContext(accountContext)
        }
        val fioAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
        return FioAccount(fioBlockchainService, fioEndpoints, accountContext = accountContext, backing = fioAccountBacking,
                accountListener = accountListener, privkeyString = privkeyString, address = address,
                walletManager = walletManager, fioServerLogsListWrapper = fioServerLogsListWrapper)
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is FIOMasterseedConfig -> {
                val newIndex = getCurrentBip44Index() + 1
                val accountContext = createAccountContext(fioKeyManager.getUUID(newIndex),
                        accountType = ACCOUNT_TYPE_FROM_MASTERSEED)
                baseLabel = accountContext.accountName
                result = createAccount(accountContext, privkeyString = getPrivkeyStringByIndex(accountContext.accountIndex), isRestore = false)
            }
            is FIOUnrelatedHDConfig -> {
                val hdKeyNode = config.hdKeyNodes[0]
                val uuid = hdKeyNode.uuid
                val accountContext = createAccountContext(uuid, isReadOnly = true,
                        accountType = ACCOUNT_TYPE_UNRELATED_X_PRIV)
                baseLabel = if (config.labelBase.isNotEmpty()) config.labelBase else accountContext.accountName
                result = createAccount(accountContext, privkeyString = getPrivkeyStringByHdNode(hdKeyNode), isRestore = false)
            }
            is FIOAddressConfig -> {
                val pubkeyString = try {
                    when (config.address.getSubType()) {
                        FioAddressSubtype.ACTOR.toString() -> FioBlockchainService.getPubkeyByActor(
                                FioEndpoints(apiEndpoints, historyEndpoints), config.address.toString())
                        FioAddressSubtype.ADDRESS.toString() -> FioBlockchainService.getPubkeyByFioAddress(
                                FioEndpoints(apiEndpoints, historyEndpoints), config.address.toString(),
                                coinType.symbol, coinType.symbol).publicAddress
                        else -> config.address.toString()
                    }
                } catch (e: Exception) {
                    if (e is FIOError) {
                        fioServerLogsList.add(e.toJson())
                    }
                    null
                }
                if (pubkeyString.isNullOrEmpty()) {
                    throw IllegalStateException("Cannot find public key for: ${config.address}")
                }
                val fioAddress = FioAddress(coinType, FioAddressData(pubkeyString))
                val uuid = UUID.nameUUIDFromBytes(fioAddress.getBytes())
                secureStore.storePlaintextValue(uuid.toString().toByteArray(),
                        fioAddress.toString().toByteArray())
                val accountContext = createAccountContext(uuid, isReadOnly = true,
                        accountType = ACCOUNT_TYPE_UNRELATED_X_PUB)
                baseLabel = accountContext.accountName
                result = createAccount(accountContext, address = fioAddress, isRestore = false)
            }
            is FIOPrivateKeyConfig -> {
                val uuid = UUID.nameUUIDFromBytes(getFioAddressByPrivkey(config.privkey).getBytes())
                secureStore.encryptAndStoreValue(uuid.toString().toByteArray(),
                        config.privkey.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())
                val accountContext = createAccountContext(uuid, isReadOnly = true,
                        accountType = ACCOUNT_TYPE_UNRELATED_X_PRIV)
                baseLabel = accountContext.accountName
                result = createAccount(accountContext, privkeyString = getPrivkeyString(config.privkey), isRestore = false)
            }
            else -> {
                throw NotImplementedError("Unknown config")
            }
        }
        accounts[result.id] = result
        result.label = createLabel(baseLabel)
        storeLabel(result.id, result.label)
        return result
    }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed() }
            .maxBy { it.accountIndex }
            ?.accountIndex
            ?: -1

    override fun canCreateAccount(config: Config): Boolean {
        return config is FIOMasterseedConfig || config is FIOAddressConfig || config is FIOPrivateKeyConfig
                || config is FIOUnrelatedHDConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is FioAccount) {
            if (secureStore.getPlaintextValue(walletAccount.id.toString().toByteArray()) != null) {
                secureStore.deletePlaintextValue(walletAccount.id.toString().toByteArray())
            }
            backing.deleteAccountContext(walletAccount.id)
            accounts.remove(walletAccount.id)
            true
        } else {
            false
        }
    }

    override fun getAccounts(): List<WalletAccount<*>> {
        return accounts.values.toList()
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private fun createAccountContext(uuid: UUID, accountType: Int, isReadOnly: Boolean = false): FioAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            FioAccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.accountIndex,
                    accountContextInDB.registeredFIONames,
                    accountContextInDB.registeredFIODomains,
                    accountContextInDB.archived,
                    accountContextInDB.blockHeight,
                    accountContextInDB.accountType,
                    accountContextInDB.actionSequenceNumber)
        } else {
            FioAccountContext(
                    uuid,
                    coinType,
                    if (isReadOnly) DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
                    else "FIO ${getCurrentBip44Index() + 2}",
                    Balance.getZeroBalance(coinType),
                    backing::updateAccountContext,
                    if (isReadOnly) 0 else getCurrentBip44Index() + 1,
                    accountType = accountType)
        }
    }

    fun getBip44Path(account: FioAccount): HdKeyPath? =
            HdKeyPath.valueOf("m/44'/235'/${account.accountIndex}'/0/0")

    companion object {
        const val ID: String = "FIO"
        const val DEFAULT_BUNDLED_TXS_NUM = 100
    }
}

fun WalletManager.getFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible() }
        .map { it as FioAccount }.sortedBy { it.accountIndex }

fun WalletManager.getActiveSpendableFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible() && it.isActive && it.canSpend() }
        .map { it as FioAccount }.sortedBy { it.accountIndex }

fun WalletManager.getActiveFioAccounts() = getAccounts()
        .filter { it is FioAccount && it.isVisible() && it.isActive }
        .map { it as FioAccount }

fun WalletManager.getActiveFioAccount(fioName: String) = getActiveFioAccounts().firstOrNull { fioAccount ->
    fioAccount.registeredFIONames.any {
        it.name == fioName
    }
}

class FioServerLogsListWrapper(private val fioServerLogsList: MutableList<String>) {
    fun addLog(log: String) {
        fioServerLogsList.add(log)
    }
}