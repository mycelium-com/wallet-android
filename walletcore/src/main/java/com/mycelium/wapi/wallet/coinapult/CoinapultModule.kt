package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*


class CoinapultModule(val accountKey: InMemoryPrivateKey,
                      val networkParameters: NetworkParameters,
                      val api: CoinapultApi,
                      val backing: WalletBacking<CoinapultAccountContext, CoinapultTransaction>,
                      val listener: AccountListener,
                      val metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {

    override fun getId(): String = "coinapult module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        backing.loadAccountContexts().forEach { context ->
            val id = CoinapultUtils.getGuidForAsset(context.currency, accountKey.publicKey.publicKeyBytes)
            val account = CoinapultAccount(context, accountKey, api, backing.getAccountBacking(id)
                    , networkParameters, context.currency, listener)
            result[account.id] = account
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        if (config is CoinapultConfig) {
            val id = CoinapultUtils.getGuidForAsset(config.currency, accountKey.publicKey.publicKeyBytes)
            api.activate(config.mail)
            val address = api.getAddress(config.currency, null)
            if (address != null) {
                val context = CoinapultAccountContext(id, address, false, config.currency)
                backing.createAccountContext(context)
                result = CoinapultAccount(context, accountKey
                        , api, backing.getAccountBacking(id), networkParameters, config.currency, listener)
            }
        }

        val baseLabel = DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault()).format(Date())
        if (result != null) {
            result.label = createLabel(baseLabel, result.id)
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is CoinapultConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        backing.deleteAccountContext(walletAccount.id)
        return true
    }

    fun setMail(mail: String): Boolean {
        return api.setMail(mail)
    }

    fun verifyMail(link: String, email: String): Boolean {
        return api.verifyMail(link, email)
    }

}