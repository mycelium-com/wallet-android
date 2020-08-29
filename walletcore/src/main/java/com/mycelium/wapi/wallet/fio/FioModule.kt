package com.mycelium.wapi.wallet.fio

import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import com.mycelium.wapi.wallet.fio.coins.FIOTest
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import fiofoundation.io.fiosdk.FIOSDK
import fiofoundation.io.fiosdk.interfaces.ISerializationProvider
import java.util.*

class FioModule(
        private val serializationProvider : ISerializationProvider,
        private val secureStore: SecureKeyValueStore,
        private val backing: Backing<FioAccountContext>,
        private val walletDB: WalletDB,
        networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val fioKeyManager: FioKeyManager,
        private val accountListener: AccountListener?
) : WalletModule(metaDataStorage) {

    private val coinType = if (networkParameters.isProdnet) FIOMain else FIOTest
    private val accounts = mutableMapOf<UUID, FioAccount>()
    override val id = ID

    init {
        assetsList.add(coinType)
    }

    private fun getFioSdk(accountIndex: Int): FIOSDK {
        val fioPublicKey = fioKeyManager.getFioPublicKey(accountIndex)
        val publicKey = fioKeyManager.formatPubKey(fioPublicKey)
        // in FIO WIF testnet private keys aren't used
        val privateKey = fioKeyManager.getFioPrivateKey(accountIndex).getBase58EncodedPrivateKey(NetworkParameters.productionNetwork)
        return FIOSDK.getInstance(privateKey, publicKey, serializationProvider, coinType.url)
    }

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> =
            backing.loadAccountContexts()
                    .associateBy({ it.uuid }, { accountFromUUID(it.uuid) })

    private fun accountFromUUID(uuid: UUID): WalletAccount<*> {
        val accountContext = createAccountContext(uuid)
        val fioAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
        val account = FioAccount(accountContext, fioAccountBacking, accountListener,
                getFioSdk(accountContext.accountIndex))
        accounts[account.id] = account
        return account
    }

    override fun createAccount(config: Config): WalletAccount<*> {
        val newIndex = getCurrentBip44Index() + 1
        val accountContext = createAccountContext(fioKeyManager.getUUID(newIndex))
        backing.createAccountContext(accountContext)
        val fioAccountBacking = FioAccountBacking(walletDB, accountContext.uuid, coinType)
        val newAccount = FioAccount(accountContext, fioAccountBacking, accountListener, getFioSdk(newIndex))
        newAccount.label = createLabel(accountContext.accountName)
        storeLabel(newAccount.id, newAccount.label)
        accounts[newAccount.id] = newAccount
        return newAccount
    }

    private fun getCurrentBip44Index() = accounts.values
            .filter { it.isDerivedFromInternalMasterseed }.size

    override fun canCreateAccount(config: Config): Boolean {
        return config is FIOConfig
    }

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        accounts.remove(walletAccount.id)
        return true
    }

    override fun getAccounts(): List<WalletAccount<*>> {
        return accounts.values.toList()
    }

    override fun getAccountById(id: UUID): WalletAccount<*>? {
        return accounts[id]
    }

    private fun createAccountContext(uuid: UUID): FioAccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            FioAccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.accountIndex,
                    accountContextInDB.archived,
                    accountContextInDB.blockHeight)
        } else {
            FioAccountContext(
                    uuid,
                    coinType,
                    "FIO ${getCurrentBip44Index() + 1}",
                    Balance.getZeroBalance(coinType),
                    backing::updateAccountContext,
                    getCurrentBip44Index() + 1)
        }
    }

    fun getBip44Path(account: FioAccount): HdKeyPath? =
            HdKeyPath.valueOf("m/44'/235'/${account.accountIndex}'/0/0")

    companion object {
        const val ID: String = "FIO"
    }
}

fun WalletManager.getFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible }
fun WalletManager.getActiveFioAccounts() = getAccounts().filter { it is FioAccount && it.isVisible && it.isActive }