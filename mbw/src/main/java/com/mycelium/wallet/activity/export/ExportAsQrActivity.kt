package com.mycelium.wallet.activity.export

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.ExportAsQrActivityBinding
import com.mycelium.wapi.wallet.ExportableAccount
import kotlinx.android.synthetic.main.export_as_qr_activity.*

class ExportAsQrActivity : AppCompatActivity() {

    private lateinit var viewModel: ExportAsQrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountData = intent.getSerializableExtra(ACCOUNT_DATA) as ExportableAccount.Data
        val isHdAccount = intent.getSerializableExtra(BTC_MULTIADDRESS) as Boolean

        if (!accountData.publicData.isPresent && !accountData.privateData.isPresent) {
            finish()
            return
        }

        val viewModelProvider = ViewModelProviders.of(this)
        viewModel = viewModelProvider.get(ExportAsQrViewModel::class.java)
        if (!viewModel.isInitialized()) {
            viewModel.init(accountData, isHdAccount)
        }

        // Inflate view and obtain an instance of the binding class.
        val binding = DataBindingUtil.setContentView<ExportAsQrActivityBinding>(this, R.layout.export_as_qr_activity)
        binding.viewModel = viewModel
        binding.activity = this
        binding.setLifecycleOwner(this)

        // Prevent the OS from taking screenshots of this activity
        Utils.preventScreenshots(this)

        subscribeQR()
    }

    // sets key as qr and as textView
    private fun subscribeQR() =
        viewModel.getAccountDataString().observe(this, Observer { accountData -> ivQrCode.qrCode = accountData})

    override fun onPause() {
        // This way we finish the activity when home is pressed, so you are forced
        // to reenter the PIN to see the QR-code again
        finish()
        super.onPause()
    }

    companion object {
        private const val ACCOUNT_DATA = "accountData"
        private const val BTC_MULTIADDRESS = "btcMultiAddress"

        @JvmStatic
        fun callMe(currentActivity: Activity, accountData: ExportableAccount.Data, btcMultiAddress: Boolean) {
            val intent = Intent(currentActivity, ExportAsQrActivity::class.java)
            intent.putExtra(ACCOUNT_DATA, accountData)
            intent.putExtra(BTC_MULTIADDRESS, btcMultiAddress)
            currentActivity.startActivity(intent)
        }
    }
}