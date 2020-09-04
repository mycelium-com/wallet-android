package com.mycelium.wallet.activity.fio.mapaddress

import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import kotlinx.android.synthetic.main.activity_fio_add_address.*
import java.util.*

class FIOAddAddressActivity : AppCompatActivity() {
    private lateinit var viewModel: FIORegisterAddressViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fio_add_address)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            title = "Register FIO Address"
        }

        viewModel = ViewModelProviders.of(this).get(FIORegisterAddressViewModel::class.java)
        val accountid = intent.getSerializableExtra("account") as UUID
        val fioAccount = MbwManager.getInstance(this.application).getWalletManager(false).getAccount(accountid) as FioAccount
        viewModel.account.value = fioAccount
        viewModel.address.observe(this, Observer {
            viewModel.addressWithDomain.value = "${viewModel.address.value}@${viewModel.domain.value}"
        })
        viewModel.remainingBalance.value = fioAccount.accountBalance.spendable.toString()
        UpdateFeeTask(fioAccount) { feeInSUF ->
            val coinType = fioAccount.coinType

            val feeValue = if (feeInSUF != null) {
                Value.valueOf(coinType, feeInSUF)
            } else {
                Value.valueOf(coinType, DEFAULT_FEE)
            }
            viewModel.registrationFee.value = feeValue.toString()
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    class UpdateFeeTask(
            val account: FioAccount,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                account.getFeeByEndpoint(FIOApiEndPoints.FeeEndPoint.RegisterFioAddress).toString()
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }

    companion object {
        const val DEFAULT_FEE = "7100000000" // 7.1 FIO
    }
}
