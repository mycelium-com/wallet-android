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
import org.web3j.abi.TypeDecoder
import org.web3j.abi.datatypes.Address
import org.web3j.abi.datatypes.generated.Uint256
import org.web3j.crypto.Credentials
import org.web3j.tx.gas.StaticGasProvider
import java.lang.reflect.Method
import java.math.BigInteger
import java.util.*
import java.util.logging.Level


class ERC20Account(private val accountContext: ERC20AccountContext,
                   private val token: ERC20Token,
                   private val ethAcc: EthAccount,
                   credentials: Credentials? = null,
                   backing: EthAccountBacking,
                   private val accountListener: AccountListener?,
                   web3jWrapper: Web3jWrapper) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, ERC20Account::class.simpleName, web3jWrapper) {
    private var incomingTxDisposable: Disposable? = null
    private val balanceService = ERC20BalanceService(receivingAddress.addressString, token, basedOnCoinType, web3jWrapper, credentials!!)
    private var removed = false

    private fun subscribeOnIncomingTransactions(): Disposable {
        return web3jWrapper.pendingTransactionFlowable()
                .filter { tx ->
                    logger.log(Level.INFO, "tx.hash: ${tx.hash}, input: ${tx.input}, from: ${tx.from}")
                    token.contractAddress.equals(tx.to, true) &&
                            isTransfer(tx.input) &&
                            receiveAddress.addressString.equals(getToAddress(tx.input), true)
                }.subscribeOn(Schedulers.io()).subscribe({ tx ->
                    logger.log(Level.INFO, "have received incoming transaction")
                    backing.putTransaction(-1, System.currentTimeMillis() / 1000, tx.hash,
                            tx.raw, tx.from, tx.to, Value.valueOf(basedOnCoinType, getValue(tx.input)),
                            Value.valueOf(basedOnCoinType, tx.gasPrice * typicalEstimatedTransactionSize.toBigInteger()), 0, tx.nonce, tx.gas)
                    updateBalanceCache()
                }, {
                    logger.log(Level.SEVERE, "onError in subscribeOnPendingTransactions, ${it.localizedMessage}")
                })
    }

    private fun isTransfer(input: String?) = input != null && input.length == 138 && input.substring(0, 10) == TRANSFER_ID

    private fun getToAddress(input: String?): String? {
        input ?: return null
        val to: String = input.substring(10, 74)
        val refMethod: Method = TypeDecoder::class.java.getDeclaredMethod("decode", String::class.java, Int::class.javaPrimitiveType, Class::class.java)
        refMethod.isAccessible = true
        val address: Address = refMethod.invoke(null, to, 0, Address::class.java) as Address
        return address.toString()
    }

    private fun getValue(input: String): BigInteger {
        val value: String = input.substring(74)
        val refMethod: Method = TypeDecoder::class.java.getDeclaredMethod("decode", String::class.java, Int::class.javaPrimitiveType, Class::class.java)
        refMethod.isAccessible = true
        val amount = refMethod.invoke(null, value, 0, Uint256::class.java) as Uint256
        return amount.value
    }

    override fun createTx(address: GenericAddress, amount: Value, fee: GenericFee): GenericTransaction {
        if (calculateMaxSpendableAmount(null, null) < amount) {
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
                    Value.valueOf(basedOnCoinType, tx.value.value), Value.valueOf(basedOnCoinType, tx.gasPrice * result.gasUsed), 0,
                    accountContext.nonce, tx.gasLimit, result.gasUsed)
            return BroadcastResult(BroadcastResultType.SUCCESS)
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Error sending ERC20 transaction: ${e.localizedMessage}")
            return BroadcastResult("Unable to send transaction: ${e.localizedMessage}", BroadcastResultType.REJECTED)
        }
    }

    override fun getCoinType() = token

    override fun getBasedOnCoinType() = accountContext.currency

    override fun getAccountBalance() = readBalance()

    override fun getLabel(): String = token.name

    override fun setLabel(label: String?) {
    }

    @Synchronized
    override fun doSynchronization(mode: SyncMode?): Boolean {
        if (removed || isArchived) {
            return false
        }
        renewSubscriptions()
        return updateBalanceCache()
    }

    private fun renewSubscriptions() {
        stopSubscriptions()
        logger.log(Level.INFO, "Resubscribing on incoming transactions...")
        incomingTxDisposable = subscribeOnIncomingTransactions()
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
        stopSubscriptions()
    }

    override fun activateAccount() {
        accountContext.archived = false
        dropCachedData()
    }

    override fun dropCachedData() {
        saveBalance(Balance.getZeroBalance(coinType))
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = false

    override fun getId(): UUID = accountContext.uuid

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(minerFeePerKilobyte: Value?, destinationAddress: EthAddress?): Value =
            accountBalance.spendable

    override fun getSyncTotalRetrievedTransactions() = 0 // TODO implement after full transaction history implementation

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun updateBalanceCache(): Boolean {
        balanceService.updateBalanceCache()
        var newBalance = balanceService.balance
        syncTransactions()

        val pendingReceiving = getPendingReceiving()
        val pendingSending = getPendingSending()
        newBalance = Balance(Value.valueOf(basedOnCoinType, newBalance.confirmed.value - pendingSending),
                Value.valueOf(basedOnCoinType, pendingReceiving), Value.valueOf(basedOnCoinType, pendingSending), Value.zeroValue(basedOnCoinType))
        if (newBalance != accountContext.balance) {
            accountContext.balance = newBalance
            accountListener?.balanceUpdated(this)
            return true
        }
        return false
    }

    private fun getPendingReceiving(): BigInteger {
        return backing.getUnconfirmedTransactions().filter { it.from != receiveAddress.addressString && it.to == receiveAddress.addressString }
                .map { it.value.value }
                .fold(BigInteger.ZERO, BigInteger::add)
    }

    private fun getPendingSending(): BigInteger {
        return backing.getUnconfirmedTransactions().filter { it.from == receiveAddress.addressString && it.to != receiveAddress.addressString }
                .map { it.value.value }
                .fold(BigInteger.ZERO, BigInteger::add)
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

    fun stopSubscriptions(remove: Boolean = false) {
        removed = remove
        if (incomingTxDisposable != null && !incomingTxDisposable!!.isDisposed) {
            logger.log(Level.INFO, "Stopping subscriptions...")
            incomingTxDisposable!!.dispose()
            incomingTxDisposable = null
        }
    }

    companion object {
        const val TRANSFER_ID = "0xa9059cbb"
    }
}