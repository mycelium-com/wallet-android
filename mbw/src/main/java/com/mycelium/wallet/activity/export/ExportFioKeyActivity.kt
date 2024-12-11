package com.mycelium.wallet.activity.export

import android.os.Bundle
import android.view.View.*
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.export.viewmodel.ExportFioAsQrViewModel
import com.mycelium.wallet.databinding.ActivityExportFioKeyBinding
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.fio.FioKeyManager
import androidx.activity.viewModels

class ExportFioKeyActivity : AppCompatActivity() {
    private val viewModel: ExportFioAsQrViewModel by viewModels()
    private lateinit var binding: ActivityExportFioKeyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.hide()
        val mbwManager = MbwManager.getInstance(this)
        val fioKeyManager = FioKeyManager(mbwManager.masterSeedManager)
        val publicKey = fioKeyManager.getFioPublicKey((mbwManager.selectedAccount as HDAccount).accountIndex)
        val formatPubKey = fioKeyManager.formatPubKey(publicKey)

        if (!viewModel.isInitialized()) {
            viewModel.init(ExportableAccount.Data(Optional.absent(), mapOf(BipDerivationType.BIP44 to formatPubKey)))
        }

        setContentView(ActivityExportFioKeyBinding.inflate(layoutInflater).apply {
            binding = this
            this.viewModel = viewModel as ExportFioAsQrViewModel
            this.activity = this@ExportFioKeyActivity
        }.root)
        binding.lifecycleOwner = this
        subscribeQR()
        binding.layoutQR.tvWarning.visibility = GONE
        binding.layoutShare.btShare.text = getString(R.string.share_fio_public_key)
    }

    // sets key as qr and as textView
    private fun subscribeQR() =
            viewModel.getAccountDataString().observe(this, Observer { accountData ->
                binding.layoutQR.ivQrCode.qrCode = accountData
            })

}
