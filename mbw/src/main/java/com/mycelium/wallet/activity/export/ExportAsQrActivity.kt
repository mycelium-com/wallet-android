package com.mycelium.wallet.activity.export

import android.app.Activity
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.View
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.QrImageView
import com.mycelium.wallet.databinding.ExportAsQrActivityBinding
import com.mycelium.wapi.wallet.ExportableAccount

class ExportAsQrActivity : AppCompatActivity() {

    private lateinit var viewModel: ExportAsQrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val mbwManager = MbwManager.getInstance(application)

        val accountData = intent.getSerializableExtra(ACCOUNT_DATA) as ExportableAccount.Data
        val isHdAccount = intent.getSerializableExtra(ACCOUNT_HD) as Boolean

        if (!accountData.publicData.isPresent && !accountData.privateData.isPresent) {
            finish()
            return
        }

        val viewModelProvider = ViewModelProviders.of(this)
        viewModel = viewModelProvider.get(ExportAsQrViewModel::class.java)
        viewModel.init(accountData, isHdAccount)

        // Inflate view and obtain an instance of the binding class.
        val binding = DataBindingUtil.setContentView<ExportAsQrActivityBinding>(this, R.layout.export_as_qr_activity)
        binding.viewModel = viewModel
        binding.activity = this
        binding.setLifecycleOwner(this)

        // Prevent the OS from taking screenshots of this activity
        Utils.preventScreenshots(this)
    }

    fun setQrode()
    {
        // Set QR code
        val iv = findViewById<View>(R.id.ivQrCode) as QrImageView
        iv.qrCode = viewModel.getData()
    }

    override fun onPause() {
        // This way we finish the activity when home is pressed, so you are forced
        // to reenter the PIN to see the QR-code again
        finish()
        super.onPause()
    }

    companion object {
        private const val ACCOUNT_DATA = "accountData"
        private const val ACCOUNT_HD = "accountHd"

        @JvmStatic
        fun callMe(currentActivity: Activity, accountData: ExportableAccount.Data, isHdAccount : Boolean) {
            val intent = Intent(currentActivity, ExportAsQrActivity::class.java)
            intent.putExtra(ACCOUNT_DATA, accountData)
            intent.putExtra(ACCOUNT_HD, isHdAccount)
            currentActivity.startActivity(intent)
        }
    }
}