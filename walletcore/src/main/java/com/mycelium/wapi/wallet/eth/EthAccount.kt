package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import io.reactivex.disposables.Disposable
import org.web3j.crypto.*
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.infura.InfuraHttpService
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class EthAccount(private val accountContext: EthAccountContext,
                 private val credentials: Credentials? = null,
                 private val accountListener: AccountListener?,
                 address: EthAddress? = null) : WalletAccount<EthAddress> {
    private val logger = Logger.getLogger(EthBalanceService::javaClass.name)
    val receivingAddress = credentials?.let { EthAddress(coinType, it.address) } ?: address!!

    override fun setAllowZeroConfSpending(b: Boolean) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    @Throws(GenericInsufficientFundsException::class, GenericBuildTransactionException::class)
    override fun createTx(toAddress: GenericAddress, value: Value, gasPrice: GenericFee): GenericTransaction {
        val gasPriceLong = (gasPrice as FeePerKbFee).feePerKb.value
        // check whether account has enough funds
        if (value > calculateMaxSpendableAmount(gasPriceLong, null)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPriceLong.toBigDecimal(), Convert.Unit.GWEI) + " gwei"))
        }

        try {
            val nonce = getNonce(receivingAddress)
            val rawTransaction = RawTransaction.createEtherTransaction(nonce,
                    BigInteger.valueOf(gasPrice.feePerKb.value), BigInteger.valueOf(21000),
                    toAddress.toString(), BigInteger.valueOf(value.value))
            return EthTransaction(coinType, toAddress, value, gasPrice, rawTransaction)
        } catch (e: Exception) {
            throw GenericBuildTransactionException(Throwable(e.localizedMessage))
        }
    }

    @Throws(Exception::class)
    private fun getNonce(address: EthAddress): BigInteger {
        return try {
            val web3j: Web3j = Web3j.build(InfuraHttpService("http://parity.mycelium.com:18545"))
            val ethGetTransactionCount = web3j.ethGetTransactionCount(address.toString(),
                    DefaultBlockParameterName.PENDING)
                    .send()

            accountContext.nonce = ethGetTransactionCount.transactionCount
            accountContext.nonce
        } catch (e: Exception) {
            accountContext.nonce
        }
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        val rawTransaction = (request as EthTransaction).rawTransaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.signedHex = hexValue
        request.txHash = TransactionUtils.generateTransactionHash(rawTransaction, credentials)
    }

    override fun broadcastTx(tx: GenericTransaction?): BroadcastResult {
        val web3j: Web3j = Web3j.build(InfuraHttpService("http://parity.mycelium.com:18545"))
        val ethSendTransaction = web3j.ethSendRawTransaction((tx as EthTransaction).signedHex).send()
        if (ethSendTransaction.hasError()) {
            return BroadcastResult(ethSendTransaction.error.message, BroadcastResultType.REJECTED)
        }
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun getReceiveAddress() = receivingAddress

    override fun getCoinType() = accountContext.currency

    override fun getBasedOnCoinType() = coinType

    private val ethBalanceService = EthBalanceService(receivingAddress.toString(), coinType)

    private var balanceDisposable: Disposable = subscribeOnBalanceUpdates()

    override fun getAccountBalance() = accountContext.balance

    override fun isMineAddress(address: GenericAddress?) =
            address == receivingAddress

    override fun isExchangeable() = true

    override fun getTx(transactionId: ByteArray?): GenericTransaction {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTxSummary(transactionId: ByteArray?): GenericTransactionSummary {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getTransactionSummaries(offset: Int, limit: Int) = emptyList<GenericTransactionSummary>()

    override fun getTransactionsSince(receivingSince: Long) = emptyList<GenericTransactionSummary>()

    override fun getTransactions(offset: Int, limit: Int) = emptyList<GenericTransaction>()

    override fun getUnspentOutputViewModels(): MutableList<GenericOutputViewModel> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getLabel() = accountContext.accountName

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    override fun isSpendingUnconfirmed(tx: GenericTransaction?): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        return false
    }

    override fun synchronize(mode: SyncMode?): Boolean {
        val succeed = ethBalanceService.updateBalanceCache()
        if (succeed) {
            accountContext.balance = ethBalanceService.balance
            accountListener?.balanceUpdated(this)
            if (balanceDisposable.isDisposed) {
                balanceDisposable = subscribeOnBalanceUpdates()
            }
        }
        return succeed
    }

    override fun getBlockChainHeight(): Int {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun canSpend() = credentials != null

    override fun isSyncing() = false

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
        accountContext.balance = Balance.getZeroBalance(coinType)
    }

    override fun isVisible() = true

    override fun isDerivedFromInternalMasterseed() = true

    override fun getId() = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

    override fun isSynchronizing() = false

    override fun broadcastOutgoingTransactions() = true

    override fun removeAllQueuedTransactions() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun calculateMaxSpendableAmount(gasPrice: Long, ign: EthAddress?): Value {
        val spendable = accountBalance.spendable - Value.valueOf(coinType, gasPrice * typicalEstimatedTransactionSize)
        if (spendable < 0) {
            return Value.zeroValue(coinType)
        }
        return spendable
    }

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getDummyAddress() = EthAddress.getDummyAddress(coinType)

    override fun getDummyAddress(subType: String?): EthAddress = dummyAddress

    override fun getDependentAccounts() = emptyList<WalletAccount<GenericAddress>>()

    override fun queueTransaction(transaction: GenericTransaction) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun subscribeOnBalanceUpdates(): Disposable {
        return ethBalanceService.balanceFlowable.subscribe({ balance ->
            accountContext.balance = balance
            accountListener?.balanceUpdated(this)
        }, {
            logger.log(Level.SEVERE, "Error synchronizing ETH, $it")
        })
    }

    fun stopSubscriptions() {
        if (!balanceDisposable.isDisposed) {
            balanceDisposable.dispose()
        }
    }
}

fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))
