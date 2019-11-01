package com.mycelium.wapi.wallet.eth

import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.genericdb.GenericBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.*


class EthereumModule(
        private val secureStore: SecureKeyValueStore,
        private val backing: GenericBacking<EthAccountContext>,
        private val walletDB: WalletDB,
        metaDataStorage: IMetaDataStorage) : GenericModule(metaDataStorage), WalletModule {
    var settings: EthereumSettings = EthereumSettings()
    val password = ""
    private val coinType = EthTest

    private val accounts = mutableMapOf<UUID, EthAccount>()
    override val id = ID

    init {
        assetsList.add(EthTest)
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as EthereumSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts()
                    .associateBy({ it.uuid }, { ethAccountFromUUID(it.uuid) })

    override fun canCreateAccount(config: Config) = (config is EthereumMasterseedConfig && accounts.isEmpty())
            || config is EthAddressConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        when (config) {
            is EthereumMasterseedConfig -> {
                val credentials = deriveKey()

                val accountContext = createAccountContext(credentials.ecKeyPair.toUUID())
                backing.createAccountContext(accountContext)

                val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
                val ethAccount = EthAccount(accountContext, credentials, ethAccountBacking)
                accounts[ethAccount.id] = ethAccount

                return ethAccount
            }
            is EthAddressConfig -> {
                val uuid = UUID.nameUUIDFromBytes(config.address.getBytes())
                secureStore.storePlaintextValue(uuid.toString().toByteArray(),
                        config.address.addressString.toByteArray())
                val accountContext = createAccountContext(uuid)
                backing.createAccountContext(accountContext)

                val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
                val ethAccount = EthAccount(accountContext, address = config.address, backing = ethAccountBacking)
                accounts[ethAccount.id] = ethAccount
                return ethAccount
            }
            else -> {
                throw NotImplementedError("Unknown config")
            }
        }
    }

    private fun ethAccountFromUUID(uuid: UUID): EthAccount {
        return if (secureStore.hasCiphertextValue(uuid.toString().toByteArray())) {
            val credentials = Credentials.create(Keys.deserialize(
                    secureStore.getDecryptedValue(uuid.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))
            val accountContext = createAccountContext(uuid)
            val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = EthAccount(accountContext, credentials, ethAccountBacking)
            accounts[ethAccount.id] = ethAccount
            ethAccount
        } else {
            val accountContext = createAccountContext(uuid)
            val ethAddress = EthAddress(coinType, secureStore.getPlaintextValue(uuid.toString().toByteArray()).toString())
            val ethAccountBacking = EthAccountBacking(walletDB, accountContext.uuid, coinType)
            val ethAccount = EthAccount(accountContext, address = ethAddress, backing = ethAccountBacking)
            accounts[ethAccount.id] = ethAccount
            ethAccount
        }
    }

    private fun deriveKey(): Credentials {
        val seed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())

        val masterKeypair = Bip32ECKeyPair.generateKeyPair(seed.bip32Seed)

        // m/44'/60'/0'/0
        val path = intArrayOf(44 or HARDENED_BIT, 60 or HARDENED_BIT, 0 or HARDENED_BIT, 0, 0)
        val bip44Keypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path)
        val credentials = Credentials.create(bip44Keypair)

        secureStore.encryptAndStoreValue(bip44Keypair.toUUID().toString().toByteArray(),
                Keys.serialize(bip44Keypair), AesKeyCipher.defaultKeyCipher())

        return credentials
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is EthAccount) {
            if (secureStore.hasCiphertextValue(walletAccount.id.toString().toByteArray())) {
                secureStore.deleteEncryptedValue(walletAccount.id.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())
            } else {
                secureStore.deletePlaintextValue(walletAccount.id.toString().toByteArray())
            }
            backing.deleteAccountContext(walletAccount.id)

            true
        } else {
            false
        }
    }

    private fun createAccountContext(uuid: UUID): EthAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            EthAccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.archived,
                    accountContextInDB.nonce)
        } else {
            EthAccountContext(
                    uuid,
                    coinType,
                    "Ethereum",
                    Balance.getZeroBalance(coinType),
                    backing::updateAccountContext)
        }
    }

    companion object {
        const val ID: String = "Ethereum"
    }
}

fun WalletManager.getEthAccounts() = getAccounts().filter { it is EthAccount && it.isVisible }