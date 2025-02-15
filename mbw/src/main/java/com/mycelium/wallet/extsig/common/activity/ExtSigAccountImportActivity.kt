package com.mycelium.wallet.extsig.common.activity

import android.app.LoaderManager
import android.app.ProgressDialog
import android.content.AsyncTaskLoader
import android.content.Context
import android.content.Intent
import android.content.Loader
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.HdAccountSelectorActivity.HdAccountWrapper
import com.mycelium.wallet.databinding.ActivityInstantExtSigBinding
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager.OnPinMatrixRequest
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignaturesAccountConfig
import com.squareup.otto.Subscribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

abstract class ExtSigAccountImportActivity : ExtSigAccountSelectorActivity(),
    LoaderManager.LoaderCallbacks<UUID?> {
    lateinit var binding: ActivityInstantExtSigBinding

    override fun accountClickListener(): OnItemClickListener? {
        return object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                val bundle = Bundle()
                bundle.putSerializable(
                    ITEM_WRAPPER,
                    adapterView.getItemAtPosition(i) as HdAccountWrapper?
                )
                getLoaderManager().initLoader<UUID?>(1, bundle, this@ExtSigAccountImportActivity)
                    .forceLoad()

                val dialog = ProgressDialog(this@ExtSigAccountImportActivity)
                dialog.setCancelable(false)
                dialog.setCanceledOnTouchOutside(false)
                dialog.setTitle(getString(R.string.hardware_account_create))
                dialog.setMessage(getString(R.string.please_wait_hardware))
                dialog.show()
            }
        }
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<UUID?> =
        AccountCreationLoader(
            applicationContext,
            (bundle!!.getSerializable(ITEM_WRAPPER) as HdAccountWrapper?)!!,
            (masterseedScanManager as ExternalSignatureDeviceManager?)!!
        )

    override fun onLoadFinished(loader: Loader<UUID?>?, uuid: UUID?) {
        val result = Intent()
        result.putExtra("account", uuid)
        setResult(RESULT_OK, result)
        finish()
    }

    override fun onLoaderReset(loader: Loader<UUID?>?) {}

    class AccountCreationLoader(
        context: Context,
        val item: HdAccountWrapper,
        val masterseedScanManager: ExternalSignatureDeviceManager
    ) : AsyncTaskLoader<UUID?>(context) {

        override fun loadInBackground(): UUID? {
            // create the new account and get the uuid of it

            val mbwManager = MbwManager.getInstance(context)

            val acc = mbwManager.getWalletManager(false)
                .createAccounts(
                    ExternalSignaturesAccountConfig(
                        item.publicKeyNodes!!,
                        masterseedScanManager,
                        item.accountHdKeysPaths!!.iterator().next().lastIndex
                    )
                )[0]

            // Mark this account as backup warning ignored
            mbwManager.metadataStorage
                .setOtherAccountBackupState(acc, MetadataStorage.BackupState.IGNORED)

            return acc
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
                lifecycleScope.launch(Dispatchers.Default) {
                    val nextAccount = masterseedScanManager?.getNextUnusedAccounts()

                    val mbwManager =
                        MbwManager.getInstance(this@ExtSigAccountImportActivity)

                    if (nextAccount?.isNotEmpty() == true) {
                        val acc = mbwManager.getWalletManager(false)
                            .createAccounts(
                                ExternalSignaturesAccountConfig(
                                    nextAccount,
                                    (masterseedScanManager as ExternalSignatureDeviceManager?)!!,
                                    nextAccount[0].index
                                )
                            )[0]

                        mbwManager.metadataStorage.setOtherAccountBackupState(
                            acc, MetadataStorage.BackupState.IGNORED
                        )

                        withContext(Dispatchers.Main) {
                            setResult(RESULT_OK, Intent().putExtra("account", acc))
                            finish()
                        }
                    }
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

    companion object {
        private const val ITEM_WRAPPER = "ITEM_WRAPPER"
    }
}
