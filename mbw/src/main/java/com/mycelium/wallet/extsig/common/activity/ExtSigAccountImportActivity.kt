package com.mycelium.wallet.extsig.common.activity

import android.app.ProgressDialog
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.core.view.isVisible
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.HdAccountSelectorActivity.HdAccountWrapper
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wallet.databinding.ActivityInstantExtSigBinding
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.squareup.otto.Subscribe

abstract class ExtSigAccountImportActivity<AccountScanManager : AbstractAccountScanManager> :
    ExtSigAccountSelectorActivity<AccountScanManager>() {
    lateinit var binding: ActivityInstantExtSigBinding

    override fun accountClickListener(): OnItemClickListener? =
        object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                val item = adapterView.getItemAtPosition(i) as HdAccountWrapper
                createAccountAndFinish(
                    item.publicKeyNodes,
                    item.accountHdKeysPaths.last().lastIndex
                )
                ProgressDialog(this@ExtSigAccountImportActivity).apply {
                    setCancelable(false)
                    setCanceledOnTouchOutside(false)
                    setTitle(getString(R.string.hardware_account_create))
                    setMessage(getString(R.string.please_wait_hardware))
                }.show()
            }
        }

    override fun updateUi() {
        super.updateUi()
        binding.btNextAccount.setEnabled(masterseedScanManager?.currentAccountState == AccountScanManager.AccountStatus.done)
    }

    override fun setView() {
        setContentView(ActivityInstantExtSigBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)
        binding.tvCaption.text = getString(R.string.ext_sig_import_account_caption)
        binding.tvSelectAccount.text = getString(R.string.ext_sig_select_account_to_import)
        binding.btNextAccount.isVisible = true

        binding.btNextAccount.setOnClickListener {
            Utils.showSimpleMessageDialog(
                this@ExtSigAccountImportActivity,
                getString(R.string.ext_sig_next_unused_account_info)
            ) {
                val nextAccount = masterseedScanManager?.getNextUnusedAccounts()
                if (nextAccount?.isNotEmpty() == true) {
                    createAccountAndFinish(nextAccount, nextAccount.first().index)
                }
            }
        }
    }

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

    override fun onAccountFound(event: OnAccountFound) {
        super.onAccountFound(event)
    }

    @Subscribe
    override fun onPassphraseRequest(event: OnPassphraseRequest?) {
        super.onPassphraseRequest(event)
    }
}
