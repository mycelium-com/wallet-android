package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class ColuModule(val networkParameters: NetworkParameters
                 , val netParams: org.bitcoinj.core.NetworkParameters
                 , internal val publicPrivateKeyStore: PublicPrivateKeyStore
                 , val coluApi: ColuApi
                 , val backing: WalletBacking<ColuAccountContext, ColuTransaction>
                 , val listener: AccountListener) : WalletModule {

    override fun getId(): String = "colored coin module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        for (context in contexts) {
            val accountKey = publicPrivateKeyStore.getPrivateKey(Address(context.address.allAddressBytes), AesKeyCipher.defaultKeyCipher())
            val account = if (accountKey == null) {
                ColuPubOnlyAccount(context
                        , PublicKey(context.address.allAddressBytes)
                        , context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id)
                        , listener)
            } else {
                ColuAccount(context
                        , accountKey, context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id)
                        , listener)
            }
            account.synchronize(SyncMode.NORMAL)
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
                        , BtcAddress(cfg.coinType, cfg.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)?.allAddressBytes)
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuAccount(context, cfg.privateKey, cfg.coinType, networkParameters, netParams
                        , coluApi, backing.getAccountBacking(id), listener)
                publicPrivateKeyStore.setPrivateKey(cfg.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)
                        , cfg.privateKey, cfg.cipher)
            }
            "colu_public" -> {
                val cfg = config as PublicColuConfig
                val id = ColuUtils.getGuidForAsset(cfg.coinType, cfg.publicKey.publicKeyBytes)
                val context = ColuAccountContext(id, cfg.coinType
                        , BtcAddress(cfg.coinType, cfg.publicKey.publicKeyBytes)
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuPubOnlyAccount(context, cfg.publicKey, cfg.coinType, networkParameters
                        , netParams, coluApi, backing.getAccountBacking(id), listener)
            }
        }
        result?.synchronize(SyncMode.NORMAL)
        return result
    }

    override fun canCreateAccount(config: Config): Boolean = config.getType() == "colu_private"
            || config.getType() == "colu_public"

    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        backing.deleteAccountContext(walletAccount.id)
        return true
    }
}