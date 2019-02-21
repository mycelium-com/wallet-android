package com.mycelium.wapi.wallet.bch.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.SpvBalanceFetcher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.single.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.lang.IllegalArgumentException
import java.util.*


class BitcoinCashSingleAddressModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext, BtcTransaction>,
                                     internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                                     internal val networkParameters: NetworkParameters,
                                     internal val spvBalanceFetcher: SpvBalanceFetcher,
                                     internal var _wapi: Wapi,
                                     internal val metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {
    override fun getId(): String = ID

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        val contexts = backing.loadSingleAddressAccountContexts()
        for (context in contexts) {
            val store = publicPrivateKeyStore
            val accountBacking = checkNotNull(backing.getSingleAddressAccountBacking(context.id))
            val account = SingleAddressBCHAccount(context, store, networkParameters, accountBacking, _wapi, spvBalanceFetcher)
            spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(account.id.toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */ 2)
            result[account.id] = account
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is PrivateSingleConfig
                || config is PublicSingleConfig
    }

    override fun createAccount(config: Config): WalletAccount<*, *> {
        var result: WalletAccount<*, *>? = null

        if (config is PrivateSingleConfig) {
            val cfg = config
            result = createAccount(cfg.privateKey, cfg.cipher)
        } else if (config is PublicSingleConfig) {
            val cfg = config
            result = createAccount(cfg.publicKey)
        }

        val baseLabel = "BCH Single Address"
        if (result != null) {
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
//            accountBacking.createSingleAddressAccountContext(context)
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
//            result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi)
//            context.persist(accountBacking)

            result = SingleAddressBCHAccount(context,
                    publicPrivateKeyStore, networkParameters, accountBacking, _wapi, spvBalanceFetcher)
            spvBalanceFetcher.requestTransactionsFromUnrelatedAccountAsync(result.id.toString(), /* IntentContract.UNRELATED_ACCOUNT_TYPE_SA */2)
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
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    companion object {
        @JvmField
        val ID: String = "BCHSA"
    }
}

/**
 * Get bitcoin single account list
 *
 * @return list of accounts
 */
fun WalletManager.getBCHSingleAddressAccounts() = getAccounts().filter { it is SingleAddressBCHAccount && it.isVisible }
