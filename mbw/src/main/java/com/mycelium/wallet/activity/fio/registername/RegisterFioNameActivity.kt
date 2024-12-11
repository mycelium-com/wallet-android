package com.mycelium.wallet.activity.fio.registername

import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.fio.registername.viewmodel.RegisterFioNameViewModel
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.*
import fiofoundation.io.fiosdk.errors.FIOError
import fiofoundation.io.fiosdk.isFioAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import androidx.activity.viewModels

class RegisterFioNameActivity : AppCompatActivity(R.layout.activity_fio_add_address) {
    private val viewModel: RegisterFioNameViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = resources.getString(R.string.fio_register_address)
        }

        val fioEndpoints = MbwManager.getInstance(this).fioEndpoints
        // set default fee at first, it will be updated in async task
        viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), DEFAULT_FEE)
        val fioModule = MbwManager.getInstance(this).getWalletManager(false).getModuleById(FioModule.ID) as FioModule
        viewModel.addressWithDomain.observe(this, Observer { addressWithDomain ->
            if (viewModel.address.value!!.isNotEmpty()) {
                viewModel.isFioAddressValid.value = addressWithDomain.isFioAddress().also { addressValid ->
                    if (addressValid) {
                        CheckAddressAvailabilityTask(fioEndpoints, addressWithDomain, fioModule) { isAvailable ->
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
        (intent.getSerializableExtra(EXT_DOMAIN) as? FIODomain)?.let {
            viewModel.domain.value = it
        }
        (intent.getSerializableExtra(EXT_RENEW) as? Boolean)?.let {
            viewModel.isRenew.value = true
            (intent.getSerializableExtra(EXT_FIO_NAME) as? String)?.let {
                viewModel.addressWithDomain.value = it
                viewModel.address.value = it.split("@")[0]
            }
        }

        UpdateFeeTask(fioEndpoints, FIOApiEndPoints.FeeEndPoint.RegisterFioAddress.endpoint, fioModule) { feeInSUF ->
            if (feeInSUF != null) {
                viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), feeInSUF)
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    class UpdateFeeTask(
            private val fioEndpoints: FioEndpoints,
            private val endpointName: String,
            private val fioModule: FioModule,
            val listener: ((String?) -> Unit)) : AsyncTask<Void, Void, String?>() {
        override fun doInBackground(vararg args: Void): String? {
            return try {
                FioBlockchainService.getFeeByEndpoint(fioEndpoints, endpointName).toString()
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                Logger.getLogger(UpdateFeeTask::class.simpleName).log(Level.WARNING, "failed to get fee: ${e.localizedMessage}")
                null
            }
        }

        override fun onPostExecute(result: String?) {
            listener(result)
        }
    }

    class CheckAddressAvailabilityTask(
            private val fioEndpoints: FioEndpoints,
            private val addressWithDomainOrDomain: String,
            private val fioModule: FioModule,
            val listener: ((Boolean?) -> Unit)) : AsyncTask<Void, Void, Boolean?>() {
        override fun doInBackground(vararg args: Void): Boolean? {
            return try {
                FioBlockchainService.isFioNameOrDomainAvailable(fioEndpoints, addressWithDomainOrDomain)
            } catch (e: Exception) {
                if (e is FIOError) {
                    fioModule.addFioServerLog(e.toJson())
                }
                Logger.getLogger(CheckAddressAvailabilityTask::class.simpleName).log(Level.WARNING, "failed to check fio name availability: ${e.localizedMessage}")
                null
            }
        }

        override fun onPostExecute(result: Boolean?) {
            listener(result)
        }
    }

    companion object {
        private const val EXT_ACCOUNT = "account"
        private const val EXT_DOMAIN = "domain"
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

        fun start(context: Context, account: UUID, domain: FIODomain) {
            context.startActivity(Intent(context, RegisterFioNameActivity::class.java)
                    .putExtra(EXT_ACCOUNT, account)
                    .putExtra(EXT_DOMAIN, domain))
        }

        fun startRenew(context: Context, account: UUID, fioName: String) {
            context.startActivity(Intent(context, RegisterFioNameActivity::class.java)
                    .putExtra(EXT_ACCOUNT, account)
                    .putExtra(EXT_FIO_NAME, fioName)
                    .putExtra(EXT_RENEW, true))
        }
    }
}
