package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.api.lib.CurrencyCode
import com.mycelium.wapi.wallet.Util.convertToDate
import com.mycelium.wapi.wallet.Util.strToBigInteger
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioRequestStatus
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.SentFIORequestContent
import kotlinx.android.synthetic.main.fio_sent_request_status_activity.*
import java.text.DateFormat
import java.util.*

class SentFioRequestStatusActivity : AppCompatActivity() {
    companion object {
        const val CONTENT = "content"
        fun start(activity: Activity, item: FIORequestContent) {
            with(Intent(activity, SentFioRequestStatusActivity::class.java)) {
                putExtra(CONTENT, item.toJson())
                activity.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fio_sent_request_status_activity)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            title = "Sent FIO Request"
        }
        val mbwManager = MbwManager.getInstance(this)
        val fioRequestContent: SentFIORequestContent = Gson().fromJson(intent.getStringExtra(CONTENT), SentFIORequestContent::class.java)

        // hack for requests requesting bitcoins
        val requestedCurrency: CryptoCurrency? = if (fioRequestContent.deserializedContent!!.chainCode == "BTC") {
            Utils.getBtcCoinType()
        } else {
            COINS.values.firstOrNull {
                it.symbol.equals(fioRequestContent.deserializedContent?.chainCode ?: "", true)
            }
        }

        val status = FioRequestStatus.getStatus(fioRequestContent.status)
        tvStatus.text = if (status != FioRequestStatus.NONE) {
            val color = when (status) {
                FioRequestStatus.SENT_TO_BLOCKCHAIN -> R.color.fio_green
                FioRequestStatus.REJECTED -> R.color.fio_red
                else -> R.color.fio_request_pending
            }
            tvStatus.setTextColor(ContextCompat.getColor(this, color))
            status.status.capitalize()
        } else {
            ""
        }

        if (requestedCurrency != null) {
            val amount = Value.valueOf(requestedCurrency, strToBigInteger(requestedCurrency,
                    fioRequestContent.deserializedContent!!.amount))
            tvAmount.text = amount.toStringWithUnit()
            val convertedAmount = mbwManager.exchangeRateManager.get(amount, Utils.getTypeByName(CurrencyCode.USD.shortString)!!).toStringWithUnit()
            tvConvertedAmount.text = " ~ $convertedAmount"
        } else {
            tvAmount.text = "${fioRequestContent.deserializedContent!!.amount} ${fioRequestContent.deserializedContent!!.tokenCode}"
        }

        tvFrom.text = fioRequestContent.payeeFioAddress
        tvTo.text = fioRequestContent.payerFioAddress
        tvMemo.text = fioRequestContent.deserializedContent!!.memo ?: ""
        tvDate.text = getDateString(convertToDate(fioRequestContent.timeStamp))
        btNextButton.setOnClickListener { finish() }
    }

    private fun getDateString(date: Date): String {
        val locale = resources.configuration.locale

        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
        val dateString = dayFormat.format(date)

        val hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)
        val timeString = hourFormat.format(date)

        return "$dateString $timeString"
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}