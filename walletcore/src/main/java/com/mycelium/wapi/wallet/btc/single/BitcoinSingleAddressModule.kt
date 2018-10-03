package com.mycelium.wapi.wallet.btc.single

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.Reference
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode
import com.mycelium.wapi.wallet.btc.WalletManagerBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import java.util.*


class BitcoinSingleAddressModule(internal val backing: WalletManagerBacking<SingleAddressAccountContext>
                                 , internal val publicPrivateKeyStore: PublicPrivateKeyStore
                                 , internal val networkParameters: NetworkParameters
                                 , internal var _wapi: Wapi) : WalletModule {

    override fun getId(): String = "Bitcoin Single Address Accounts"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        val contexts = backing.loadSingleAddressAccountContexts()
        for (context in contexts) {
            val store = publicPrivateKeyStore
            val accountBacking = backing.getSingleAddressAccountBacking(context.id)
            val account = SingleAddressAccount(context, store, networkParameters, accountBacking, _wapi, Reference(ChangeAddressMode.P2WPKH))
            result[account.id] = account
        }
        return result
    }

    override fun canCreateAccount(config: Config): Boolean {
        return config.getType() == "public_bitcoin_single"
                || config.getType() == "private_bitcoin_single"
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null
        when (config.getType()) {
            "public_bitcoin_single" -> {
                val cfg = config as PublicSingleConfig
                result = createAccount(cfg.publicKey)
            }
            "private_bitcoin_single" -> {
                val cfg = config as PrivateSingleConfig
                result = createAccount(cfg.privateKey, cfg.cipher)
            }
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
            result = SingleAddressAccount(context, publicPrivateKeyStore, networkParameters, accountBacking, _wapi,  Reference(ChangeAddressMode.P2WPKH))
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
            publicPrivateKeyStore.setPrivateKey(address, privateKey, cipher)
        }
        return createAccount(publicKey)
    }


    override fun deleteAccount(walletAccount: WalletAccount<*, *>): Boolean {
        backing.deleteSingleAddressAccountContext(walletAccount.id)
        return true
    }


}