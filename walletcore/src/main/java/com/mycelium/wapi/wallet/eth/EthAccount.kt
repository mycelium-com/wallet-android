package com.mycelium.wapi.wallet.eth

import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.util.BitUtils
import com.mrd.bitlib.util.HexUtils
import com.mycelium.net.HttpEndpoint
import com.mycelium.net.ServerEndpoints
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.coins.Value.Companion.max
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.exceptions.GenericBuildTransactionException
import com.mycelium.wapi.wallet.exceptions.GenericInsufficientFundsException
import com.mycelium.wapi.wallet.genericdb.EthAccountBacking
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import org.web3j.crypto.*
import org.web3j.utils.Convert
import org.web3j.utils.Numeric
import java.math.BigInteger
import java.util.*
import java.util.logging.Level
import kotlin.concurrent.thread

class EthAccount(private val accountContext: EthAccountContext,
                 credentials: Credentials? = null,
                 backing: EthAccountBacking,
                 private val accountListener: AccountListener?,
                 endpoints: List<HttpEndpoint>,
                 address: EthAddress? = null) : AbstractEthERC20Account(accountContext.currency, credentials,
        backing, endpoints, address, EthAccount::class.simpleName) {
    var enabledTokens: MutableList<String> = accountContext.enabledTokens?.toMutableList()
            ?: mutableListOf()

    val accountIndex: Int
        get() = accountContext.accountIndex

    fun removeEnabledToken(tokenName: String) {
        enabledTokens.remove(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun addEnabledToken(tokenName: String) {
        enabledTokens.add(tokenName)
        accountContext.enabledTokens = enabledTokens
    }

    fun isEnabledToken(tokenName: String) = enabledTokens.contains(tokenName)

    fun hasHadActivity(): Boolean {
        return accountBalance.spendable.isPositive() || accountContext.nonce > BigInteger.ZERO
    }

    @Throws(GenericInsufficientFundsException::class, GenericBuildTransactionException::class)
    override fun createTx(toAddress: GenericAddress, value: Value, gasPrice: GenericFee): GenericTransaction {
        val gasPriceValue = (gasPrice as FeePerKbFee).feePerKb
        if (gasPriceValue.value <= BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Gas price should be positive and non-zero"))
        }
        if (value.value <= BigInteger.ZERO) {
            throw GenericBuildTransactionException(Throwable("Value should be positive and non-zero"))
        }
        // check whether account has enough funds
        if (value > calculateMaxSpendableAmount(gasPriceValue, null)) {
            throw GenericInsufficientFundsException(Throwable("Insufficient funds to send " + Convert.fromWei(value.value.toBigDecimal(), Convert.Unit.ETHER) +
                    " ether with gas price " + Convert.fromWei(gasPriceValue.valueAsBigDecimal, Convert.Unit.GWEI) + " gwei"))
        }

        try {
            val nonce = getNonce(receivingAddress)
            val rawTransaction = RawTransaction.createEtherTransaction(nonce,
                    gasPrice.feePerKb.value, BigInteger.valueOf(typicalEstimatedTransactionSize.toLong()),
                    toAddress.toString(), value.value)
            return EthTransaction(coinType, toAddress, value, gasPrice, rawTransaction)
        } catch (e: Exception) {
            throw GenericBuildTransactionException(Throwable(e.localizedMessage))
        }
    }

    override fun signTx(request: GenericTransaction?, keyCipher: KeyCipher?) {
        val rawTransaction = (request as EthTransaction).rawTransaction
        val signedMessage = TransactionEncoder.signMessage(rawTransaction, credentials)
        val hexValue = Numeric.toHexString(signedMessage)
        request.signedHex = hexValue
        request.txHash = TransactionUtils.generateTransactionHash(rawTransaction, credentials)
    }

    override fun broadcastTx(tx: GenericTransaction): BroadcastResult {
        val ethSendTransaction = client.ethSendRawTransaction((tx as EthTransaction).signedHex).send()
        if (ethSendTransaction.hasError()) {
            return BroadcastResult(ethSendTransaction.error.message, BroadcastResultType.REJECTED)
        }
        backing.putTransaction(-1, System.currentTimeMillis() / 1000, "0x" + HexUtils.toHex(tx.txHash),
                tx.signedHex!!, receivingAddress.addressString, tx.toAddress.toString(),
                tx.value, (tx.gasPrice as FeePerKbFee).feePerKb * typicalEstimatedTransactionSize.toBigInteger(), 0, accountContext.nonce)
        return BroadcastResult(BroadcastResultType.SUCCESS)
    }

    override fun getCoinType() = accountContext.currency

    override fun getBasedOnCoinType() = coinType

    private val ethBalanceService = EthBalanceService(receivingAddress.toString(), coinType, client, this.endpoints)

    private var balanceDisposable: Disposable = subscribeOnBalanceUpdates()

    private var incomingTxsDisposable: Disposable = subscribeOnIncomingTx()

    override fun getAccountBalance() = accountContext.balance

    override fun setLabel(label: String?) {
        accountContext.accountName = label!!
    }

    override fun getNonce() = accountContext.nonce

    override fun setNonce(nonce: BigInteger) {
        accountContext.nonce = nonce
    }

    override fun doSynchronization(mode: SyncMode?): Boolean {
        if (!selectEndpoint()) {
            return false
        }
        if (!syncTransactions()) {
            return false
        }
        updateBalanceCache()
        renewSubscriptions()
        return true
    }

    private fun updateBalanceCache() {
        ethBalanceService.updateBalanceCache()
        if (ethBalanceService.balance != accountContext.balance) {
            accountContext.balance = ethBalanceService.balance
            accountListener?.balanceUpdated(this)
        }
    }

    private fun selectEndpoint(): Boolean {
        val currentEndpointIndex = endpoints.currentEndpointIndex
        for (x in 0 until endpoints.size()) {
            val ethUtils = EthSyncChecker(client)
            try {
                if (ethUtils.isSynced) {
                    if (currentEndpointIndex != endpoints.currentEndpointIndex) {
                        ethBalanceService.client = client
                        balanceDisposable = subscribeOnBalanceUpdates()
                        incomingTxsDisposable = subscribeOnIncomingTx()
                    }
                    return true
                }
            } catch (ex: Exception) {
                logger.log(Level.SEVERE, "Error synchronizing ETH, $ex")
                logger.log(Level.SEVERE, "Switching to next endpoint...")
            }
            endpoints.switchToNextEndpoint()
            updateClient()
        }
        return false
    }

    override fun archiveAccount() {
        accountContext.archived = true
        dropCachedData()
        stopSubscriptions(newThread = true)
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

    override fun getId(): UUID = credentials?.ecKeyPair?.toUUID()
            ?: UUID.nameUUIDFromBytes(receivingAddress.getBytes())

    override fun broadcastOutgoingTransactions() = true

    override fun calculateMaxSpendableAmount(gasPrice: Value, ign: EthAddress?): Value {
        val spendable = accountBalance.spendable - gasPrice * typicalEstimatedTransactionSize.toLong()
        return max(spendable, Value.zeroValue(coinType))
    }

    override fun getLabel() = accountContext.accountName

    override fun getBlockChainHeight() = accountContext.blockHeight

    override fun setBlockChainHeight(height: Int) {
        accountContext.blockHeight = height
    }

    override fun isArchived() = accountContext.archived

    override fun getSyncTotalRetrievedTransactions() = 0

    override fun getTypicalEstimatedTransactionSize() = 21000

    override fun getPrivateKey(cipher: KeyCipher?): InMemoryPrivateKey {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun subscribeOnBalanceUpdates(): Disposable {
        return ethBalanceService.balanceFlowable.subscribeOn(Schedulers.io()).subscribe({ balance ->
            accountContext.balance = balance
            accountListener?.balanceUpdated(this)
        }, {
            logger.log(Level.SEVERE, "Error synchronizing ETH, $it")
        })
    }

    private fun subscribeOnIncomingTx(): Disposable {
        return ethBalanceService.incomingTxsFlowable.subscribeOn(Schedulers.io()).subscribe({ tx ->
            backing.putTransaction(-1, System.currentTimeMillis() / 1000, tx.hash,
                    tx.raw, tx.from, receivingAddress.addressString, valueOf(coinType, tx.value),
                    valueOf(coinType, tx.gasPrice * typicalEstimatedTransactionSize.toBigInteger()), 0, tx.nonce, tx.gas)
        }, {})
    }

    private fun renewSubscriptions() {
        updateClient()
        if (balanceDisposable.isDisposed) {
            balanceDisposable = subscribeOnBalanceUpdates()
        }
        if (incomingTxsDisposable.isDisposed) {
            incomingTxsDisposable = subscribeOnIncomingTx()
        }
    }

    //to avoid io.reactivex.exceptions.UndeliverableException (inside android.os.NetworkOnMainThreadException)
    //we have to stop subscriptions in another thread
    //but if we want to restart subscriptions we have to stop it synchronously before subscribe again
    fun stopSubscriptions(newThread: Boolean = true) {
        if (newThread) {
            thread {
                stopSubscriptions()
            }
        } else {
            stopSubscriptions()
        }
    }

    private fun stopSubscriptions() {
        client.shutdown()
        if (!balanceDisposable.isDisposed) {
            balanceDisposable.dispose()
        }
        if (!incomingTxsDisposable.isDisposed) {
            incomingTxsDisposable.dispose()
        }
    }

    override fun serverListChanged(newEndpoints: Array<HttpEndpoint>) {
        endpoints = ServerEndpoints(newEndpoints)
        updateClient()
        thread {
            stopSubscriptions(newThread = false)
            renewSubscriptions()
        }
    }

    fun fetchTxNonce(txid: String): BigInteger? {
        return try {
            val tx = client.ethGetTransactionByHash(txid).send()
            if (tx.result == null) {
                null
            } else {
                val nonce = tx.result.nonce
                backing.updateNonce(txid, nonce)
                nonce
            }
        } catch (e: Exception) {
            null
        }
    }
}


fun ECKeyPair.toUUID(): UUID = UUID(BitUtils.uint64ToLong(publicKey.toByteArray(), 8), BitUtils.uint64ToLong(
        publicKey.toByteArray(), 16))