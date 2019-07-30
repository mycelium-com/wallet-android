package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.AccountContextImpl
import com.mycelium.wapi.wallet.genericdb.GenericWalletBacking
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


class EtheriumModule(
        private val secureStore: SecureKeyValueStore,
        private val backing: GenericWalletBacking,
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

    override fun canCreateAccount(config: Config) = config is EtheriumAccountConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        val credentials = deriveKey()

        val accountContext = createAccountContext(credentials.ecKeyPair.toUUID())
        backing.createAccountContext(accountContext)

        val ethAccount = EthAccount(credentials, accountContext)
        accounts[ethAccount.id] = ethAccount

        return ethAccount
    }

    private fun ethAccountFromUUID(uuid: UUID): EthAccount {
        val credentials = Credentials.create(Keys.deserialize(secureStore.getPlaintextValue(uuid.toString().toByteArray())))
        val accountContext = createAccountContext(uuid)
        val ethAccount = EthAccount(credentials, accountContext)
        accounts[ethAccount.id] = ethAccount
        return ethAccount
    }

    private fun deriveKey(): Credentials {
        val seed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())

        val masterKeypair = Bip32ECKeyPair.generateKeyPair(seed.bip32Seed)

        // m/44'/60'/0'/0
        val path = intArrayOf(44 or HARDENED_BIT, 60 or HARDENED_BIT, 0 or HARDENED_BIT, 0, 0)
        val bip44Keypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path)
        val credentials = Credentials.create(bip44Keypair)

        secureStore.storePlaintextValue(bip44Keypair.toUUID().toString().toByteArray(),
                Keys.serialize(bip44Keypair))

        return credentials
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return false
    }

    private fun createAccountContext(uuid: UUID): AccountContextImpl {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            AccountContextImpl(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.archived)
        } else {
            AccountContextImpl(
                    uuid,
                    coinType,
                    "abacaba",
                    Balance.getZeroBalance(coinType),
                    backing::updateAccountContext)
        }
    }

    companion object {
        const val ID: String = "Etherium"
    }
}

fun WalletManager.getEthAccounts() = getAccounts().filter { it is EthAccount && it.isVisible }