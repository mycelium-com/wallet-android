package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.CurrencySettings
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Bip44WalletUtils
import java.util.*
import java.io.File


class EtheriumModule(metaDataStorage: IMetaDataStorage, walletDir: String) : GenericModule(metaDataStorage), WalletModule {
    var settings: EthereumSettings = EthereumSettings()
    val password = ""
    val dir = File(walletDir)
    val credentials = Bip44WalletUtils.loadBip44Credentials("", "oil oil oil oil oil oil oil oil oil oil oil oil", false)
    //var walletFile = WalletUtils.generateBip39Wallet(password, dir)

    init {
        assetsList.add(EthTest.get())
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private val accounts = mutableMapOf<UUID, EthAccount>()
    override fun getId(): String = ID

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as EthereumSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
//        LoadingProgressTracker.subscribe(loadingProgressUpdater!!)
//        val result = mutableMapOf<UUID, WalletAccount<*>>()
//        val contexts = backing.loadSingleAddressAccountContexts()
//        var counter = 1
//        for (context in contexts) {
//            LoadingProgressTracker.setPercent(counter * 100 / contexts.size)
//            // The only way to know if we are migrating now
//            if (loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMHD || loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMSA) {
//                LoadingProgressTracker.setStatus(LoadingProgressStatus.MigratingNOfMSA(Integer.toString(counter++), Integer.toString(contexts.size)))
//            } else {
//                LoadingProgressTracker.setStatus(LoadingProgressStatus.LoadingNOfMSA(Integer.toString(counter++), Integer.toString(contexts.size)))
//            }
//            val store = publicPrivateKeyStore
//            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
//            val account = SingleAddressAccount(context, store, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
//            account.setEventHandler(eventHandler)
//            account.label = readLabel(account.id)
//            accounts[account.id] = account
//            result[account.id] = account
//        }
//        return result
        return emptyMap()
    }

    override fun canCreateAccount(config: Config) =  config is EtheriumAccountConfig

    override fun createAccount(config: Config): WalletAccount<*> {
//        var result: WalletAccount<*>? = null
//        var baseLabel = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
//        var configLabel = (config as LabeledConfig).label
//
//        if (config is PublicSingleConfig) {
//            result = createAccount(config.publicKey, settings.defaultAddressType)
//        } else if (config is PrivateSingleConfig) {
//            val addressType = if (config.addressType != null) config.addressType else settings.defaultAddressType
//            result = createAccount(config.privateKey, config.cipher, addressType)
//        } else if (config is AddressSingleConfig) {
//            val id = SingleAddressAccount.calculateId(config.address.address)
//            backing.beginTransaction()
//            try {
//                val context = SingleAddressAccountContext(id, mapOf(config.address.address.type to config.address.address), false, 0, config.address.address.type)
//                backing.createSingleAddressAccountContext(context)
//                val accountBacking = backing.getSingleAddressAccountBacking(context.id)
//                result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi, settings.changeAddressModeReference)
//                context.persist(accountBacking)
//                backing.setTransactionSuccessful()
//            } finally {
//                backing.endTransaction()
//            }
//        }
//
//        if (result != null) {
//            accounts[result.id] = result as SingleAddressAccount
//            result.setEventHandler(eventHandler)
//            if (configLabel.isNotEmpty()) {
//                result.label = storeLabel(result.id, configLabel)
//            } else {
//                result.label = createLabel(baseLabel, result.id)
//            }
//        } else {
//            throw IllegalStateException("Account can't be created")
//        }
//        return result
        val ethAccount = EthAccount(credentials)
        accounts[ethAccount.id] = ethAccount
        return ethAccount
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
//        if (walletAccount is SingleAddressAccount) {
//            walletAccount.forgetPrivateKey(keyCipher)
//            accounts[walletAccount.id]?.markToRemove()
//            backing.deleteSingleAddressAccountContext(walletAccount.id)
//            return true
//        }
        return false
    }

    companion object {
        const val ID: String = "Etherium"
    }
}