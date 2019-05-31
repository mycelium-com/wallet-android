package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.single.*
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*

class ColuModule(val networkParameters: NetworkParameters,
                 internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                 val coluApi: ColuApi,
                 val wapi: Wapi,
                 val backing: WalletBacking<ColuAccountContext>,
                 val listener: AccountListener,
                 metaDataStorage: IMetaDataStorage,
                 private val singleAddressModule: BitcoinSingleAddressModule) : GenericModule(metaDataStorage), WalletModule {

    init {
        if (networkParameters.isProdnet) {
            assetsList.addAll(listOf(MASSCoin, MTCoin, RMCCoin))
        } else {
            assetsList.addAll(listOf(MASSCoinTest, MTCoinTest, RMCCoinTest))
        }
    }

    // Ensures that for all Colu accounts we have corresponding BTC SA accounts, if not - creates them
    // The linked BTC SA account creation should happen in Wallet upgrade situations only
    // In usual cases, BTC SA account will be created together with a Colu account
    override fun afterAccountsLoaded() {
        accounts.values.forEach {
            when(it) {
                is PublicColuAccount -> {
                    var btcAddress = it.receiveAddress as BtcAddress
                    val saId = SingleAddressAccount.calculateId(btcAddress.address)
                    if (singleAddressModule.getAccountById(saId) == null) {
                        val sa = singleAddressModule.createAccount(AddressSingleConfig(btcAddress))
                        singleAddressModule.createLabel(sa.label + " Bitcoin", sa.id)
                    }
                }

                is PrivateColuAccount -> {
                    val saId = SingleAddressAccount.calculateId(it.privateKey.publicKey.toAddress(networkParameters, AddressType.P2SH_P2WPKH, true))
                    if (singleAddressModule.getAccountById(saId) == null) {
                        singleAddressModule.createAccount(PrivateSingleConfig(it.privateKey, AesKeyCipher.defaultKeyCipher(), it.label + " Bitcoin"))
                    }
                }
            }
        }
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private val accounts = mutableMapOf<UUID, WalletAccount<*>>()
    private val MAX_ACCOUNTS_NUMBER = 1000
    override fun getId(): String = ID

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        for (context in contexts) {
            try {
                val addresses = context.publicKey?.getAllSupportedBtcAddresses(context.coinType, networkParameters)
                        ?: context.address
                val accountKey = getPrivateKey(addresses)
                val account = if (accountKey == null) {
                    PublicColuAccount(context, context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id) as ColuAccountBacking, backing
                            , listener, wapi)
                } else {
                    PrivateColuAccount(context, accountKey, context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id) as ColuAccountBacking, backing
                            , listener, wapi)
                }
                account.label = readLabel(account.id)
                accounts[account.id] = account
                result[account.id] = account
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    private fun getPrivateKey(addresses: Map<AddressType, BtcAddress>?): InMemoryPrivateKey? {
        var result: InMemoryPrivateKey? = null
        addresses?.let {
            for (address in it.values) {
                val privateKey = publicPrivateKeyStore.getPrivateKey(address, AesKeyCipher.defaultKeyCipher())
                if (privateKey != null) {
                    result = privateKey
                }
            }
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        var result: PublicColuAccount? = null
        var coinType: ColuMain? = null

        if (config is PrivateColuConfig) {
            val address = config.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)
            coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type, config.privateKey.publicKey, null
                        , false, 0)
                backing.createAccountContext(context)
                result = PrivateColuAccount(context, config.privateKey, type, networkParameters
                        , coluApi, backing.getAccountBacking(id) as ColuAccountBacking, backing, listener, wapi)
                publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, config.privateKey, config.cipher)
            }
        }  else if (config is AddressColuConfig) {
            coinType = coluMain(config.address.address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, config.address.getBytes())
                val context = ColuAccountContext(id, type, null, mapOf(config.address.type to config.address)
                        , false, 0)
                backing.createAccountContext(context)
                result = PublicColuAccount(context, type, networkParameters
                        , coluApi, backing.getAccountBacking(id) as ColuAccountBacking, backing, listener, wapi)
            }
        }

        result?.let {
            accounts[it.id] = result!!
            val baseName = createColuAccountLabel(coinType)
            it.label = createLabel(baseName, it.id)

            // Create a linked Colu account
            when (config) {
                is PrivateColuConfig -> {
                    val saAccount = singleAddressModule.createAccount(PrivateSingleConfig(config.privateKey, config.cipher, result!!.label + " Bitcoin", AddressType.P2PKH))
                    it.linkedAccount = saAccount as SingleAddressAccount
                }

                is AddressColuConfig -> {
                    val saAccount = singleAddressModule.createAccount(AddressSingleConfig(result!!.receiveAddress as BtcAddress))
                    it.linkedAccount = saAccount as SingleAddressAccount
                }
            }

        } ?: run {
            throw IllegalStateException("Account can't be created")
        }

        return result!!
    }

    private fun createColuAccountLabel(coinType: ColuMain?): String {
        var proposedLabel: String
        var i = 1

        while (i < MAX_ACCOUNTS_NUMBER) {
            proposedLabel = coinType!!.symbol + " " + Integer.toString(i)

            var foundExistingLabel = false
            for (coluAccount in accounts.values) {
                if (coluAccount.coinType != coinType || coluAccount.label ==
                null) {
                    continue
                }
                val curLabel = coluAccount.label
                if (proposedLabel == curLabel) {
                    foundExistingLabel = true
                    break
                }
            }

            if (!foundExistingLabel) {
                return proposedLabel
            }

            i++
        }
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
    }

    fun getColuAssets(address: Address): List<ColuMain> {
        return coluApi.getCoinTypes(address)
    }

    fun hasColuAssets(address: Address): Boolean {
        return getColuAssets(address).isNotEmpty()
    }

    private fun coluMain(address: Address, coinType: ColuMain?): ColuMain? = if (coinType == null) {
        val types = coluApi.getCoinTypes(address)
        if (types.isEmpty()) null
        else types[0]
    } else {
        coinType
    }

    override fun canCreateAccount(config: Config) = config is PrivateColuConfig || config is AddressColuConfig

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        if (walletAccount is PublicColuAccount || walletAccount is PrivateColuAccount ) {
            accounts.remove(walletAccount.id)
            publicPrivateKeyStore.forgetPrivateKey(walletAccount.receiveAddress.getBytes(), keyCipher)
            backing.deleteAccountContext(walletAccount.id)
            return true
        }
        return false
    }

    companion object {
        const val ID: String = "colored coin module"
        private const val MAX_ACCOUNTS_NUMBER = 1000
    }
}

fun PublicKey.getAllSupportedBtcAddresses(coin: ColuMain, networkParameters: NetworkParameters): Map<AddressType, BtcAddress> {
    val result = mutableMapOf<AddressType, BtcAddress>()
    for (allSupportedAddress in getAllSupportedAddresses(networkParameters)) {
        result[allSupportedAddress.key] = BtcAddress(coin, allSupportedAddress.value)
    }
    return result
}

/**
 * Get active colored coin accounts
 *
 * @return list of accounts
 */
fun WalletManager.getColuAccounts(): List<WalletAccount<*>> = getAccounts().filter { it is PublicColuAccount && it.isVisible && it.isActive }