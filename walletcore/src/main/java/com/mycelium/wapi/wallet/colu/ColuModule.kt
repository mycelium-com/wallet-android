package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.Address
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.single.PublicPrivateKeyStore
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.lang.IllegalArgumentException
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

    private val accounts = mutableMapOf<UUID, WalletAccount<*, *>>()
    override fun getId(): String = ID

    override fun getAccounts(): List<WalletAccount<*, *>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*, *>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*, *>>()
        for (context in contexts) {
            try {
                val addresses = context.publicKey?.getAllSupportedBtcAddresses(context.coinType, networkParameters)
                        ?: context.address
                val accountKey = getPrivateKey(addresses)
                val account = if (accountKey == null) {
                    PublicColuAccount(context, context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id), backing
                            , listener)
                } else {
                    PrivateColuAccount(context, accountKey, context.coinType, networkParameters, coluApi
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

    override fun createAccount(config: Config): WalletAccount<*, *> {
        var result: WalletAccount<*, *>? = null
        var baseName = DateFormat.getDateInstance(java.text.DateFormat.MEDIUM, Locale.getDefault()).format(Date())
        var num = 1

        if (config is PrivateColuConfig) {
            val address = config.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
            val coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type, config.privateKey.publicKey, null
                        , false, 0)
                backing.createAccountContext(context)
                result = PrivateColuAccount(context, config.privateKey, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
                publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, config.privateKey, config.cipher)
            }
            num = if (coluNumOfType(config.coinType) == 0) num else num + 1
            baseName = if (config.coinType != null)
                config.coinType.symbol + " " + num else baseName
        } else if (config is PublicColuConfig) {
            val address = config.publicKey.toAddress(networkParameters, AddressType.P2PKH)!!
            val coinType = coluMain(address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, address.allAddressBytes)
                val context = ColuAccountContext(id, type, config.publicKey, null
                        , false, 0)
                backing.createAccountContext(context)
                result = PublicColuAccount(context, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
            }
            num = if (coluNumOfType(config.coinType) == 0) num else num + 1
            baseName = if (config.coinType != null)
                config.coinType.symbol + " " + num else baseName
        } else if (config is AddressColuConfig) {
            val coinType = coluMain(config.address.address, config.coinType)
            coinType?.let { type ->
                val id = ColuUtils.getGuidForAsset(config.coinType, config.address.getBytes())
                val context = ColuAccountContext(id, type, null, mapOf(config.address.type to config.address)
                        , false, 0)
                backing.createAccountContext(context)
                result = PublicColuAccount(context, type, networkParameters
                        , coluApi, backing.getAccountBacking(id), backing, listener)
            }
            num = if (coluNumOfType(config.coinType) == 0) num else num + 1
            baseName = if (config.coinType != null)
                config.coinType.symbol + " " + num else baseName
        }

        result?.let {
            accounts[it.id] = result!!
            it.label = createLabel(baseName, it.id)
        } ?: run {
            throw IllegalStateException("Account can't be created")
        }

        return result!!
    }

    private fun coluNumOfType(coinType: ColuMain?) = accounts.values.filter { it.coinType == coinType }.count()

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
        if (walletAccount is PublicColuAccount) {
            publicPrivateKeyStore.forgetPrivateKey(walletAccount.receiveAddress.getBytes(), keyCipher)
            backing.deleteAccountContext(walletAccount.id)
            return true
        }
        return false
    }

    companion object {
        @JvmField
        val ID: String = "colored coin module"
    }
}

fun PublicKey.getAllSupportedBtcAddresses(coin: ColuMain, networkParameters: NetworkParameters): Map<AddressType, BtcAddress> {
    val result = mutableMapOf<AddressType, BtcAddress>()
    for (allSupportedAddress in getAllSupportedAddresses(networkParameters)) {
        result[allSupportedAddress.key] = BtcAddress(coin, allSupportedAddress.value)
    }
    return result
}

/**
 * Get active colored coin accounts
 *
 * @return list of accounts
 */
fun WalletManager.getColuAccounts(): List<WalletAccount<*, *>> = getAccounts().filter { it is PublicColuAccount && it.isVisible && it.isActive }