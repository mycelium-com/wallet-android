package com.mycelium.wallet.activity.fio.registerdomain

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
import com.mycelium.wallet.activity.fio.registerdomain.viewmodel.RegisterFioDomainViewModel
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.coins.isFioDomain
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIOApiEndPoints
import java.util.*

class RegisterFIODomainActivity : AppCompatActivity() {
    private lateinit var viewModel: RegisterFioDomainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_fio_domain)
        supportActionBar?.run {
            setHomeAsUpIndicator(R.drawable.ic_back_arrow)
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            title = "Register FIO Domain"
        }
        supportFragmentManager.beginTransaction().add(R.id.container, RegisterFioDomainFragment()).commit()

        viewModel = ViewModelProviders.of(this).get(RegisterFioDomainViewModel::class.java)

        // set default fee at first, it will be updated in async task
        viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), RegisterFioNameActivity.DEFAULT_FEE)
        viewModel.domain.observe(this, Observer { domain ->
            if (viewModel.domain.value!!.isNotEmpty()) {
                viewModel.isFioDomainValid.value = domain.isFioDomain().also { domainValid ->
                    if (domainValid) {
                        Log.i("asdaf", "asdaf checking avail. for $domain")
                        RegisterFioNameActivity.CheckAddressAvailabilityTask(domain) { isAvailable ->
                            if (isAvailable != null) {
                                viewModel.isFioDomainAvailable.value = isAvailable
                            } else {
                                viewModel.isFioServiceAvailable.value = false
                            }
                        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                }
            }
        })
        (intent.getSerializableExtra("account") as? UUID)?.let {
            val walletManager = MbwManager.getInstance(this).getWalletManager(false)
            viewModel.fioAccountToRegisterName.value = walletManager.getAccount(it) as? FioAccount
        }
        RegisterFioNameActivity.UpdateFeeTask(FIOApiEndPoints.FeeEndPoint.RegisterFioDomain.endpoint) { feeInSUF ->
            if (feeInSUF != null) {
                viewModel.registrationFee.value = Value.valueOf(Utils.getFIOCoinType(), feeInSUF)
                Log.i("asdaf", "asdaf updated fee: $feeInSUF, viewModel.registrationFee: ${viewModel.registrationFee.value}")
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
}
