package com.mycelium.wallet.activity.send.model

import android.app.Application
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.MinerFee
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.send.NoneItem
import com.mycelium.wallet.activity.send.SpinnerItem
import com.mycelium.wallet.activity.send.TransactionItem
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import com.mycelium.wallet.activity.util.AdaptiveDateFormat
import com.mycelium.wallet.activity.util.toStringFriendlyWithUnit
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.EthTransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.AVG_TOKEN_TRANSFER_GAS
import com.mycelium.wapi.wallet.erc20.ERC20Account.Companion.TOKEN_TRANSFER_GAS_LIMIT
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.eth.EthTransactionData
import com.mycelium.wapi.wallet.eth.coins.EthCoin
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigInteger
import java.util.*

class SendEthModel(application: Application,
                   account: WalletAccount<*>,
                   intent: Intent)
    : SendCoinsModel(application, account, intent) {
    var txItems: List<SpinnerItem> = emptyList()

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
                super.setValue(value)
                gasLimitStatus.value = if (value != null) {
                    when {
                        value < Transfer.GAS_LIMIT -> SendEthViewModel.GasLimitStatus.ERROR
                        value < BigInteger.valueOf(TOKEN_TRANSFER_GAS_LIMIT) -> SendEthViewModel.GasLimitStatus.WARNING
                        else -> SendEthViewModel.GasLimitStatus.OK
                    }
                } else {
                    SendEthViewModel.GasLimitStatus.EMPTY
                }

                val oldData =
                    (transactionData.value as? EthTransactionData) ?: EthTransactionData()
                transactionData.value =
                    EthTransactionData(oldData.nonce, if (value == null || value < Transfer.GAS_LIMIT) null else value,
                                       oldData.inputData, oldData.suggestedGasPrice)
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

    var estimatedFee: String? = null
    val convertedEstimatedFee: MutableLiveData<String?> = MutableLiveData()
    val parentAccountLabel: String? = (account as? ERC20Account)?.ethAcc?.label
    var gasLimitStatus: MutableLiveData<SendEthViewModel.GasLimitStatus> = MutableLiveData(SendEthViewModel.GasLimitStatus.EMPTY)

    init {
        populateTxItems()
        selectedTxItem.value = NoneItem()
        selectedFee.observeForever {
            val fee = Value.valueOf(it.type, BigInteger.valueOf(AVG_TOKEN_TRANSFER_GAS) * it.value)
            estimatedFee = fee.toStringFriendlyWithUnit()
            convertedEstimatedFee.value =
                " ~${mbwManager.exchangeRateManager.get(fee, mbwManager.getFiatCurrency(account.coinType))?.toStringFriendlyWithUnit() ?: ""}"
        }
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

    override fun estimateTxSize(): Int = transaction?.estimatedTransactionSize
        ?: (transactionData.value as? EthTransactionData)?.gasLimit?.toInt()
        ?: account.typicalEstimatedTransactionSize
}