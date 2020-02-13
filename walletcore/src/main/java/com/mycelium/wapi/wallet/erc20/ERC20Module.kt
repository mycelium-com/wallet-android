package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.net.HttpEndpoint
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthereumModule
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.genericdb.GenericBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.GenericModule
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import java.util.*

class ERC20Module(
        private val secureStore: SecureKeyValueStore,
        private val backing: GenericBacking<ERC20AccountContext>,
        private val walletDB: WalletDB,
        private val web3jServices: List<HttpEndpoint>,
        networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val accountListener: AccountListener?,
        private val ethereumModule: EthereumModule) : GenericModule(metaDataStorage), WalletModule {
    private val accounts = mutableMapOf<UUID, ERC20Account>()
    override val id = ID
    private val ethCoinType = if (networkParameters.isProdnet) EthMain else EthTest
    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is ERC20Config -> {
                val credentials = Credentials.create(Keys.deserialize(
                        secureStore.getDecryptedValue(config.ethAccount.id.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))
                val token = config.token as ERC20Token
                baseLabel = token.name

                val uuid = UUID(BitUtils.uint64ToLong(HexUtils.toBytes(token.contractAddress.substring(2)), 0),
                        config.ethAccount.id.mostSignificantBits)
                val accountContext = createAccountContext(uuid, config)
                backing.createAccountContext(accountContext)
                val accountBacking = EthAccountBacking(walletDB, accountContext.uuid, ethCoinType, token)
                result = ERC20Account(accountContext, token, config.ethAccount, credentials, accountBacking, accountListener, web3jServices)
            }
            else -> {
                throw NotImplementedError("Unknown config")
            }
        }
        accounts[result.id] = result
        storeLabel(result.id, baseLabel)
        return result
    }

    override fun canCreateAccount(config: Config) = config is ERC20Config

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean {
        return if (walletAccount is ERC20Account) {
            backing.deleteAccountContext(walletAccount.id)
            accounts.remove(walletAccount.id)
            true
        } else {
            false
        }
    }

    override fun getAccounts() = accounts.values.toList()

    override fun getAccountById(id: UUID) = accounts[id]

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> = backing.loadAccountContexts()
            .associateBy({ it.uuid }, { accountFromUUID(it.uuid) })

    private fun createAccountContext(uuid: UUID, config: ERC20Config? = null): ERC20AccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            ERC20AccountContext(accountContextInDB.uuid,
                    accountContextInDB.currency,
                    accountContextInDB.accountName,
                    accountContextInDB.balance,
                    backing::updateAccountContext,
                    accountContextInDB.contractAddress,
                    accountContextInDB.symbol,
                    accountContextInDB.unitExponent,
                    accountContextInDB.ethAccountId,
                    accountContextInDB.archived,
                    accountContextInDB.blockHeight,
                    accountContextInDB.nonce)
        } else {
            val token = config!!.token
            ERC20AccountContext(
                    uuid,
                    ethCoinType,
                    token.name,
                    Balance.getZeroBalance(ethCoinType),
                    backing::updateAccountContext,
                    (token as ERC20Token).contractAddress,
                    token.symbol,
                    token.unitExponent,
                    config.ethAccount.id)
        }
    }

    private fun accountFromUUID(uuid: UUID): ERC20Account {
        val accountContext = createAccountContext(uuid)
        val credentials = Credentials.create(Keys.deserialize(
                secureStore.getDecryptedValue(accountContext.ethAccountId.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())))

        val token = ERC20Token(accountContext.accountName, accountContext.symbol, accountContext.unitExponent, accountContext.contractAddress)
        val accountBacking = EthAccountBacking(walletDB, accountContext.uuid, ethCoinType, token)
        val ethAccount = ethereumModule.getAccountById(accountContext.ethAccountId) as EthAccount
        val account = ERC20Account(accountContext, token, ethAccount, credentials, accountBacking, accountListener, web3jServices)
        accounts[account.id] = account
        return account
    }

    companion object {
        const val ID: String = "ERC20"
    }
}

fun WalletManager.getERC20Accounts() = getAccounts().filter { it is ERC20Account && it.isVisible }
fun WalletManager.getActiveERC20Accounts() = getAccounts().filter { it is ERC20Account && it.isVisible && it.isActive }