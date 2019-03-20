package com.mycelium.wapi.wallet.btc.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.LoadingProgressTracker
import com.mycelium.wapi.wallet.LoadingProgressUpdater
import com.mycelium.wapi.wallet.LoadingProgressStatus
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.Reference
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest
import com.mycelium.wapi.wallet.colu.ColuModule
import com.mycelium.wapi.wallet.colu.PrivateColuConfig
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*


class BitcoinSingleAddressModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>,
                                 internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                                 internal val networkParameters: NetworkParameters,
                                 internal var _wapi: Wapi,
                                 internal var walletManager: WalletManager,
                                 internal val metaDataStorage: IMetaDataStorage,
                                 internal val loadingProgressUpdater: LoadingProgressUpdater?) : GenericModule(metaDataStorage), WalletModule {

    init {
        assetsList.add(if (networkParameters.isProdnet) BitcoinMain.get() else BitcoinTest.get())
    }

    private val accounts = mutableMapOf<UUID, SingleAddressAccount>()
    override fun getId(): String = ID

    override fun getAccounts(): List<WalletAccount<*, *>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        LoadingProgressTracker.subscribe(loadingProgressUpdater!!)
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        val contexts = backing.loadSingleAddressAccountContexts()
        var counter = 1
        for (context in contexts) {
            LoadingProgressTracker.setPercent(counter * 100 / contexts.size)
            // The only way to know if we are migrating now
            if (loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMHD || loadingProgressUpdater.status is LoadingProgressStatus.MigratingNOfMSA) {
                LoadingProgressTracker.setStatus(LoadingProgressStatus.MigratingNOfMSA(Integer.toString(counter++), Integer.toString(contexts.size)))
            } else {
                LoadingProgressTracker.setStatus(LoadingProgressStatus.LoadingNOfMSA(Integer.toString(counter++), Integer.toString(contexts.size)))
            }
            val store = publicPrivateKeyStore
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
            val account = SingleAddressAccount(context, store, networkParameters, accountBacking, _wapi, Reference(ChangeAddressMode.P2WPKH))
            result[account.id] = account
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is PublicSingleConfig
                || config is PrivateSingleConfig
                || config is AddressSingleConfig
    }

    override fun createAccount(config: Config): WalletAccount<*, *> {
        var result: WalletAccount<*, *>? = null
        var baseLabel = DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault()).format(Date())

        if (config is PublicSingleConfig) {
            result = createAccount(config.publicKey)
        } else if (config is PrivateSingleConfig) {
            result = createAccount(config.privateKey, config.cipher)
            if (config is PrivateColuConfig) {
                walletManager.getModuleById(ColuModule.ID)?.let {
                    val coluOfType = it.getAccounts().filter { it.coinType == config.coinType }.count()
                    baseLabel = if (config.coinType != null) config.coinType.symbol + " " +
                            coluOfType + " Bitcoin" else baseLabel
                }
            }
        } else if (config is AddressSingleConfig) {
            val id = SingleAddressAccount.calculateId(config.address.address)
            backing.beginTransaction()
            try {
                val context = SingleAddressAccountContext(id, mapOf(config.address.address.type to config.address.address), false, 0)
                backing.createSingleAddressAccountContext(context)
                val accountBacking = backing.getSingleAddressAccountBacking(context.id)
                result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi, Reference(ChangeAddressMode.P2WPKH))
                context.persist(accountBacking)
                backing.setTransactionSuccessful()
            } finally {
                backing.endTransaction()
            }
        }

        if (result != null) {
            accounts[result.id] = result as SingleAddressAccount
            result.label = createLabel(baseLabel, result.id)
        } else {
            throw IllegalStateException("Account can't be created")
        }
        return result
    }

    private fun createAccount(publicKey: PublicKey): WalletAccount<*, *>? {
        val result: WalletAccount<*, *>?
        val id = SingleAddressAccount.calculateId(publicKey.toAddress(networkParameters, AddressType.P2SH_P2WPKH))
        backing.beginTransaction()
        try {
            val context = SingleAddressAccountContext(id, publicKey.getAllSupportedAddresses(networkParameters), false, 0)
            backing.createSingleAddressAccountContext(context)
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
            result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi, Reference(ChangeAddressMode.P2WPKH))
            context.persist(accountBacking)
            backing.setTransactionSuccessful()
        } finally {
            backing.endTransaction()
        }
        return result
    }

    private fun createAccount(privateKey: InMemoryPrivateKey, cipher: KeyCipher): WalletAccount<*, *>? {
        val publicKey = privateKey.publicKey
        for (address in publicKey.getAllSupportedAddresses(networkParameters).values) {
            publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, privateKey, cipher)
        }
        return createAccount(publicKey)
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        if (walletAccount is SingleAddressAccount) {
            publicPrivateKeyStore.forgetPrivateKey(walletAccount.address.allAddressBytes, keyCipher);
            backing.deleteSingleAddressAccountContext(walletAccount.id)
            return true
        }
        return false
    }

    companion object {
        @JvmField
        val ID: String = "BitcoinSA"
    }
}