package com.mycelium.wallet.activity.receive

import android.app.Application
import android.app.NotificationManager
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.nfc.NfcAdapter
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.wallet.GenericAddress
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.Value
import com.squareup.otto.Subscribe

class ReceiveCoinsModel(
        val context: Application,
        val account: WalletAccount<*, *>,
        private val accountLabel: String,
        val havePrivateKey: Boolean,
        showIncomingUtxo: Boolean = false
) {
    val amountData: MutableLiveData<Value?> = MutableLiveData()
    val alternativeAmountData: MutableLiveData<Value?> = MutableLiveData()
    val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    val receivingAmount: MutableLiveData<Value?> = MutableLiveData()
    val receivingAmountWrong: MutableLiveData<Boolean> = MutableLiveData()
    val receivingAddress: MutableLiveData<GenericAddress> = MutableLiveData()

    private var syncErrors = 0
    private val mbwManager = MbwManager.getInstance(context)
    private var receivingSince = System.currentTimeMillis()
    private var lastAddressBalance: Value? = null
    private var accountDisplayType: AccountDisplayType? = null

    init {
        mbwManager.eventBus.register(this)
        receivingAmountWrong.value = false
        receivingAddress.value = account.receiveAddress

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
        mbwManager.eventBus.unregister(this)
    }

    fun setAmount(newAmount: Value) {
        if (!Value.isNullOrZero(newAmount)) {
            alternativeAmountData.value = newAmount
            amountData.value = newAmount
        } else {
            amountData.value = null
            alternativeAmountData.value = null
        }
    }

    fun getPaymentUri(): String {
        val prefix = accountLabel

        val uri = StringBuilder(prefix).append(':')
        uri.append(receivingAddress.value)
        if (!Value.isNullOrZero(amountData.value)) {
            if (accountDisplayType == AccountDisplayType.COLU_ACCOUNT) {
                uri.append("?amountData=").append(amountData.value!!.valueAsBigDecimal.toPlainString())
            } else {
                val value = mbwManager.exchangeRateManager.get(amountData.value, account.coinType)
                if (value != null) {
                    uri.append("?amountData=").append(value.valueAsBigDecimal.toPlainString())
                } else {
                    Toast.makeText(context, R.string.value_conversion_error, Toast.LENGTH_LONG).show()
                }
            }
        }
        return uri.toString()
    }

    fun loadInstance(savedInstanceState: Bundle) {
        amountData.value = savedInstanceState.getSerializable(AMOUNT) as Value?
        receivingSince = savedInstanceState.getLong(RECEIVING_SINCE)
        syncErrors = savedInstanceState.getInt(SYNC_ERRORS)
        lastAddressBalance = savedInstanceState.getSerializable(LAST_ADDRESS_BALANCE) as Value?
    }

    fun saveInstance(outState: Bundle) {
        if (!Value.isNullOrZero(amountData.value)) {
            outState.putSerializable(AMOUNT, amountData.value)
        }
        outState.putLong(RECEIVING_SINCE, receivingSince)
        outState.putInt(SYNC_ERRORS, syncErrors)
        outState.putSerializable(LAST_ADDRESS_BALANCE, lastAddressBalance)
    }

    @Subscribe
    fun syncError(event: SyncFailed) {
        // stop syncing after a certain amountData of errors (no network available)
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
            if (interesting.first().isIncoming) interesting.first().received else interesting.first().sent
        }
        interesting.drop(1).forEach { sum = sum!!.add(if (it.isIncoming) it.received else it.sent) }
        receivingAmount.value = if (sum != null) Value.valueOf(account.coinType, sum!!.value)
        else Value.zeroValue(account.coinType)

        if (!Value.isNullOrZero(amountData.value) && sum != null) {
            // if the user specified an amountData, check it if it matches up...
            receivingAmountWrong.value = sum!! != amountData.value
            if (sum != lastAddressBalance) {
                //makeNotification(sum)
            }
        }
    }

    private fun makeNotification(sum: Value?) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        val mBuilder = NotificationCompat.Builder(context) //TODO api 28 change, broken.
                .setSmallIcon(R.drawable.ic_launcher)
                .setSound(soundUri, AudioManager.STREAM_NOTIFICATION) //This sets the sound to play
        notificationManager!!.notify(0, mBuilder.build())
        lastAddressBalance = sum
    }

    private fun getTransactionsToCurrentAddress(transactionsSince: MutableList<out GenericTransaction>) =
            transactionsSince.filter { tx -> tx.outputs.retainAll { it.address == receivingAddress.value } }

    companion object {
        private const val MAX_SYNC_ERRORS = 8
        private const val LAST_ADDRESS_BALANCE = "lastAddressBalance"
        private const val RECEIVING_SINCE = "receivingSince"
        private const val SYNC_ERRORS = "syncErrors"
        private const val AMOUNT = "amount"
    }
}