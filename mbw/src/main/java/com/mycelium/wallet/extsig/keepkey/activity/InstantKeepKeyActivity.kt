package com.mycelium.wallet.extsig.keepkey.activity

import android.app.Activity
import android.content.Intent
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ActivityInstantExtSigBinding
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wallet.extsig.common.activity.InstantExtSigActivity
import com.mycelium.wallet.extsig.keepkey.KeepKeyManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe

class InstantKeepKeyActivity : InstantExtSigActivity<KeepKeyManager>() {
    lateinit var binding: ActivityInstantExtSigBinding

    override fun setView() {
        setContentView(ActivityInstantExtSigBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)
        binding.ivConnectExtSig.setImageResource(R.drawable.connect_keepkey)
        binding.tvCaption.setText(R.string.keepkey_cold_storage_header)
        binding.tvDeviceType.setText(R.string.keepkey_name)
    }

    override fun initMasterseedManager(): KeepKeyManager =
        MbwManager.getInstance(this).keepKeyManager

    override fun getFirmwareUpdateDescription(): String =
        getString(R.string.keepkey_new_firmware_description)

    // Otto.EventBus does not traverse class hierarchy to find subscribers
    @Subscribe
    override fun onPinMatrixRequest(event: OnPinMatrixRequest?) {
        super.onPinMatrixRequest(event)
    }

    @Subscribe
    override fun onScanError(event: OnScanError) {
        super.onScanError(event)
    }

    @Subscribe
    override fun onStatusChanged(event: OnStatusChanged?) {
        super.onStatusChanged(event)
    }

    @Subscribe
    override fun onAccountFound(event: OnAccountFound) {
        super.onAccountFound(event)
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest?) {
        super.onPassphraseRequest(event)
    }

    companion object {
        fun callMe(currentActivity: Activity, requestCode: Int) {
            val intent = Intent(currentActivity, InstantKeepKeyActivity::class.java)
            currentActivity.startActivityForResult(intent, requestCode)
        }
    }
}
