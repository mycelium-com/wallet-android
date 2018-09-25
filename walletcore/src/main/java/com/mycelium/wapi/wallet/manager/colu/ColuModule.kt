package com.mycelium.wapi.wallet.manager.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.colu.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class ColuModule(val networkParameters: NetworkParameters
                 , val netParams: org.bitcoinj.core.NetworkParameters
                 , internal val publicPrivateKeyStore: PublicPrivateKeyStore
                 , val coluApi: ColuApi
                 , val backing: WalletManagerBacking<ColuAccountContext>) : WalletModule {

    override fun getId(): String = "colored coin module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        for (context in contexts) {
            val account: ColuPubOnlyAccount
            val accountKey = publicPrivateKeyStore.getPrivateKey(context.address, AesKeyCipher.defaultKeyCipher())
            if (accountKey == null) {
                account = ColuPubOnlyAccount(context
                        , PublicKey(context.address.allAddressBytes)
                        , context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id))
            } else {
                account = ColuAccount(context
                        , accountKey, context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id))
            }
            result[account.id] = account
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        when (config.getType()) {
            "colu_private" -> {
                val cfg = config as PrivateColuConfig
                val id = ColuUtils.getGuidForAsset(cfg.coinType, cfg.privateKey.publicKey.publicKeyBytes)
                val context = ColuAccountContext(id, cfg.coinType
                        , cfg.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
                        , false, 0)
                result = ColuAccount(context, cfg.privateKey, cfg.coinType, networkParameters, netParams
                        , coluApi, backing.getSingleAddressAccountBacking(id))

            }
            "colu_public" -> {
                val cfg = config as PublicColuConfig
                val id = ColuUtils.getGuidForAsset(cfg.coinType, cfg.publicKey.publicKeyBytes)
                val context = ColuAccountContext(id, cfg.coinType,
                        cfg.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
                        , false, 0)
                result = ColuPubOnlyAccount(context, cfg.publicKey, cfg.coinType, networkParameters
                        , netParams, coluApi, backing.getSingleAddressAccountBacking(id))
            }
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean = config.getType() == "colu_private"
            || config.getType() == "colu_public"

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        backing.deleteSingleAddressAccountContext(walletAccount.id)
        return true
    }
}