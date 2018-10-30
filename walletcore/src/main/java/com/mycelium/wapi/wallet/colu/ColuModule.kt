package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.colu.coins.ColuMain
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
            val accountKey = publicPrivateKeyStore.getPrivateKey(Address(context.address.getBytes()), AesKeyCipher.defaultKeyCipher())
            val account = if (accountKey == null) {
                ColuPubOnlyAccount(context
                        , PublicKey(context.address.getBytes())
                        , context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id)
                        , backing
                        , listener)
            } else {
                ColuAccount(context
                        , accountKey, context.coinType, networkParameters, netParams, coluApi
                        , backing.getAccountBacking(context.id)
                        , backing
                        , listener)
            }
            result[account.id] = account
        }
        return result
    }

    override fun createAccount(config: Config): WalletAccount<*, *>? {
        var result: WalletAccount<*, *>? = null

        if (config is PrivateColuConfig) {
            val address = config.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
            val coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type
                        , BtcLegacyAddress(coinType, address.allAddressBytes)
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuAccount(context, config.privateKey, type, networkParameters, netParams
                        , coluApi, backing.getAccountBacking(id), backing, listener)
                publicPrivateKeyStore.setPrivateKey(address, config.privateKey, config.cipher)
            }
        } else if (config is PublicColuConfig) {
            val address = config.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
            val coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type
                        , BtcLegacyAddress(config.coinType, address.allAddressBytes)
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuPubOnlyAccount(context, config.publicKey, type, networkParameters
                        , netParams, coluApi, backing.getAccountBacking(id), backing, listener)
            }
        }
        return result
    }

    private fun coluMain(address: Address, coinType: ColuMain?): ColuMain? = if (coinType == null) {
        val types = coluApi.getCoinTypes(address)
        if (types.isEmpty()) null
        else types[0]
    } else {
        coinType
    }

    override fun canCreateAccount(config: Config): Boolean {
        when (config) {
            is PrivateColuConfig -> return true
            is PublicColuConfig -> return true
        }
        return false
    }

    override fun deleteAccount(walletAccount: WalletAccount<*, *>, keyCipher: KeyCipher): Boolean {
        if(walletAccount is ColuPubOnlyAccount) {
            publicPrivateKeyStore.forgetPrivateKey(Address(walletAccount.receiveAddress.getBytes()), keyCipher)
            backing.deleteAccountContext(walletAccount.id)
            return true
        }
        return false
    }
}