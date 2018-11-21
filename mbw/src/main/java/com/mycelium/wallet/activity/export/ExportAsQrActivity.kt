package com.mycelium.wallet.activity.export

import android.app.Activity
import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.databinding.DataBindingUtil
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.databinding.ExportAsQrActivityBinding
import com.mycelium.wallet.databinding.ExportAsQrBtcHdActivityBinding
import com.mycelium.wallet.databinding.ExportAsQrBtcSaActivityBinding
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.single.SingleAddressAccount
import kotlinx.android.synthetic.main.export_as_qr_activity_qr.*
import java.util.*

class ExportAsQrActivity : AppCompatActivity() {

    private lateinit var viewModel: ExportAsQrViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val accountData = intent.getSerializableExtra(ACCOUNT_DATA) as ExportableAccount.Data
        val accountUUID = intent.getSerializableExtra(ACCOUNT_UUID) as UUID

        val account = MbwManager.getInstance(this)
                .getWalletManager(false)
                .getAccount(accountUUID)

        if (!accountData.publicData.isPresent && !accountData.privateData.isPresent) {
            finish()
            return
        }

        val viewModelProvider = ViewModelProviders.of(this)
        viewModel = when {
            account is HDAccount && (accountData.publicDataMap?.size ?: 0 > 1) ->
                viewModelProvider.get(ExportAsQrBtcHDViewModel::class.java)
            account is SingleAddressAccount && account.availableAddressTypes.size > 1 ->
                viewModelProvider.get(ExportAsQrBtcSAViewModel::class.java)
            else -> viewModelProvider.get(ExportAsQrViewModel::class.java)
        }

        if (!viewModel.isInitialized()) {
            viewModel.init(accountData)
        }

        // Inflate view and obtain an instance of the binding class.

        val binding = when {
            account is HDAccount && accountData.publicDataMap != null && accountData.publicDataMap!!.size > 1 -> {
                val binding = DataBindingUtil.setContentView<ExportAsQrBtcHdActivityBinding>(this, R.layout.export_as_qr_btc_hd_activity)
                binding.viewModel = viewModel as ExportAsQrMultiKeysViewModel
                binding.activity = this
                binding
            }
            account is SingleAddressAccount && accountData.privateData.isPresent -> {
                val binding = DataBindingUtil.setContentView<ExportAsQrBtcSaActivityBinding>(this, R.layout.export_as_qr_btc_sa_activity)
                binding.viewModel = viewModel as ExportAsQrMultiKeysViewModel
                binding.activity = this
                binding
            }
            else -> {
                val binding = DataBindingUtil.setContentView<ExportAsQrActivityBinding>(this, R.layout.export_as_qr_activity)
                binding.viewModel = viewModel
                binding.activity = this
                binding
            }
        }
        binding.setLifecycleOwner(this)

        // Prevent the OS from taking screenshots of this activity
        Utils.preventScreenshots(this)

        subscribeQR()
    }

    // sets key as qr and as textView
    private fun subscribeQR() =
            viewModel.getAccountDataString().observe(this, Observer { accountData -> ivQrCode.qrCode = accountData })

    override fun onPause() {
        // This way we finish the activity when home is pressed, so you are forced
        // to reenter the PIN to see the QR-code again
        finish()
        super.onPause()
    }

    companion object {
        private const val ACCOUNT_DATA = "accountData"
        private const val ACCOUNT_UUID = "accountUUID"

        @JvmStatic
        fun callMe(currentActivity: Activity, accountData: ExportableAccount.Data, account: WalletAccount) {
            val intent = Intent(currentActivity, ExportAsQrActivity::class.java)
            intent.putExtra(ACCOUNT_DATA, accountData)
            intent.putExtra(ACCOUNT_UUID, account.id)
            currentActivity.startActivity(intent)
        }
    }
}