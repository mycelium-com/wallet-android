package com.mycelium.wallet.extsig.trezor.activity

import android.app.Activity
import android.content.Intent
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wallet.extsig.common.activity.ExtSigAccountImportActivity
import com.mycelium.wallet.extsig.trezor.TrezorManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe

class TrezorAccountImportActivity : ExtSigAccountImportActivity<TrezorManager>() {
    override fun initMasterseedManager(): TrezorManager =
        MbwManager.getInstance(this).trezorManager

    override fun setView() {
        super.setView()
        binding.ivConnectExtSig.setImageResource(R.drawable.connect_trezor)
        binding.tvDeviceType.setText(R.string.trezor_name)
    }

    override fun getFirmwareUpdateDescription(): String =
        getString(R.string.trezor_new_firmware_description)

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
        @JvmStatic
        fun callMe(currentActivity: Activity, requestCode: Int) {
            currentActivity.startActivityForResult(
                Intent(currentActivity, TrezorAccountImportActivity::class.java),
                requestCode
            )
        }
    }
}
