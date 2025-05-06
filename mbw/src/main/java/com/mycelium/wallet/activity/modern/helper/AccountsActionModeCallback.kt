package com.mycelium.wallet.activity.modern.helper

import android.content.Context
import android.content.Intent
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ActionMode
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.export.ShamirSharingActivity
import com.mycelium.wallet.activity.fio.registername.RegisterFioNameActivity
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.modern.UnspentOutputsActivity
import com.mycelium.wallet.activity.modern.event.SelectTab
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel.Companion.showBackupMissingWarning
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.WalletAccount

class AccountsActionModeCallback(
    val context: Context,
    val menus: List<Int>,
    val mbwManager: MbwManager,
    val account: WalletAccount<Address>,
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
            if (showBackupMissingWarning(mbwManager.getSelectedAccount(), mbwManager))
                MenuItem.SHOW_AS_ACTION_IF_ROOM else MenuItem.SHOW_AS_ACTION_NEVER
        )
        val dropPriKeyItem = menu?.findItem(R.id.miDropPrivateKey)
        dropPriKeyItem?.isVisible = account.canSpend()
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.miMapFioAddress -> {
                RegisterFioNameActivity.start(context, account.id)
                true
            }

            R.id.miMapToFio -> {
                FioHelper.chooseAccountToMap(context, account)
                true
            }

            R.id.miFIORequests -> {
                MbwManager.getEventBus().post(SelectTab(ModernMain.TAB_FIO_REQUESTS));
                true
            }

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
                shamirExportSelectedPrivateKey()
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

    private fun shamirExportSelectedPrivateKey() {
        runPinProtected(Runnable {
            val account = MbwManager.getInstance(context).getSelectedAccount()
            val builder = AlertDialog.Builder(context)
            builder.setMessage(R.string.export_account_data_warning).setCancelable(true)
                .setPositiveButton(R.string.yes, { dialog, id ->
                    dialog.dismiss()
                    account.interruptSync()
                    try {
                        val privateKey = account.getPrivateKey(AesKeyCipher.defaultKeyCipher())
                        ShamirSharingActivity.callMe(context, privateKey!!)
                    } catch (e: InvalidKeyCipher) {
                        toaster.toast("Something went wrong", false)
                    }
                }).setNegativeButton(R.string.no, null)
            val alertDialog = builder.create()
            alertDialog.show()
        })
    }
}