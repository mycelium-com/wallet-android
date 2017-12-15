package com.mycelium.wallet

import com.google.common.collect.ImmutableMap
import com.mycelium.wallet.coinapult.CoinapultManager
import com.mycelium.wallet.colu.ColuManager
import com.mycelium.wapi.wallet.AccountProvider
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import java.util.*
import kotlin.collections.HashMap

/**
 * Created by Nelson on 15/12/2017.
 */
object AccountManager : AccountProvider {
    init {
        val mbwManager = MbwManager
                .getInstance(WalletApplication.getInstance())
        val coinapultManager : CoinapultManager = mbwManager.coinapultManager
        val coluManager : ColuManager = mbwManager.coluManager
        val accounts: HashMap<UUID, WalletAccount>  = hashMapOf()
        val walletManager : WalletManager = mbwManager.getWalletManager(false)
        accounts.putAll(walletManager.activeAccounts)
    }

    override fun getAccounts(): ImmutableMap<UUID, WalletAccount> =
            ImmutableMap.copyOf<UUID, WalletAccount>(accounts)

    fun getBTCSingleAddressAccounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.BTCSINGLEADDRESS
        })
    }

    fun getBTCBip44Accounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.BTCBIP44
        })
    }

    fun getBCHSingleAddressAccounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.BCHSINGLEADDRESS
        })
    }

    fun getBCHBip44Accounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.BCHBIP44
        })
    }

    fun getCoinapultAccounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.COINAPULT
        })
    }

    fun getColuAccounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.COLU
        })
    }

    fun getDashAccounts(): ImmutableMap<UUID, WalletAccount> {
        return ImmutableMap.copyOf<UUID, WalletAccount>(accounts.filter {
            it.value.type == WalletAccount.Type.DASH
        })
    }

    override fun getAccount(uuid: UUID?): WalletAccount? = accounts.get(uuid)

    override fun hasAccount(uuid: UUID?): Boolean = accounts.containsKey(uuid)


    private fun HashMap<UUID, WalletAccount>.putAll(from: List<WalletAccount>) {
        val result : MutableMap<UUID, WalletAccount> = mutableMapOf()
        for(walletAccount in from) {
            result.put(walletAccount.id, walletAccount)
        }
        this.putAll(result)
    }
}

