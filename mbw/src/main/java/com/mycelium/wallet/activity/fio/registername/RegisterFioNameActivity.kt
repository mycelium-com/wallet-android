package com.mycelium.wallet.activity.fio.registername

import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import fiofoundation.io.fiosdk.isFioAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import kotlinx.android.synthetic.main.activity_fio_add_address.*
import java.util.*

class RegisterFioNameActivity : AppCompatActivity() {
    private lateinit var viewModel: RegisterFioNameViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fio_add_address)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            title = resources.getString(R.string.fio_register_address)
        }
        supportFragmentManager.beginTransaction().add(R.id.container, RegisterFioNameFragment()).commit()

        viewModel = ViewModelProviders.of(this).get(RegisterFioNameViewModel::class.java)

        val accountid = intent.getSerializableExtra("account") as UUID
        val fioAccount = MbwManager.getInstance(this.application).getWalletManager(false).getAccount(accountid) as FioAccount
        viewModel.account.value = fioAccount
        // set default fee at first, it will be updated in async task
        viewModel.registrationFee.value = Value.valueOf(fioAccount.coinType, DEFAULT_FEE)
        viewModel.address.observe(this, Observer {
            viewModel.addressWithDomain.value = "${viewModel.address.value}${viewModel.domain.value}"
        })
        viewModel.addressWithDomain.observe(this, Observer { addressWithDomain ->
            if (viewModel.address.value!!.isNotEmpty()) {
                viewModel.isFioAddressValid.value = addressWithDomain.isFioAddress().also { addressValid ->
                    if (addressValid) {
                        Log.i("asdaf", "asdaf checking avail. for $addressWithDomain")
                        CheckAddressAvailabilityTask(fioAccount, addressWithDomain) { isAvailable ->
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
        viewModel.remainingBalance.value = fioAccount.accountBalance.spendable
        UpdateFeeTask(fioAccount) { feeInSUF ->
            val coinType = fioAccount.coinType

            if (feeInSUF != null) {
                viewModel.registrationFee.value = Value.valueOf(coinType, feeInSUF)
            }
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

    class CheckAddressAvailabilityTask(
            val account: FioAccount,
            private val addressWithDomain: String,
            val listener: ((Boolean?) -> Unit)) : AsyncTask<Void, Void, Boolean?>() {
        override fun doInBackground(vararg args: Void): Boolean? {
            return try {
                account.isFIOAddressAvailable(addressWithDomain)
            } catch (e: Exception) {
                null
            }
        }

        override fun onPostExecute(result: Boolean?) {
            listener(result)
        }
    }

    companion object {
        const val DEFAULT_FEE = "10000000000" // 10 FIO
    }
}
