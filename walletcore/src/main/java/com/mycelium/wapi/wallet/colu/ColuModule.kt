package com.mycelium.wapi.wallet.colu

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.crypto.PublicKey
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.model.AddressType
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wapi.api.Wapi
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.single.*
import com.mycelium.wapi.wallet.colu.coins.*
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import java.text.DateFormat
import java.util.*

class ColuModule(val networkParameters: NetworkParameters,
                 internal val publicPrivateKeyStore: PublicPrivateKeyStore,
                 val coluApi: ColuApi,
                 val wapi: Wapi,
                 val backing: WalletBacking<ColuAccountContext>,
                 val listener: AccountListener,
                 metaDataStorage: IMetaDataStorage,
                 private val singleAddressModule: BitcoinSingleAddressModule) : WalletModule(metaDataStorage) {

    init {
        if (networkParameters.isProdnet) {
            assetsList.addAll(listOf(MASSCoin, MTCoin, RMCCoin))
        } else {
            assetsList.addAll(listOf(MASSCoinTest, MTCoinTest, RMCCoinTest))
        }
    }

    // Ensures that for all Colu accounts we have corresponding BTC SA accounts, if not - creates them
    // The linked BTC SA account creation should happen in Wallet upgrade situations only
    // In usual cases, BTC SA account will be created together with a Colu account
    override fun afterAccountsLoaded() {
        accounts.values.forEach { it as ColuAccount
            if (it.canSpend()) {
                val saId = SingleAddressAccount.calculateId(it.privateKey!!.publicKey.toAddress(networkParameters, AddressType.P2PKH, true))
                var linked = singleAddressModule.getAccountById(saId) as SingleAddressAccount?
                if (linked == null) {
                    linked = singleAddressModule.createAccount(PrivateSingleConfig(it.privateKey, AesKeyCipher.defaultKeyCipher(), it.label + " Bitcoin", AddressType.P2PKH)) as SingleAddressAccount
                }
                it.linkedAccount = linked
            } else {
                val btcAddress = it.receiveAddress as BtcAddress
                val saId = SingleAddressAccount.calculateId(btcAddress.address)
                var linked = singleAddressModule.getAccountById(saId) as SingleAddressAccount?
                if (linked == null) {
                    linked = singleAddressModule.createAccount(AddressSingleConfig(btcAddress)) as SingleAddressAccount
                    val label = singleAddressModule.createLabel(it.label + " Bitcoin")
                    storeLabel(linked.id, label)
                }
                it.linkedAccount = linked
            }
        }
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private val accounts = mutableMapOf<UUID, WalletAccount<*>>()
    override val id = ID

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> {
        val contexts = backing.loadAccountContexts()
        val result = mutableMapOf<UUID, WalletAccount<*>>()
        for (context in contexts) {
            try {
                val addresses = context.address
                val account = ColuAccount(context, getPrivateKey(addresses), context.coinType, networkParameters, coluApi
                            , backing.getAccountBacking(context.id) as ColuAccountBacking, backing
                            , listener, wapi)
                account.label = readLabel(account.id)
                accounts[account.id] = account
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

    override fun createAccount(config: Config): WalletAccount<*> {
        val coinType: ColuMain
        val result: ColuAccount = when (config) {
            is PrivateColuConfig -> {
                val address = config.privateKey.publicKey.toAddress(networkParameters, AddressType.P2PKH)
                coinType = coluMain(address, config.coinType)!!
                val id = ColuUtils.getGuidForAsset(coinType, address.allAddressBytes)
                val addresses = config.privateKey.publicKey.getAllSupportedBtcAddresses(config.coinType, networkParameters)
                val context = ColuAccountContext(id, coinType, addresses, false, 0)
                backing.createAccountContext(context)
                publicPrivateKeyStore.setPrivateKey(address.allAddressBytes, config.privateKey, config.cipher)
                ColuAccount(context, config.privateKey, coinType, networkParameters
                        , coluApi, backing.getAccountBacking(id) as ColuAccountBacking, backing, listener, wapi)
            }
            is AddressColuConfig -> {
                coinType = coluMain(config.address.address, config.coinType)!!
                val id = ColuUtils.getGuidForAsset(config.coinType, config.address.getBytes())
                val context = ColuAccountContext(id, coinType, mapOf(config.address.type to BtcAddress(coinType, config.address.address))
                        , false, 0)
                backing.createAccountContext(context)
                ColuAccount(context, null, coinType, networkParameters
                        , coluApi, backing.getAccountBacking(id) as ColuAccountBacking, backing, listener, wapi)
            }
            else -> throw IllegalArgumentException("Unexpected config $config.")
        }
        accounts[result.id] = result
        val baseName = createColuAccountLabel(coinType)
        result.label = createLabel(baseName)
        storeLabel(result.id, result.label)

        // Create a linked Colu account
        val saAccount = singleAddressModule.createAccount(when (config) {
            is PrivateColuConfig -> PrivateSingleConfig(config.privateKey, config.cipher, result.label + " Bitcoin", AddressType.P2PKH)
            is AddressColuConfig -> AddressSingleConfig(result.receiveAddress as BtcAddress, result.label + " Bitcoin")
            else -> throw IllegalArgumentException("Unexpected config $config.")
        })
        result.linkedAccount = saAccount as SingleAddressAccount
        return result
    }

    private fun createColuAccountLabel(coinType: ColuMain?): String {
        var proposedLabel: String
        var i = 1

        while (i < MAX_ACCOUNTS_NUMBER) {
            proposedLabel = coinType!!.symbol + " " + Integer.toString(i)

            var foundExistingLabel = false
            for (coluAccount in accounts.values) {
                if (coluAccount.coinType != coinType || coluAccount.label ==
                null) {
                    continue
                }
                val curLabel = coluAccount.label
                if (proposedLabel == curLabel) {
                    foundExistingLabel = true
                    break
                }
            }

            if (!foundExistingLabel) {
                return proposedLabel
            }

            i++
        }
        return DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.getDefault()).format(Date())
    }

    fun getColuAssets(address: BitcoinAddress): List<ColuMain> {
        return coluApi.getCoinTypes(address)
    }

    fun hasColuAssets(address: BitcoinAddress): Boolean {
        return getColuAssets(address).isNotEmpty()
    }

    private fun coluMain(address: BitcoinAddress, coinType: ColuMain?): ColuMain? = if (coinType == null) {
        val types = coluApi.getCoinTypes(address)
        if (types.isEmpty()) null
        else types[0]
    } else {
        coinType
    }

    override fun canCreateAccount(config: Config) = config is PrivateColuConfig || config is AddressColuConfig

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        if (walletAccount is ColuAccount) {
            accounts.remove(walletAccount.id)
            publicPrivateKeyStore.forgetPrivateKey(walletAccount.receiveAddress.getBytes(), keyCipher)
            backing.deleteAccountContext(walletAccount.id)
            return true
        }
        return false
    }

    companion object {
        const val ID: String = "colored coin module"
        private const val MAX_ACCOUNTS_NUMBER = 1000
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
fun WalletManager.getColuAccounts(): List<WalletAccount<*>> = getAccounts().filter { it is ColuAccount && it.isVisible() && it.isActive }