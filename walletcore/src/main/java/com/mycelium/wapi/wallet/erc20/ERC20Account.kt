package com.mycelium.wapi.wallet.erc20

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthAddress
import com.mycelium.wapi.wallet.eth.Web3jWrapper
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.Credentials
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.DefaultBlockParameterNumber
import org.web3j.tx.gas.DefaultGasProvider
import org.web3j.tx.gas.StaticGasProvider
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import kotlin.concurrent.thread

class ERC20Account(private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   private val ethAcc: EthAccount,
                   credentials: Credentials? = null,
                   backing: EthAccountBacking,
                   private val accountListener: AccountListener?,
                   web3jWrapper: Web3jWrapper) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, ERC20Account::class.simpleName, web3jWrapper) {

    private var transfersDisposable: Disposable = subscribeOnTransferEvents()

    private fun subscribeOnTransferEvents(): Disposable {
        val contract = web3jWrapper.loadContract(token.contractAddress, credentials!!, DefaultGasProvider())
        return contract.transferEventFlowable(DefaultBlockParameterNumber(getBlockHeight().toBigInteger()), DefaultBlockParameterName.PENDING)
                .filter { it.to == receivingAddress.addressString }
                .subscribeOn(Schedulers.io())
                .subscribe({
                    val txhash = it.log!!.transactionHash
                    val tx = web3jWrapper.ethGetTransactionByHash(txhash).send()
                    backing.putTransaction(-1, System.currentTimeMillis() / 1000, txhash,
                            "", it.from!!, it.to!!, transformValueForDb(Value.valueOf(token, it.value!!)),
                            Value.valueOf(basedOnCoinType, tx.result.gasPrice * tx.result.gas), 0,
                            tx.result.nonce, tx.result.gas, tx.result.gas)
                    updateBalanceCache()
                }, {
                    logger.log(Level.SEVERE, "onError in subscribeOnTransferEvents, ${it.localizedMessage}")
                })
    }

    override fun createTx(address: GenericAddress, amount: Value, fee: GenericFee): GenericTransaction {
        if (calculateMaxSpendableAmount(null, null).equalZero()) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds"))
        }
        val gasPrice = (fee as FeePerKbFee).feePerKb.value
        val gasLimit = BigInteger.valueOf(90_000)
        if (ethAcc.accountBalance.spendable.value < (gasPrice * gasLimit)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds on eth account to pay for fee"))
        }
        return Erc20Transaction(coinType, address, amount, gasPrice, gasLimit)
    }

    override fun signTx(request: GenericTransaction, keyCipher: KeyCipher) {
        (request as Erc20Transaction).txBinary = ByteArray(0)
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val erc20Tx = (tx as Erc20Transaction)
        try {
            accountContext.nonce = getNonce(receivingAddress)
            val erc20Contract = web3jWrapper.loadContract(token.contractAddress, credentials!!, StaticGasProvider(erc20Tx.gasPrice, erc20Tx.gasLimit))
            val result = erc20Contract.transfer(erc20Tx.toAddress.toString(), erc20Tx.value.value).send()
            if (!result.isStatusOK) {
                logger.log(Level.SEVERE, "Error sending ERC20 transaction, status not OK: ${result.status}")
                return BroadcastResult("Unable to send transaction.", BroadcastResultType.REJECTED)
            }
            tx.txHash = HexUtils.toBytes(result.transactionHash.substring(2))
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, result.transactionHash,
                    "", receivingAddress.addressString, tx.toAddress.toString(),
                    transformValueForDb(tx.value), Value.valueOf(basedOnCoinType, tx.gasPrice * result.gasUsed), 0,
                    accountContext.nonce, tx.gasLimit, result.gasUsed)
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error sending ERC20 transaction: ${e.localizedMessage}")
            return BroadcastResult("Unable to send transaction: ${e.localizedMessage}", BroadcastResultType.REJECTED)
        }
    }

    override fun getCoinType() = token

    override fun getBasedOnCoinType() = accountContext.currency

    private val balanceService = ERC20BalanceService(receivingAddress.addressString, token, basedOnCoinType, web3jWrapper, backing, credentials!!)

    override fun getAccountBalance() = readBalance()

    override fun getLabel(): String = token.name

    override fun setLabel(label: String?) {
    }

    @Synchronized
    override fun doSynchronization(mode: SyncMode?): Boolean {
        if (!syncTransactions()) {
            return false
        }
        updateBalanceCache()
        return true
    }

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun getBlockChainHeight() = accountContext.blockHeight

    override fun isArchived() = accountContext.archived

    override fun isActive() = !isArchived

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
        thread {
            transfersDisposable.dispose()
        }
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
        transfersDisposable = subscribeOnTransferEvents()
    }

    override fun dropCachedData() {
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true // TODO implement

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: EthAddress?): Value =
            accountBalance.spendable

    override fun getSyncTotalRetrievedTransactions() = 0 // TODO implement

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun updateBalanceCache() {
        balanceService.updateBalanceCache()
        if (balanceService.balance != accountContext.balance) {
            accountContext.balance = balanceService.balance
            accountListener?.balanceUpdated(this)
        }
    }

    // the following two wrappers are needed because we can't store balance in db with ERC20 coin type
    // we only can store balance in db with coinType within cryptocurrencies list
    // (refer to com.mycelium.wapi.wallet.coins.COIN for the list)
    // but on different activities we need balance with ERC20 coin type, therefore we need to convert
    // coinType -> basedOnCoinType (which is ETH for ERC20 and it's in the cryptocurrencies list)
    // all the time we going to store it in db, and basedOnCoinType -> coinType when we read from db
    private fun saveBalance(balance: Balance) {
        accountContext.balance = Balance(Value.valueOf(basedOnCoinType, balance.confirmed.value),
                Value.valueOf(basedOnCoinType, balance.pendingReceiving.value),
                Value.valueOf(basedOnCoinType, balance.pendingSending.value),
                Value.valueOf(basedOnCoinType, 0))
    }

    private fun readBalance(): Balance {
        val balance = accountContext.balance
        return Balance(Value.valueOf(coinType, balance.confirmed.value),
                Value.valueOf(coinType, balance.pendingReceiving.value),
                Value.valueOf(coinType, balance.pendingSending.value),
                Value.valueOf(coinType, 0))
    }

    /**
     * replace coinType -> basedOnCoinType before insert into db
     */
    private fun transformValueForDb(value: Value): Value {
        return Value.valueOf(basedOnCoinType, value.value)
    }
}