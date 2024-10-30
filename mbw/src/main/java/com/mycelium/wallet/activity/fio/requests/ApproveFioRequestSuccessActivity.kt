package com.mycelium.wallet.activity.fio.requests

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.txdetails.TransactionDetailsActivity
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.ACCOUNT
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.AMOUNT
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.CONVERTED_AMOUNT
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.DATE
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.FEE
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.FROM
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.MEMO
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.TO
import com.mycelium.wallet.activity.fio.requests.ApproveFioRequestActivity.Companion.TXID
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FioSendRequestStatusActivityBinding
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.Value
import java.text.DateFormat
import java.util.*


class ApproveFioRequestSuccessActivity : AppCompatActivity() {
    private lateinit var walletManager: WalletManager
    lateinit var binding: FioSendRequestStatusActivityBinding

    companion object {
        fun start(activity: Activity, amount: Value,
                  convertedAmount: String,
                  fee: Value,
                  date: Long,
                  from: String,
                  to: String, memo: String,
                  txid: ByteArray,
                  accountId: UUID) {
            with(Intent(activity, ApproveFioRequestSuccessActivity::class.java)) {
                putExtra(AMOUNT, amount)
                putExtra(CONVERTED_AMOUNT, convertedAmount)
                putExtra(FEE, fee)
                putExtra(DATE, date)
                putExtra(FROM, from)
                putExtra(TO, to)
                putExtra(MEMO, memo)
                putExtra(TXID, txid)
                putExtra(ACCOUNT, accountId)
                activity.startActivity(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(FioSendRequestStatusActivityBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)

        supportActionBar?.run {
            title = "Success!"
        }
        walletManager = MbwManager.getInstance(this.application).getWalletManager(false)
        binding.tvAmount.text = (intent.getSerializableExtra(AMOUNT) as Value).toStringWithUnit()
        binding.tvConvertedAmount.text = " ~ ${intent.getStringExtra(CONVERTED_AMOUNT)}"
        binding.tvMinerFee.text = (intent.getSerializableExtra(FEE) as Value).toStringWithUnit()
        binding.tvFrom.text = intent.getStringExtra(FROM)
        val date = intent.getLongExtra(DATE, -1)
        binding.tvTo.text = intent.getStringExtra(TO)
        binding.tvMemo.text = intent.getStringExtra(MEMO)
        val accountId = intent.getSerializableExtra(ACCOUNT) as UUID
        val account = walletManager.getAccount(accountId)
        val txid = intent.getByteArrayExtra(TXID)
        binding.btNextButton.setOnClickListener { finish() }
        try {
            if (txid?.isNotEmpty() != true) {
                if (date != -1L) {
                    binding.tvDate.text = getDateString(date)
                }
                binding.tvTxDetailsLink.isVisible = false
            } else {
                val txTimestamp = account!!.getTxSummary(txid)?.timestamp
                binding.tvDate.text = getDateString(txTimestamp!!)
                binding.tvTxDetailsLink.setOnClickListener {
                    val intent: Intent = Intent(this, TransactionDetailsActivity::class.java)
                            .putExtra(TransactionDetailsActivity.EXTRA_TXID, txid)
                            .putExtra(TransactionDetailsActivity.ACCOUNT_ID, accountId)
                    startActivity(intent)
                    finish()
                }
            }
        } catch (ex: Exception) {
            //error read transaction
        }
    }

    private fun getDateString(timestamp: Long): String {
        val date = Date(timestamp * 1000L)
        val locale = resources.configuration.locale

        val dayFormat = DateFormat.getDateInstance(DateFormat.LONG, locale)
        val dateString = dayFormat.format(date)

        val hourFormat = DateFormat.getTimeInstance(DateFormat.LONG, locale)
        val timeString = hourFormat.format(date)

        return "$dateString $timeString"
    }
}