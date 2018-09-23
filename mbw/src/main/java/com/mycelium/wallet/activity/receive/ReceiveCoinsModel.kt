package com.mycelium.wallet.activity.receive

import android.app.Application
import android.app.NotificationManager
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import android.media.AudioManager
import android.media.RingtoneManager
import android.nfc.NfcAdapter
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import com.mrd.bitlib.util.CoinUtil
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.AccountDisplayType
import com.mycelium.wallet.event.SyncFailed
import com.mycelium.wallet.event.SyncStopped
import com.mycelium.wapi.model.TransactionSummary
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.currency.CurrencyValue
import com.mycelium.wapi.wallet.currency.ExchangeBasedCurrencyValue
import com.squareup.otto.Subscribe

class ReceiveCoinsModel(
        val context: Application,
        val account: WalletAccount,
        private val accountLabel: String,
        val havePrivateKey: Boolean,
        showIncomingUtxo: Boolean = false
) {
    val amountData: MutableLiveData<CurrencyValue?> = MutableLiveData()
    val nfc: NfcAdapter? = NfcAdapter.getDefaultAdapter(context)
    val receivingAmount: MutableLiveData<CurrencyValue?> = MutableLiveData()
    val receivingAmountWrong: MutableLiveData<Boolean> = MutableLiveData()

    private var syncErrors = 0
    private val mbwManager = MbwManager.getInstance(context)
    private var address = account.receivingAddress.get()
    private var receivingSince = System.currentTimeMillis()
    private var lastAddressBalance: CurrencyValue? = null
    private var accountDisplayType: AccountDisplayType? = null

    init {
        mbwManager.eventBus.register(this)
        if (showIncomingUtxo) {
            mbwManager.watchAddress(address)
        }
        receivingAmountWrong.value = false
    }

    fun onCleared() {
        mbwManager.stopWatchingAddress()
        mbwManager.eventBus.unregister(this)
    }

    fun getPaymentUri(): String {
        val prefix = accountLabel

        val uri = StringBuilder(prefix).append(':')
        uri.append(address)
        if (!CurrencyValue.isNullOrZero(amountData.value)) {
            if (accountDisplayType == AccountDisplayType.COLU_ACCOUNT) {
                uri.append("?amountData=").append(amountData.value!!.value.toPlainString())
            } else {
                val value = ExchangeBasedCurrencyValue.fromValue(amountData.value,
                        account.accountDefaultCurrency, mbwManager.exchangeRateManager).value
                if (value != null) {
                    uri.append("?amountData=").append(CoinUtil.valueString(value,
                            CoinUtil.Denomination.BTC, false))
                } else {
                    Toast.makeText(context, R.string.value_conversion_error, Toast.LENGTH_LONG).show()
                }
            }
        }
        return uri.toString()
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
        var sum = if (interesting.isEmpty()) { null } else { interesting.first().value }
        interesting.drop(1).forEach { sum = sum!!.add(it.value, mbwManager.exchangeRateManager)}
        receivingAmount.value = sum

        if (!CurrencyValue.isNullOrZero(amountData.value) && sum != null) {
            // if the user specified an amountData, check it if it matches up...
            receivingAmountWrong.value = sum!! != amountData.value
            if (sum != lastAddressBalance) {
                makeNotification(sum)
            }
        }
    }

    fun makeNotification(sum: CurrencyValue?) {
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager?
        val mBuilder = NotificationCompat.Builder(context) //TODO api 28 change, broken.
                .setSmallIcon(R.drawable.ic_launcher)
                .setSound(soundUri, AudioManager.STREAM_NOTIFICATION) //This sets the sound to play
        notificationManager!!.notify(0, mBuilder.build())
        lastAddressBalance = sum
    }

    private fun getTransactionsToCurrentAddress(transactionsSince: MutableList<TransactionSummary>) =
            transactionsSince.filter { it.toAddresses.contains(address) }

    companion object {
        private const val MAX_SYNC_ERRORS = 8
    }
}