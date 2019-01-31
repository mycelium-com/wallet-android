package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*

class ColuModule(val networkParameters: NetworkParameters,
                 internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                 val coluApi: ColuApi,
                 val backing: WalletBacking<ColuAccountContext, ColuTransaction>,
                 val listener: AccountListener,
                 val metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {

    init {
        if (networkParameters.isProdnet) {
            assetsList.addAll(listOf(MASSCoin, MTCoin, RMCCoin))
        } else {
            assetsList.addAll(listOf(MASSCoinTest, MTCoinTest, RMCCoinTest))
        }
    }

    override fun getId(): String = "colored coin module"

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        for (context in contexts) {
            try {
                val addresses = context.publicKey?.getAllSupportedBtcAddresses(context.coinType, networkParameters)
                        ?: context.address
                val accountKey = getPrivateKey(addresses)
                val account = if (accountKey == null) {
                    ColuPubOnlyAccount(context, context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id), backing
                            , listener)
                } else {
                    ColuAccount(context, accountKey, context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id), backing
                            , listener)
                }
                result[account.id] = account
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return result
    }

    private fun getPrivateKey(addresses: Map<AddressType, BtcAddress>?): InMemoryPrivateKey? {
        var result: InMemoryPrivateKey? = null
        addresses?.let {
            for (address in it.values) {
                val privateKey = publicPrivateKeyStore.getPrivateKey(address, AesKeyCipher.defaultKeyCipher())
                if (privateKey != null) {
                    result = privateKey
                }
            }
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
                val context = ColuAccountContext(id, type, config.privateKey.publicKey, null
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuAccount(context, config.privateKey, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
                publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, config.privateKey, config.cipher)
            }
        } else if (config is PublicColuConfig) {
            val address = config.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
            val coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type, config.publicKey, null
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuPubOnlyAccount(context, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
            }
        } else if (config is AddressColuConfig) {
            val coinType = coluMain(config.address.address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, config.address.getBytes())
                val context = ColuAccountContext(id, type, null, mapOf(config.address.type to config.address)
                        , false, 0)
                backing.createAccountContext(context)
                result = ColuPubOnlyAccount(context, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
            }
        }
        result?.let {
            val baseName = DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault()).format(Date())
            it.label = createLabel(baseName, it.id)
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
        if (walletAccount is ColuPubOnlyAccount) {
            publicPrivateKeyStore.forgetPrivateKey(walletAccount.receiveAddress.getBytes(), keyCipher)
            backing.deleteAccountContext(walletAccount.id)
            return true
        }
        return false
    }
}

fun PublicKey.getAllSupportedBtcAddresses(coin: ColuMain, networkParameters: NetworkParameters): Map<AddressType, BtcAddress> {
    val result = mutableMapOf<AddressType, BtcAddress>()
    for (allSupportedAddress in getAllSupportedAddresses(networkParameters)) {
        result[allSupportedAddress.key] = BtcLegacyAddress(coin, allSupportedAddress.value.allAddressBytes)
    }
    return result
}

/**
 * Get active colored coin accounts
 *
 * @return list of accounts
 */
fun WalletManager.getColuAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is ColuPubOnlyAccount && it.isVisible && it.isActive }