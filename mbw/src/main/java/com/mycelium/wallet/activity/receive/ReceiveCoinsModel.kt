package com.mycelium.wallet.activity.receive

import android.app.Application
import android.app.NotificationManager
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.nfc.NfcAdapter
import android.os.Bundle
import androidx.core.app.NotificationCompat
import androidx.lifecycle.MutableLiveData
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.fio.FioModule
import com.squareup.otto.Subscribe

class ReceiveCoinsModel(
        val context: Application,
        val account: WalletAccount<*>,
        private val accountLabel: String,
        showIncomingUtxo: Boolean = false
) {
    val amount: MutableLiveData<Value?> = MutableLiveData()
    val alternativeAmountData: MutableLiveData<Value?> = MutableLiveData()
    val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    val receivingAmount: MutableLiveData<Value?> = MutableLiveData()
    val receivingAmountWrong: MutableLiveData<Boolean> = MutableLiveData()
    val receivingAddress: MutableLiveData<Address> = MutableLiveData()
    val receivingFioName = MutableLiveData<String>()
    val fioNameList = MutableLiveData<List<String>>()
    val alternativeAmountWarning: MutableLiveData<Boolean> = MutableLiveData()

    private var syncErrors = 0
    private val mbwManager = MbwManager.getInstance(context)
    private var receivingSince = System.currentTimeMillis()
    private var lastAddressBalance: Value? = null
    private var accountDisplayType: AccountDisplayType? = null

    init {
        MbwManager.getEventBus().register(this)
        receivingAmountWrong.value = false
        receivingAddress.value = account.receiveAddress

        val walletManager = mbwManager.getWalletManager(false)
        val module = walletManager.getModuleById(FioModule.ID) as FioModule
        fioNameList.value = module.getFIONames(account).map { it.name }

        if (showIncomingUtxo) {
            updateObservingAddress()
        }
    }

    fun updateObservingAddress() {
        val address = receivingAddress.value
        mbwManager.watchAddress(address)
    }

    fun onCleared() {
        mbwManager.stopWatchingAddress()
        MbwManager.getEventBus().unregister(this)
    }

    fun setAmount(newAmount: Value) {
        amount.value = if (!Value.isNullOrZero(newAmount)) {
            newAmount
        } else {
            null
        }
    }

    fun setAlternativeAmount(newAmount: Value) {
        alternativeAmountData.value = if (!Value.isNullOrZero(newAmount)) {
            newAmount
        } else {
            null
        }
    }

    fun setAlternativeAmountWarning(show: Boolean) {
        alternativeAmountWarning.value = show
    }

    fun getPaymentUri(): String {
        val prefix = accountLabel
        val uri = StringBuilder(prefix)
                .append(':')
                .append(receivingAddress.value)
        if (!Value.isNullOrZero(amount.value)) {
            if (accountDisplayType == AccountDisplayType.COLU_ACCOUNT) {
                uri.append("?amount=").append(amount.value!!.valueAsBigDecimal.stripTrailingZeros().toPlainString())
            } else {
                val value = mbwManager.exchangeRateManager.get(amount.value, account.coinType) ?: amount.value

                if (value != null) {
                    uri.append(if (account is AbstractEthERC20Account) "?value=" else "?amount=")
                    uri.append(value.valueAsBigDecimal.stripTrailingZeros().toPlainString())
                } else {
                    Toaster(context).toast(R.string.value_conversion_error, false);
                }
                if (account is ERC20Account) {
                    uri.append("&req-asset=${account.coinType.contractAddress}")
                }
            }
        }
        return uri.toString()
    }

    fun loadInstance(savedInstanceState: Bundle) {
        amount.value = savedInstanceState.getSerializable(AMOUNT) as Value?
        receivingSince = savedInstanceState.getLong(RECEIVING_SINCE)
        syncErrors = savedInstanceState.getInt(SYNC_ERRORS)
        lastAddressBalance = savedInstanceState.getSerializable(LAST_ADDRESS_BALANCE) as Value?
    }

    fun saveInstance(outState: Bundle) {
        if (!Value.isNullOrZero(amount.value)) {
            outState.putSerializable(AMOUNT, amount.value)
        }
        outState.putLong(RECEIVING_SINCE, receivingSince)
        outState.putInt(SYNC_ERRORS, syncErrors)
        outState.putSerializable(LAST_ADDRESS_BALANCE, lastAddressBalance)
    }

    @Subscribe
    fun syncError(event: SyncFailed) {
        // stop syncing after a certain amount of errors (no network available)
        if (++syncErrors > MAX_SYNC_ERRORS) {
            mbwManager.stopWatchingAddress()
        }
    }

    @Subscribe
    fun syncStopped(event: SyncStopped) {
        val transactionsSince = account.getTransactionsSince(receivingSince)
        val interesting = getTransactionsToCurrentAddress(transactionsSince)
        var sum = if (interesting.isEmpty()) {
            null
        } else {
            interesting.first().transferred.abs()
        }
        interesting.drop(1).forEach { sum = sum!!.plus(it.transferred.abs())}
        receivingAmount.value = if (sum != null) Value.valueOf(account.coinType, sum!!.value)
        else Value.zeroValue(account.coinType)

        if (!Value.isNullOrZero(amount.value) && sum != null) {
            // if the user specified an amount, check it if it matches up...
            receivingAmountWrong.value = sum!! != Value.valueOf(account.coinType, amount.value!!.value)
            if (sum != lastAddressBalance) {
                makeNotification(sum)
            }
        }
    }

    private fun makeNotification(sum: Value?) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        val mBuilder = NotificationCompat.Builder(context, "coins received channel")
                .setSmallIcon(R.drawable.ic_notification_icon)
                .setSound(soundUri, AudioManager.STREAM_NOTIFICATION) //This sets the sound to play
        notificationManager!!.notify(0, mBuilder.build())
        lastAddressBalance = sum
    }

    private fun getTransactionsToCurrentAddress(transactionsSince: List<TransactionSummary>) =
            transactionsSince.filter { tx -> tx.outputs.any {it.address == receivingAddress.value} }

    companion object {
        private const val MAX_SYNC_ERRORS = 8
        private const val LAST_ADDRESS_BALANCE = "lastAddressBalance"
        private const val RECEIVING_SINCE = "receivingSince"
        private const val SYNC_ERRORS = "syncErrors"
        private const val AMOUNT = "amount"
    }
}
