package com.mycelium.wallet.extsig.trezor.activity

import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.databinding.SignExtSigTransactionActivityBinding
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnButtonRequest
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnStatusUpdate
import com.mycelium.wallet.extsig.common.activity.ExtSigSignTransactionActivity
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe

class TrezorSignTransactionActivity : ExtSigSignTransactionActivity(), MasterseedPasswordSetter {

    override val extSigManager: ExternalSignatureDeviceManager
        get() = MbwManager.getInstance(this).trezorManager


    override fun setView() {
        setContentView(SignExtSigTransactionActivityBinding.inflate(layoutInflater).apply {
            binding = this
        }.getRoot())
        binding.ivConnectExtSig.setImageResource(R.drawable.connect_trezor)
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest) {
        super.onPassphraseRequest(event)
    }

    @Subscribe
    override fun onScanError(event: OnScanError) {
        super.onScanError(event)
    }

    @Subscribe
    override fun onStatusUpdate(event: OnStatusUpdate) {
        super.onStatusUpdate(event)
    }

    @Subscribe
    override fun onPinMatrixRequest(event: OnPinMatrixRequest) {
        super.onPinMatrixRequest(event)
    }

    @Subscribe
    override fun onButtonRequest(event: OnButtonRequest) {
        super.onButtonRequest(event)
    }

    @Subscribe
    override fun onStatusChanged(event: OnStatusChanged) {
        super.onStatusChanged(event)
    }
}
