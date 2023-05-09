package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.mycelium.view.Denomination
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.NoneItem
import com.mycelium.wallet.activity.send.SpinnerItem
import com.mycelium.wallet.activity.send.TransactionItem
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wallet.activity.util.AdaptiveDateFormat
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.startCoroutineTimer
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.TOKEN_TRANSFER_GAS_LIMIT
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.eth.EthTransactionData
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import kotlinx.coroutines.*
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.concurrent.timerTask

class SendEthModel(application: Application,
                   account: WalletAccount<*>,
                   intent: Intent)
    : SendCoinsModel(application, account, intent) {
    var txItems: List<SpinnerItem> = emptyList()
    val parentAccount: EthAccount? = (account as? ERC20Account)?.ethAcc
    val gasLimitStatus: MutableLiveData<GasLimitStatus> = MutableLiveData(GasLimitStatus.OK)
    val denomination: Denomination = mbwManager.getDenomination(Utils.getEthCoinType())

    val selectedTxItem: MutableLiveData<SpinnerItem> = object : MutableLiveData<SpinnerItem>() {
        override fun setValue(value: SpinnerItem) {
            if (value != this.value) {
                super.setValue(value)
                val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                when (value) {
                    is NoneItem ->
                        transactionData.value = EthTransactionData(null, oldData.gasLimit, oldData.inputData, null)
                    is TransactionItem -> {
                        val tx = value.tx as EthTransactionSummary
                        val suggestedGasPrice = if (tx.fee != null) {
                            val oldFeePlusSomething = tx.fee!!.value / tx.gasUsed + Convert.toWei("10", Convert.Unit.GWEI).toBigInteger()
                            selectedFee.value?.value?.max(oldFeePlusSomething) ?: oldFeePlusSomething
                        } else null
                        transactionData.value = EthTransactionData(tx.nonce, oldData.gasLimit, oldData.inputData, suggestedGasPrice)
                    }
                }
            }
        }
    }

    val gasLimit: MutableLiveData<BigInteger?> = object : MutableLiveData<BigInteger?>() {
        override fun setValue(value: BigInteger?) {
            if (value != this.value) {
                gasLimitStatus.value = if (value != null) {
                    when {
                        value < Transfer.GAS_LIMIT -> GasLimitStatus.ERROR
                        account is ERC20Account && value < BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT) -> GasLimitStatus.WARNING
                        else -> GasLimitStatus.OK
                    }
                } else {
                    GasLimitStatus.OK
                }

                super.setValue(value)
                val oldData =
                    (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value =
                    EthTransactionData(oldData.nonce, value, oldData.inputData, oldData.suggestedGasPrice)
            }
        }
    }

    val gasPrice: MutableLiveData<BigInteger?> = object : MutableLiveData<BigInteger?>() {
        override fun setValue(value: BigInteger?) {
            if (value != this.value) {
                super.setValue(value)
                val oldData =
                        (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value =
                        EthTransactionData(oldData.nonce, oldData.gasLimit, oldData.inputData, value)
                if (value != null) {
                    feeUpdater.stop()
                } else {
                    feeUpdater.start()
                }
            }
        }
    }

    val inputData: MutableLiveData<String?> = object : MutableLiveData<String?>() {
        override fun setValue(value: String?) {
            if (value != this.value) {
                super.setValue(value)
                val oldData = (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value = EthTransactionData(oldData.nonce, oldData.gasLimit,
                        if (value == null || value.isEmpty()) null else value, oldData.suggestedGasPrice)
            }
        }
    }

    val estimatedFee: MutableLiveData<Value> = MutableLiveData()
    val totalFee: MutableLiveData<Value> = MutableLiveData()
    val feeUpdater = FeeUpdater(FEE_UPDATE_INTERVAL, { feeUpdateRoutine() })

    init {
        populateTxItems()
        selectedTxItem.value = NoneItem()
        feeUpdater.start()
    }

    private suspend fun feeUpdateRoutine() {
        withContext(Dispatchers.Main) {
            transactionStatus.value = TransactionStatus.BUILDING
        }
        delay(1000) // for more visible update effect
        mbwManager.getFeeProvider(account.basedOnCoinType).updateFeeEstimationsAsync()
        feeEstimation = mbwManager.getFeeProvider(account.basedOnCoinType).estimation
        feeUpdatePublisher.onNext(Unit)
        txRebuildPublisher.onNext(Unit)
    }

    private fun populateTxItems() {
        val outgoingUnconfirmedTransactions = (account as AbstractEthERC20Account).getUnconfirmedTransactions().filter {
            it.sender.addressString.equals(account.receivingAddress.addressString, true)
        }
        val items: MutableList<SpinnerItem> = mutableListOf()

        items.add(NoneItem()) // fallback item

        val dateFormat = AdaptiveDateFormat(context)
        items.addAll(outgoingUnconfirmedTransactions.map {
            val date = dateFormat.format(Date(it.timestamp * 1000L))
            val amount = it.transferred.abs().toStringWithUnit(
                    mbwManager.getDenomination(mbwManager.selectedAccount.coinType))

            TransactionItem(it, date, amount)
        })

        txItems = items
    }

    override fun handlePaymentRequest(toSend: Value): TransactionStatus {
        throw IllegalStateException("Ethereum does not support payment requests")
    }

    override fun getFeeLvlItems(): List<FeeLvlItem> {
        return MinerFee.values()
                .map { fee ->
                    val blocks = when (fee) {
                        MinerFee.LOWPRIO -> 120
                        MinerFee.ECONOMIC -> 20
                        MinerFee.NORMAL -> 8
                        MinerFee.PRIORITY -> 2
                    }
                    val duration = Utils.formatBlockcountAsApproxDuration(mbwManager, blocks, EthCoin.BLOCK_TIME_IN_SECONDS)
                    FeeLvlItem(fee, "~$duration", SelectableRecyclerView.SRVAdapter.VIEW_TYPE_ITEM)
                }
    }

    override fun estimateTxSize(): Int {
        val gl = (transactionData.value as? EthTransactionData)?.gasLimit

        return transaction?.estimatedTransactionSize
            ?: if (gl != null && gasLimitStatus.value != GasLimitStatus.ERROR) {
                gl.toInt()
            } else {
                account.typicalEstimatedTransactionSize
            }
    }
    
    enum class GasLimitStatus {
        OK, WARNING, ERROR
    }

    companion object {
        val FEE_UPDATE_INTERVAL = TimeUnit.SECONDS.toMillis(30)
    }
}

class FeeUpdater(
    private val feeUpdateIntervalMs: Long,
    private val feeUpdateRoutine: suspend () -> Unit,
    private val onStop: (() -> Unit)? = null
) {
    val secondsUntilNextUpdate: MutableLiveData<Long> = MutableLiveData(feeUpdateIntervalMs / 1000L)
    private lateinit var updateSecondsTimer: Job
    private lateinit var feeUpdateScheduledTask: Timer
    @Volatile
    var hasStarted = false
        private set

    fun start() {
        if (hasStarted) return

        hasStarted = true
        startUpdateSecondsTimer()
        feeUpdateScheduledTask = Timer().apply {
            scheduleAtFixedRate(timerTask {
                runBlocking {
                    stopUpdateSecondsTimer()
                    feeUpdateRoutine()
                    startUpdateSecondsTimer()
                }
            }, feeUpdateIntervalMs, feeUpdateIntervalMs)
        }
    }

    fun stop() {
        if (!hasStarted) return

        feeUpdateScheduledTask.cancel()
        stopUpdateSecondsTimer()
        hasStarted = false
        onStop?.invoke()
    }

    private fun startUpdateSecondsTimer() {
        updateSecondsTimer = getUpdateSecondsTimer()
        updateSecondsTimer.start()
    }

    private fun stopUpdateSecondsTimer() {
        updateSecondsTimer.cancel()
        secondsUntilNextUpdate.postValue(feeUpdateIntervalMs / 1000L)
    }

    private fun getUpdateSecondsTimer() = startCoroutineTimer(
        CoroutineScope(Dispatchers.Default + SupervisorJob()), 0, 1000) {
            secondsUntilNextUpdate.postValue((secondsUntilNextUpdate.value?.minus(1)))
        }
}