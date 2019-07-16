package com.mycelium.wapi.wallet.eth

import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.AccountContext
import com.mycelium.wapi.wallet.genericdb.AccountContextDAO
import com.mycelium.wapi.wallet.genericdb.GenericWalletBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.masterseed.MasterSeedManager
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Bip32ECKeyPair
import org.web3j.crypto.Bip32ECKeyPair.HARDENED_BIT
import org.web3j.crypto.Credentials
import java.util.*
import kotlin.collections.HashMap


class EtheriumModule(
        private val secureStore: SecureKeyValueStore,
//        private val backing: GenericWalletBacking<AccountContext>,
        metaDataStorage: IMetaDataStorage,
        val accountContextsDao: AccountContextDAO
) : GenericModule(metaDataStorage), WalletModule {
    var settings: EthereumSettings = EthereumSettings()
    val password = ""
    lateinit var credentials: Credentials
    private val accounts = mutableMapOf<UUID, EthAccount>()
    override val id = ID

    init {
        assetsList.add(EthTest.get())
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    override fun setCurrencySettings(currencySettings: CurrencySettings) {
        this.settings = currencySettings as EthereumSettings
    }

    override fun getAccounts(): List<WalletAccount<*>> = accounts.values.toList()

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
        accountContextsDao.getContextsForCurrency(EthTest.get())
                .map { it.uuid to it }
                .collectInto(HashMap(), { map: HashMap<UUID, WalletAccount<*>>, it ->
                    map[it.first] = ethAccount()
                }).blockingGet()


    override fun canCreateAccount(config: Config) = config is EtheriumAccountConfig

    override fun createAccount(config: Config): WalletAccount<*> {
        return ethAccount()
    }

    private fun ethAccount(): EthAccount {
        val seed = MasterSeedManager.getMasterSeed(secureStore, AesKeyCipher.defaultKeyCipher())

        val masterKeypair = Bip32ECKeyPair.generateKeyPair(seed.bip32Seed)

        // m/44'/60'/0'/0
        val path = intArrayOf(44 or HARDENED_BIT, 60 or HARDENED_BIT, 0 or HARDENED_BIT, 0, 0)
        val bip44Keypair = Bip32ECKeyPair.deriveKeyPair(masterKeypair, path)
        credentials = Credentials.create(bip44Keypair)

        val ethAccount = EthAccount(credentials)
        accounts[ethAccount.id] = ethAccount
        return ethAccount
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return false
    }

    companion object {
        const val ID: String = "Etherium"
    }
}