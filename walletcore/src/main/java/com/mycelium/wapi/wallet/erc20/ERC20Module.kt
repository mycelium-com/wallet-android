package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.EthBlockchainService
import com.mycelium.wapi.wallet.eth.EthereumModule
import com.mycelium.wapi.wallet.eth.coins.EthMain
import com.mycelium.wapi.wallet.eth.coins.EthTest
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import org.web3j.crypto.Credentials
import org.web3j.crypto.Keys
import org.web3j.tx.ChainId
import java.util.*

class ERC20Module(
        private val secureStore: SecureKeyValueStore,
        private val backing: Backing<ERC20AccountContext>,
        private val walletDB: WalletDB,
        private val blockchainService: EthBlockchainService,
        private val networkParameters: NetworkParameters,
        metaDataStorage: IMetaDataStorage,
        private val accountListener: AccountListener?,
        private val ethereumModule: EthereumModule) : WalletModule(metaDataStorage) {
    private val accounts = mutableMapOf<UUID, ERC20Account>()
    override val id = ID
    private val ethCoinType = if (networkParameters.isProdnet) EthMain else EthTest
    private val chainId = if (networkParameters.isProdnet) ChainId.MAINNET else CHAIN_ID_GOERLI

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: WalletAccount<*>
        val baseLabel: String
        when (config) {
            is ERC20Config -> {
                val credentials = if(config.ethAccount.canSpend())
                    Credentials.create(Keys.deserialize(secureStore.getDecryptedValue(
                        config.ethAccount.id.toString().toByteArray(), AesKeyCipher.defaultKeyCipher()))) else null
                val token = config.token as ERC20Token
                baseLabel = token.name

                val uuid = UUID(BitUtils.uint64ToLong(HexUtils.toBytes(token.contractAddress.substring(2)), 0),
                        config.ethAccount.id.mostSignificantBits)
                val accountContext = createAccountContext(uuid, config)
                backing.createAccountContext(accountContext)
                val accountBacking = EthAccountBacking(walletDB, accountContext.uuid, ethCoinType, token)
                result = ERC20Account(chainId, accountContext, token, config.ethAccount, credentials, accountBacking,
                        accountListener, blockchainService, if(config.ethAccount.canSpend()) null else config.ethAccount.receivingAddress)
            }
            else -> throw NotImplementedError("Unknown config")
        }
        accounts[result.id] = result
        result.label = config.token.name
        storeLabel(result.id, baseLabel)
        return result
    }

    override fun canCreateAccount(config: Config) = config is ERC20Config

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean =
            if (walletAccount is ERC20Account) {
                backing.deleteAccountContext(walletAccount.id)
                walletAccount.clearBacking()
                accounts.remove(walletAccount.id)
                true
            } else {
                false
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
        val token = ERC20Token(accountContext.accountName + if (networkParameters.isProdnet) "" else " test",
                accountContext.symbol, accountContext.unitExponent, accountContext.contractAddress)
        val accountBacking = EthAccountBacking(walletDB, accountContext.uuid, ethCoinType, token)
        val ethAccount = ethereumModule.getAccountById(accountContext.ethAccountId) as EthAccount
        val account = if (secureStore.hasCiphertextValue(uuid.toString().toByteArray())) {
            val credentials = Credentials.create(
                Keys.deserialize(
                    secureStore.getDecryptedValue(accountContext.ethAccountId.toString().toByteArray(), AesKeyCipher.defaultKeyCipher())
                )
            )
            ERC20Account(
                chainId, accountContext, token, ethAccount, credentials, accountBacking,
                accountListener, blockchainService
            )
        } else {
            val ethAddress = EthAddress(token, String(secureStore.getPlaintextValue(ethAccount.id.toString().toByteArray())))
            ERC20Account(chainId, accountContext, token, ethAccount, null, accountBacking,
                accountListener, blockchainService, ethAddress)
        }
        accounts[account.id] = account
        return account
    }

    companion object {
        private const val CHAIN_ID_GOERLI: Byte = 5
        const val ID: String = "ERC20"
    }
}

fun WalletManager.getERC20Accounts() = getAccounts().filter { it is ERC20Account && it.isVisible() }.map { it as ERC20Account }
fun WalletManager.getActiveERC20Accounts() = getAccounts().filter { it is ERC20Account && it.isVisible() && it.isActive }.map { it as ERC20Account }