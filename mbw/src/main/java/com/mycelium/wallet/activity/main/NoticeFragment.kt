package com.mycelium.wallet.activity.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.mycelium.wallet.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.VerifyBackupActivity
import com.mycelium.wallet.databinding.MainNoticeFragmentBinding
import com.mycelium.wallet.event.AccountChanged
import com.mycelium.wallet.event.BalanceChanged
import com.mycelium.wallet.event.SelectedAccountChanged
import com.mycelium.wallet.persistence.MetadataStorage.BackupState
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.eth.EthAccount
import com.squareup.otto.Subscribe
import java.util.concurrent.TimeUnit

class NoticeFragment : Fragment() {
    private enum class Notice {
        BACKUP_MISSING,
        SINGLEKEY_BACKUP_MISSING, SINGLEKEY_VERIFY_MISSING,
        RESET_PIN_AVAILABLE, RESET_PIN_IN_PROGRESS, NONE
    }

    private var _mbwManager: MbwManager? = null
    private var _notice: Notice? = null
    private var sharedPreferences: SharedPreferences? = null

    var binding: MainNoticeFragmentBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(false)
        super.onCreate(savedInstanceState)
        sharedPreferences = activity!!.getSharedPreferences(NOTICE, Context.MODE_PRIVATE)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = MainNoticeFragmentBinding.inflate(inflater).apply{
        binding = this
    }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding?.backupMissingLayout?.setOnClickListener {
            noticeClickListener.onClick(null)
        }
        binding?.btnSecond?.setOnClickListener {
            secondButtonClick()
        }
        binding?.cbRisks?.setOnCheckedChangeListener { _, isChecked ->
            binding?.btnSecond?.isEnabled = isChecked
        }
    }

    override fun onAttach(activity: Activity) {
        _mbwManager = MbwManager.getInstance(activity)
        super.onAttach(activity)
    }

    override fun onStart() {
        MbwManager.getEventBus().register(this)
        _notice = determineNotice()
        binding?.btWarning?.setOnClickListener(warningClickListener)
        binding?.btPinResetNotice?.setOnClickListener(noticeClickListener)
        updateUi()
        super.onStart()
    }

    override fun onStop() {
        MbwManager.getEventBus().unregister(this)
        super.onStop()
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun determineNotice(): Notice {
        val account = _mbwManager!!.selectedAccount
        val meta = _mbwManager!!.metadataStorage

        val resetPinRemainingBlocksCount = _mbwManager!!.resetPinRemainingBlocksCount
        // Check first if a Pin-Reset is now possible
        if (resetPinRemainingBlocksCount.isPresent && resetPinRemainingBlocksCount.get() == 0) {
            return Notice.RESET_PIN_AVAILABLE
        }

        // Then check if a Pin-Reset is in process
        if (resetPinRemainingBlocksCount.isPresent) {
            return Notice.RESET_PIN_IN_PROGRESS
        }

        // First check if we have HD accounts with funds, but have no master seed backup
        if (meta.masterSeedBackupState != BackupState.VERIFIED) {
            if (account is HDAccount || account is EthAccount) {
                /*
              We have an HD account and no master seed backup, tell the user to act
              We shouldn't check balance, in security reason user should create backup
              and then receive money otherwise he can lost money in case delete application or lost phone
             */
                return Notice.BACKUP_MISSING
            }
        }

        // Then check if there are some SingleAddressAccounts with funds on it
        if ((account is ColuAccount || account is SingleAddressAccount)
            && account.canSpend()
        ) {
            val state = meta.getOtherAccountBackupState(account.id)
            if (state == BackupState.NOT_VERIFIED) {
                return Notice.SINGLEKEY_VERIFY_MISSING
            } else if (state != BackupState.VERIFIED
                && state != BackupState.IGNORED
            ) {
                return Notice.SINGLEKEY_BACKUP_MISSING
            }
        }

        return Notice.NONE
    }


    fun secondButtonClick() {
        val account = _mbwManager!!.selectedAccount
        when (_notice) {
            Notice.SINGLEKEY_VERIFY_MISSING -> showSingleKeyBackupWarning()
            Notice.SINGLEKEY_BACKUP_MISSING -> {
                binding?.backupMissingLayout?.visibility = View.GONE
                sharedPreferences!!.edit()
                    .putLong(LATER_CLICK_TIME + account.id, System.currentTimeMillis())
                    .apply()
            }

            Notice.BACKUP_MISSING -> {
                binding?.backupMissingLayout?.visibility = View.GONE
                sharedPreferences!!.edit()
                    .putLong(LATER_CLICK_TIME_MASTER_SEED, System.currentTimeMillis())
                    .apply()
            }

            else -> {}
        }
    }

    private val noticeClickListener = View.OnClickListener {
        when (_notice) {
            Notice.RESET_PIN_AVAILABLE, Notice.RESET_PIN_IN_PROGRESS -> showPinResetWarning()
            Notice.BACKUP_MISSING -> showBackupWarning()
            Notice.SINGLEKEY_BACKUP_MISSING -> showSingleKeyBackupWarning()
            Notice.SINGLEKEY_VERIFY_MISSING -> showSingleKeyVerifyWarning()
            else -> {}
        }
    }

    private fun showPinResetWarning() {
        val resetPinRemainingBlocksCount = _mbwManager!!.resetPinRemainingBlocksCount

        if (!resetPinRemainingBlocksCount.isPresent) {
            recheckNotice()
            return
        }

        if (resetPinRemainingBlocksCount.get() == 0) {
            // delay is done
            _mbwManager!!.showClearPinDialog(activity) { recheckNotice() }
            return
        }

        // delay is still remaining, provide option to abort
        val remaining = Utils.formatBlockcountAsApproxDuration(
            _mbwManager,
            resetPinRemainingBlocksCount.or(1),
            Constants.BTC_BLOCK_TIME_IN_SECONDS
        )
        AlertDialog.Builder(activity)
            .setMessage(
                String.format(
                    activity!!.getString(R.string.pin_forgotten_abort_pin_reset),
                    remaining
                )
            )
            .setTitle(this.activity!!.getString(R.string.pin_forgotten_reset_pin_dialog_title))
            .setPositiveButton(
                this.activity!!.getString(R.string.yes)
            ) { dialog, which ->
                _mbwManager!!.metadataStorage.clearResetPinStartBlockheight()
                recheckNotice()
            }
            .setNegativeButton(
                this.activity!!.getString(R.string.no)
            ) { dialog, which ->
                // nothing to do here
            }
            .show()
    }

    private val warningClickListener = View.OnClickListener {
        if (!shouldWarnAboutHeartbleedBug()) {
            return@OnClickListener
        }
        Utils.showSimpleMessageDialog(activity, R.string.heartbleed_alert)
    }

    private fun showBackupWarning() {
        if (!isAdded) {
            return
        }
        Utils.pinProtectedWordlistBackup(activity)
    }

    private fun showSingleKeyBackupWarning() {
        if (!isAdded) {
            return
        }
        Utils.pinProtectedBackup(activity)
    }

    private fun showSingleKeyVerifyWarning() {
        if (!isAdded) {
            return
        }
        VerifyBackupActivity.callMe(activity)
    }

    private fun shouldWarnAboutHeartbleedBug(): Boolean {
        // The Heartbleed bug is only present in Android version 4.1.1
        return Build.VERSION.RELEASE == "4.1.1"
    }

    private fun updateUi() {
        if (!isAdded) {
            return
        }

        // Show button, that a PIN reset is in progress and allow to abort it
        binding?.btPinResetNotice?.visibility =
            if (_notice == Notice.RESET_PIN_AVAILABLE || _notice == Notice.RESET_PIN_IN_PROGRESS) View.VISIBLE else View.GONE
        val account = _mbwManager!!.selectedAccount
        // Only show the "Secure My Funds" button when necessary
        binding?.backupMissingLayout?.visibility =
            if ((_notice == Notice.BACKUP_MISSING && TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - sharedPreferences!!.getLong(
                        LATER_CLICK_TIME_MASTER_SEED,
                        0
                    )
                ) > 0)
                || (_notice == Notice.SINGLEKEY_BACKUP_MISSING && TimeUnit.MILLISECONDS.toDays(
                    System.currentTimeMillis() - sharedPreferences!!.getLong(
                        LATER_CLICK_TIME + account.id,
                        0
                    )
                ) > 0)
                || _notice == Notice.SINGLEKEY_VERIFY_MISSING
            )
                View.VISIBLE
            else
                View.GONE

        if (_notice == Notice.SINGLEKEY_VERIFY_MISSING) {
            binding?.tvBackupMissing!!.setText(R.string.singlekey_verify_notice)
            binding?.btnFirst?.setText(R.string.verify)
            binding?.btnSecond?.setText(R.string.backup)
            binding?.cbRisks!!.visibility = View.GONE
            binding?.btnSecond?.isEnabled = true
        } else if (_notice == Notice.SINGLEKEY_BACKUP_MISSING) {
            binding?.tvBackupMissing?.setText(R.string.single_address_backup_missing)
            binding?.btnFirst?.setText(R.string.backup_now)
            binding?.btnSecond?.setText(R.string.backup_later)
            binding?.cbRisks?.visibility = View.VISIBLE
            binding?.cbRisks?.isChecked = false
            binding?.btnSecond?.isEnabled = false
        } else if (_notice == Notice.BACKUP_MISSING) {
            binding?.tvBackupMissing!!.setText(R.string.wallet_backup_missing)
            binding?.btnFirst?.setText(R.string.backup_now)
            binding?.btnSecond?.setText(R.string.backup_later)
            binding?.cbRisks?.visibility = View.VISIBLE
            binding?.cbRisks?.isChecked = false
            binding?.btnSecond?.isEnabled = false
        }


        // Only show the heartbleed warning when necessary
        binding?.btWarning?.visibility =
            if (shouldWarnAboutHeartbleedBug()) View.VISIBLE else View.GONE
    }

    private fun recheckNotice() {
        _notice = determineNotice()
        updateUi()
    }

    @Subscribe
    fun accountChanged(event: AccountChanged?) {
        recheckNotice()
    }

    @Subscribe
    fun balanceChanged(event: BalanceChanged?) {
        recheckNotice()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        recheckNotice()
    }

    companion object {
        const val LATER_CLICK_TIME: String = "later_click_time"
        const val NOTICE: String = "notice"
        private const val LATER_CLICK_TIME_MASTER_SEED = "later_click_time_master_seed"
    }
}
