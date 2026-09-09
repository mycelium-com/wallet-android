package com.mycelium.wapi.wallet.trc20

import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.util.BitUtils
import com.mycelium.generated.wallet.database.WalletDB
import com.mycelium.wapi.wallet.AccountListener
import com.mycelium.wapi.wallet.KeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.genericdb.Backing
import com.mycelium.wapi.wallet.genericdb.TronAccountBacking
import com.mycelium.wapi.wallet.manager.Config
import com.mycelium.wapi.wallet.manager.WalletModule
import com.mycelium.wapi.wallet.metadata.IMetaDataStorage
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import com.mycelium.wapi.wallet.tron.TronAccount
import com.mycelium.wapi.wallet.tron.TronAddress
import com.mycelium.wapi.wallet.tron.TronModule
import com.mycelium.wapi.wallet.tron.coins.TronMain
import com.mycelium.wapi.wallet.tron.coins.TronTest
import com.mycelium.wapi.wallet.tron.remote.TronBlockchainService
import java.util.UUID

class TRC20Module(
    private val backing: Backing<TRC20AccountContext>,
    private val walletDB: WalletDB,
    private val blockchainService: TronBlockchainService,
    private val networkParameters: NetworkParameters,
    metaDataStorage: IMetaDataStorage,
    private val accountListener: AccountListener?,
    private val tronModule: TronModule,
    /**
     * (contract address, stored label) -> label to use instead, or null to keep the stored one.
     * Lets the app config rename tokens without stranding accounts on the old default name.
     */
    private val refreshedLabel: (String, String) -> String? = { _, _ -> null },
) : WalletModule(metaDataStorage) {
    private val accounts = mutableMapOf<UUID, TRC20Account>()
    override val id = ID
    private val tronCoinType = if (networkParameters.isProdnet) TronMain else TronTest

    override fun createAccount(config: Config): WalletAccount<*> {
        val result: TRC20Account
        val baseLabel: String
        when (config) {
            is TRC20Config -> {
                val token = config.token as TRC20Token
                val tronAccount = config.tronAccount
                baseLabel = token.name
                val uuid = accountId(token, tronAccount)
                accounts[uuid]?.let { return it }
                val credentials = tronModule.credentialsOf(tronAccount)
                val accountContext = createAccountContext(uuid, config)
                backing.createAccountContext(accountContext.uuid, accountContext)
                result = TRC20Account(accountContext, token, tronAccount, credentials,
                    createBacking(accountContext, token), accountListener, blockchainService,
                    if (credentials == null) tronAccount.receivingAddress else null)
                tronAccount.updateEnabledTokens()
            }
            else -> throw NotImplementedError("Unknown config")
        }
        accounts[result.id] = result
        result.label = baseLabel
        storeLabel(result.id, baseLabel)
        return result
    }

    /** Deterministic id: token contract + parent account, so re-adding the same token is a no-op. */
    private fun accountId(token: TRC20Token, tronAccount: TronAccount): UUID {
        val contract = TronAddress.decode(token.contractAddress) ?: token.contractAddress.toByteArray()
        return UUID(BitUtils.uint64ToLong(contract, 1), tronAccount.id.mostSignificantBits)
    }

    private fun createBacking(context: TRC20AccountContext, token: TRC20Token) =
        TronAccountBacking(walletDB, context.uuid, token, tronCoinType) { context.blockHeight }

    override fun canCreateAccount(config: Config) = config is TRC20Config

    override fun deleteAccount(walletAccount: WalletAccount<*>, keyCipher: KeyCipher): Boolean =
        if (walletAccount is TRC20Account) {
            backing.deleteAccountContext(walletAccount.id)
            walletAccount.clearBacking()
            accounts.remove(walletAccount.id)
            walletAccount.tronAcc.updateEnabledTokens()
            true
        } else {
            false
        }

    override fun getAccounts() = accounts.values.toList()

    override fun getAccountById(id: UUID) = accounts[id]

    override fun loadAccounts(): Map<UUID, WalletAccount<*>> = backing.loadAccountContexts()
        .mapNotNull { accountFromUUID(it.uuid) }
        .associateBy { it.id }

    /** Accounts added before a token was renamed still carry its old default label — refresh those. */
    override fun afterAccountsLoaded() {
        accounts.values.forEach { account ->
            val stored = readLabel(account.id).ifEmpty { account.label }
            val fresh = refreshedLabel(account.contractAddress, stored) ?: return@forEach
            account.label = fresh
            storeLabel(account.id, fresh)
        }
    }

    private fun createAccountContext(uuid: UUID, config: TRC20Config? = null): TRC20AccountContext {
        val accountContextInDB = backing.loadAccountContext(uuid)
        return if (accountContextInDB != null) {
            TRC20AccountContext(accountContextInDB.uuid,
                accountContextInDB.currency,
                accountContextInDB.accountName,
                accountContextInDB.balance,
                { backing.updateAccountContext(it.uuid, it) },
                accountContextInDB.contractAddress,
                accountContextInDB.symbol,
                accountContextInDB.unitExponent,
                accountContextInDB.tronAccountId,
                accountContextInDB.archived,
                accountContextInDB.blockHeight)
        } else {
            val token = config!!.token as TRC20Token
            TRC20AccountContext(uuid, tronCoinType, token.name, Balance.getZeroBalance(tronCoinType),
                { backing.updateAccountContext(it.uuid, it) },
                token.contractAddress, token.symbol, token.unitExponent, config.tronAccount.id)
        }
    }

    private fun accountFromUUID(uuid: UUID): TRC20Account? {
        val accountContext = createAccountContext(uuid)
        val tronAccount = tronModule.getAccountById(accountContext.tronAccountId) as? TronAccount ?: return null
        val token = TRC20Token(accountContext.accountName, accountContext.symbol, accountContext.unitExponent,
            accountContext.contractAddress)
        val credentials = tronModule.credentialsOf(tronAccount)
        val account = TRC20Account(accountContext, token, tronAccount, credentials,
            createBacking(accountContext, token), accountListener, blockchainService,
            if (credentials == null) tronAccount.receivingAddress else null)
        accounts[account.id] = account
        return account
    }

    companion object {
        const val ID: String = "TRC20"
    }
}

fun WalletManager.getTRC20Accounts() = getAccounts().filter { it is TRC20Account && it.isVisible() }.map { it as TRC20Account }
fun WalletManager.getActiveTRC20Accounts() = getAccounts().filter { it is TRC20Account && it.isVisible() && it.isActive }.map { it as TRC20Account }
