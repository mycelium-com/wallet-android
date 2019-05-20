package com.mycelium.wapi.wallet.coinapult

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAccountBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.util.*


class CoinapultModule(val accountKey: InMemoryPrivateKey,
                      val networkParameters: NetworkParameters,
                      val api: CoinapultApi,
                      val backing: WalletBacking<CoinapultAccountContext>,
                      val listener: AccountListener,
                      val metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {

    private val accounts = mutableMapOf<UUID, CoinapultAccount>()
    override fun getId(): String = ID

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        backing.loadAccountContexts().forEach { context ->
            val id = CoinapultUtils.getGuidForAsset(context.currency, accountKey.publicKey.publicKeyBytes)
            val account = CoinapultAccount(context, accountKey, api, backing.getAccountBacking(id) as BtcAccountBacking
                    , backing, networkParameters, context.currency, listener)
            account.label = readLabel(account.id)
            accounts[account.id] = account
            result[account.id] = account
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        throw IllegalStateException("Account can't be created")
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config is CoinapultConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        backing.deleteAccountContext(walletAccount.id)
        return true
    }

    fun setMail(mail: String): Boolean {
        return api.setMail(mail)
    }

    fun verifyMail(link: String, email: String): Boolean {
        return api.verifyMail(link, email)
    }

    companion object {
        const val ID: String = "coinapult module"
    }
}

/**
 * Get active coinapult accounts
 *
 * @return list of accounts
 */
fun WalletManager.getCoinapultAccounts(): List<WalletAccount<*>> = getAccounts().filter { it is CoinapultAccount && it.isVisible && it.isActive }
