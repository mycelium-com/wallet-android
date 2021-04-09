package com.mycelium.wapi.wallet.btc.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.*
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*


class BitcoinSingleAddressModule(internal val backing: BtcWalletManagerBacking<SingleAddressAccountContext>,
                                 internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                                 internal val networkParameters: NetworkParameters,
                                 internal var _wapi: Wapi,
                                 internal var settings: BTCSettings,
                                 internal var walletManager: WalletManager,
                                 metaDataStorage: IMetaDataStorage,
                                 internal val loadingProgressUpdater: LoadingProgressUpdater?,
                                 internal val eventHandler: AbstractBtcAccount.EventHandler?) : WalletModule(metaDataStorage) {

    init {
        assetsList.add(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private val accounts = mutableMapOf<UUID, SingleAddressAccount>()
    override val id = ID

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as BTCSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        LoadingProgressTracker.subscribe(loadingProgressUpdater!!)
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        val contexts = backing.loadSingleAddressAccountContexts()
        var counter = 1
        for (context in contexts) {
            LoadingProgressTracker.setPercent(counter * 100 / contexts.size)
            // The only way to know if we are migrating now
            if (loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMHD || loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMSA) {
                LoadingProgressTracker.setStatus(LoadingProgressStatus.MigratingNOfMSA(Integer.toString(counter++), contexts.size.toString()))
            } else {
                LoadingProgressTracker.setStatus(LoadingProgressStatus.LoadingNOfMSA(Integer.toString(counter++), contexts.size.toString()))
            }
            val store = publicPrivateKeyStore
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
            val account = SingleAddressAccount(context, store, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
            account.setEventHandler(eventHandler)
            account.label = readLabel(account.id)
            accounts[account.id] = account
            result[account.id] = account
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is PublicSingleConfig
                || config is PrivateSingleConfig
                || config is AddressSingleConfig
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        var result: WalletAccount<*>? = null
        val baseLabel = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
        val configLabel = (config as LabeledConfig).label

        when (config) {
            is PublicSingleConfig -> result = createAccount(config.publicKey, settings.defaultAddressType)
            is PrivateSingleConfig -> {
                val addressType = config.addressType ?: settings.defaultAddressType
                result = createAccount(config.privateKey, config.cipher, addressType)
            }
            is AddressSingleConfig -> {
                val id = SingleAddressAccount.calculateId(config.address.address)
                backing.beginTransaction()
                try {
                    val context = SingleAddressAccountContext(id, mapOf(config.address.address.type to config.address.address), false, 0, config.address.address.type)
                    backing.createSingleAddressAccountContext(context)
                    val accountBacking = backing.getSingleAddressAccountBacking(context.id)
                    result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
                    context.persist(accountBacking)
                    backing.setTransactionSuccessful()
                } finally {
                    backing.endTransaction()
                }
            }
        }

        if (result != null) {
            accounts[result.id] = result as SingleAddressAccount
            result.setEventHandler(eventHandler)
            if (configLabel.isNotEmpty()) {
                result.label = storeLabel(result.id, configLabel)
            } else {
                result.label = createLabel(baseLabel)
                storeLabel(result.id, result.label)
            }
        } else {
            throw IllegalStateException("Account can't be created")
        }
        return result
    }

    private fun createAccount(publicKey: PublicKey,addressType: AddressType): WalletAccount<*>? {
        val result: WalletAccount<*>?
        val id = SingleAddressAccount.calculateId(publicKey.toAddress(networkParameters, AddressType.P2SH_P2WPKH, true))
        backing.beginTransaction()
        try {
            val addressTypeFinal = if (!publicKey.isCompressed) AddressType.P2PKH else addressType
            val context = SingleAddressAccountContext(id, publicKey.getAllSupportedAddresses(networkParameters), false, 0, addressTypeFinal)
            backing.createSingleAddressAccountContext(context)
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
            result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
            context.persist(accountBacking)
            backing.setTransactionSuccessful()
        } finally {
            backing.endTransaction()
        }
        return result
    }

    private fun createAccount(privateKey: InMemoryPrivateKey, cipher: KeyCipher, addressType: AddressType): WalletAccount<*>? {
        val publicKey = privateKey.publicKey
        for (address in publicKey.getAllSupportedAddresses(networkParameters).values) {
            publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, privateKey, cipher)
        }
        return createAccount(publicKey, addressType)
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        if (walletAccount is SingleAddressAccount) {
            walletAccount.forgetPrivateKey(keyCipher)
            accounts[walletAccount.id]?.markToRemove()
            backing.deleteSingleAddressAccountContext(walletAccount.id)
            return true
        }
        return false
    }

    companion object {
        const val ID: String = "BitcoinSA"
    }
}