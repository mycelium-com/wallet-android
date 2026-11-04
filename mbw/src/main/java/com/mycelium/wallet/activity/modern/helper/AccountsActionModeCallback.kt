package com.mycelium.wallet.activity.modern.helper

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import com.mycelium.wallet.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.export.ShamirSharingActivity
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.modern.UnspentOutputsActivity
import com.mycelium.wallet.activity.modern.event.SelectTab
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel.Companion.showBackupMissingWarning
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.ExportableAccount
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount

class AccountsActionModeCallback(
    val context: Context,
    val menus: List<Int>,
    val mbwManager: MbwManager,
    val account: WalletAccount<*>,
    val runPinProtected: (Runnable) -> Unit,
    val actionHandler: (Int) -> Boolean,
    val destroyActionMode: () -> Unit
) : ActionMode.Callback {
    val toaster = Toaster(context)

    override fun onCreateActionMode(mode: ActionMode, menu: Menu?): Boolean {
        menus.forEach { res ->
            mode.menuInflater.inflate(res, menu)
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu?): Boolean {
        val item = menu?.findItem(R.id.miMakeBackup)
        item?.setShowAsAction(
            if (showBackupMissingWarning(account, mbwManager))
                MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER
        )
        val dropPriKeyItem = menu?.findItem(R.id.miDropPrivateKey)
        //        // add checkbox only for SingleAddressAccounts and only if a private key is present
        // Only single-address BTC accounts can drop their key and stay watch-only. ETH/TRON accounts are
        // ExportableAccount too (key export), but "watch-only" for them would delete the account and
        // orphan its ERC-20/TRC-20 tokens -> crash loop on the next start.
        val hasPrivateData = account is SingleAddressAccount && account is ExportableAccount
                && (account as ExportableAccount).getExportData(AesKeyCipher.defaultKeyCipher()).privateData.isPresent
        dropPriKeyItem?.isVisible = hasPrivateData
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.miShowOutputs -> {
                showOutputs()
                true
            }

            R.id.miRescan -> {
                // If we are synchronizing, show "Synchronizing, please wait..." to avoid blocking behavior
                if (account.isSyncing()) {
                    toaster.toast(R.string.synchronizing_please_wait, false);
                } else {
                    rescan()
                }
                true
            }

            R.id.miShamirBackup -> {
                shamirExportFocusedPrivateKey()
                true
            }

            else -> actionHandler(item.itemId)
        }

    override fun onDestroyActionMode(mode: ActionMode) {
        destroyActionMode()
    }

    private fun rescan() {
        account.dropCachedData()
        mbwManager.getWalletManager(false)
            .startSynchronization(SyncMode.FULL_SYNC_CURRENT_ACCOUNT_FORCED, mutableListOf(account))
    }

    private fun showOutputs() {
        account.interruptSync()
        val intent = Intent(context, UnspentOutputsActivity::class.java)
            .putExtra("account", account.id)
        context.startActivity(intent)
    }

    /**
     * Shamir-shares the private key of the account this action mode was opened for (the focused
     * item, which may be archived and therefore differ from the selected account).
     */
    private fun shamirExportFocusedPrivateKey() {
        val account = this.account
        if (account !is SingleAddressAccount || !account.canSpend()) {
            return
        }
        runPinProtected(Runnable {
            // Same lockdown as "Export account keys": a freshly set PIN must not give immediate
            // access to private keys.
            if (mbwManager.isPinProtected && !mbwManager.isPinOldEnough) {
                val remainingBlocks = mbwManager.remainingPinLockdownDuration
                    .or(Constants.MIN_PIN_BLOCKHEIGHT_AGE_ADDITIONAL_BACKUP)
                AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.shamir_export_not_possible_pin_too_new, remainingBlocks,
                        Utils.formatBlockcountAsApproxDuration(mbwManager, remainingBlocks, Constants.BTC_BLOCK_TIME_IN_SECONDS)))
                    .setPositiveButton(R.string.ok, null)
                    .show()
                return@Runnable
            }
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.export_account_data_warning).setCancelable(true)
                .setPositiveButton(R.string.yes, { dialog, id ->
                    dialog.dismiss()
                    account.interruptSync()
                    try {
                        val privateKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher())
                        if (privateKey != null) {
                            ShamirSharingActivity.callMe(context, privateKey)
                        } else {
                            toaster.toast("Something went wrong", false)
                        }
                    } catch (e: InvalidKeyCipher) {
                        toaster.toast("Something went wrong", false)
                    }
                }).setNegativeButton(R.string.no, null)
            val alertDialog = builder.create()
            alertDialog.show()
        })
    }
}
