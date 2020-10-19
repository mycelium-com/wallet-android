package com.mycelium.wallet.activity.fio.registername

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.FioTransactionHistoryService
import fiofoundation.io.fiosdk.isFioAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import kotlinx.android.synthetic.main.activity_fio_add_address.*
import java.util.*

class RegisterFioNameActivity : AppCompatActivity(R.layout.activity_fio_add_address) {
    private lateinit var viewModel: RegisterFioNameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = resources.getString(R.string.fio_register_address)
        }

        viewModel = ViewModelProviders.of(this).get(RegisterFioNameViewModel::class.java)

        // set default fee at first, it will be updated in async task
        viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), DEFAULT_FEE)
        viewModel.addressWithDomain.observe(this, Observer { addressWithDomain ->
            if (viewModel.address.value!!.isNotEmpty()) {
                viewModel.isFioAddressValid.value = addressWithDomain.isFioAddress().also { addressValid ->
                    if (addressValid) {
                        Log.i("asdaf", "asdaf checking avail. for $addressWithDomain")
                        CheckAddressAvailabilityTask(addressWithDomain) { isAvailable ->
                            if (isAvailable != null) {
                                viewModel.isFioAddressAvailable.value = isAvailable
                            } else {
                                viewModel.isFioServiceAvailable.value = false
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                }
            }
        })
        (intent.getSerializableExtra(EXT_ACCOUNT) as? UUID)?.let {
            val walletManager = MbwManager.getInstance(this).getWalletManager(false)
            viewModel.fioAccountToRegisterName.value = walletManager.getAccount(it) as? FioAccount
        }
        (intent.getSerializableExtra(EXT_RENEW) as? Boolean)?.let {
            viewModel.isRenew.value = true
            (intent.getSerializableExtra(EXT_FIO_NAME) as? String)?.let {
                viewModel.addressWithDomain.value = it
                viewModel.address.value = it.split("@")[0]
            }
        }
        UpdateFeeTask(FIOApiEndPoints.FeeEndPoint.RegisterFioAddress.endpoint) { feeInSUF ->
            if (feeInSUF != null) {
                viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), feeInSUF)
                Log.i("asdaf", "asdaf updated fee: $feeInSUF, viewModel.registrationFee: ${viewModel.registrationFee.value}")
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)

        if (viewModel.isRenew.value!!) {
            supportFragmentManager.beginTransaction().add(R.id.container, RenewFioNameFragment()).commit()
        } else {
            viewModel.address.observe(this, Observer {
                viewModel.addressWithDomain.value = "${viewModel.address.value}@${viewModel.domain.value!!.domain}"
            })
            viewModel.domain.observe(this, Observer {
                viewModel.addressWithDomain.value = "${viewModel.address.value}@${viewModel.domain.value!!.domain}"
            })
            supportFragmentManager.beginTransaction().add(R.id.container, RegisterFioNameFragment()).commit()
        }
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
            private val endpoint: String,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                FioTransactionHistoryService.getFeeByEndpoint(Utils.getFIOCoinType(),
                        endpoint).toString()
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }

    class CheckAddressAvailabilityTask(
            private val addressWithDomain: String,
            val listener: ((Boolean?) -> Unit)) : AsyncTask<Void, Void, Boolean?>() {
        override fun doInBackground(vararg args: Void): Boolean? {
            return try {
                FioTransactionHistoryService.isFioNameOrDomainAvailable(Utils.getFIOCoinType(),
                        addressWithDomain)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: Boolean?) {
            listener(result)
        }
    }

    companion object {
        private const val EXT_ACCOUNT = "account"
        private const val EXT_FIO_NAME = "fioName"
        private const val EXT_RENEW = "renew"
        const val DEFAULT_FEE = "10000000000" // 10 FIO

        fun start(context: Context) {
            context.startActivity(Intent(context, RegisterFioNameActivity::class.java))
        }

        @JvmStatic
        fun start(context: Context, account: UUID) {
            context.startActivity(Intent(context, RegisterFioNameActivity::class.java)
                    .putExtra(EXT_ACCOUNT, account))
        }

        fun startRenew(context: Context, account: UUID, fioName: String) {
            context.startActivity(Intent(context, RegisterFioNameActivity::class.java)
                    .putExtra(EXT_ACCOUNT, account)
                    .putExtra(EXT_FIO_NAME, fioName)
                    .putExtra(EXT_RENEW, true))
        }
    }
}
