package com.mycelium.wallet.activity.modern

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.res.Resources
import android.text.Html
import android.view.View.*
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.adapter.holder.AccountViewHolder
import com.mycelium.wallet.activity.modern.model.ViewAccountModel
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.persistence.MetadataStorage.BackupState
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest
import com.mycelium.wapi.wallet.colu.getColuAccounts

class RecordRowBuilder(private val mbwManager: MbwManager, private val resources: Resources) {
    fun buildRecordView(holder: AccountViewHolder, model: ViewAccountModel, isSelected: Boolean, hasFocus: Boolean) {
        // Make grey if not part of the balance
        Utils.setAlpha(holder.llAddress, if (!isSelected) 0.5f else 1f)

        // Show focus if applicable
        holder.llAddress.setBackgroundColor(resources.getColor(if (hasFocus) R.color.selectedrecord else R.color.transparent))

        // Show/hide key icon
        val drawableForAccount = if (isSelected) model.drawableForAccountSelected else model.drawableForAccount
        if (drawableForAccount == null) {
            holder.icon.visibility = INVISIBLE
        } else {
            holder.icon.visibility = VISIBLE
            holder.icon.setImageDrawable(drawableForAccount)
        }
        updateRMCInfo(holder, model)
        val textColor = resources.getColor(R.color.white)
        if (model.label.isEmpty()) {
            holder.tvLabel.visibility = GONE
        } else {
            // Display name
            holder.tvLabel.visibility = VISIBLE
            holder.tvLabel.text = Html.fromHtml(model.label)
            holder.tvLabel.setTextColor(textColor)
        }
        holder.tvAddress.text = model.displayAddress
        holder.tvAddress.setTextColor(textColor)
        holder.lastSyncState.visibility = if (model.isSyncError && model.isActive) VISIBLE else GONE
        holder.lastSyncState.setOnClickListener {
            Toaster(it.context).toastSyncFailed(mbwManager.getWalletManager(false).getAccount(model.accountId)?.lastSyncStatus())
        }
        updateSyncing(holder, model)
        updateBalance(holder, model, textColor)
        // Show/hide trader account message
        holder.tvTraderKey.visibility = if (model.accountId == mbwManager.localTraderManager.localTraderAccountId) VISIBLE else GONE
    }

    private fun updateRMCInfo(holder: AccountViewHolder, model: ViewAccountModel) {
        if (model.isRMCLinkedAccount) {
            holder.tvWhatIsIt.setOnClickListener { view ->
                AlertDialog.Builder(view.context)
                        .setMessage(resources.getString(R.string.rmc_bitcoin_acc_what_is_it))
                        .setPositiveButton(R.string.button_ok, null)
                        .create()
                        .show()
            }
            holder.tvWhatIsIt.visibility = VISIBLE
        } else {
            holder.tvWhatIsIt.visibility = GONE
        }
    }

    private fun updateSyncing(holder: AccountViewHolder, model: ViewAccountModel) {
        if (model.isSyncing && model.isActive) {
            holder.tvProgressLayout.visibility = VISIBLE
            if (model.syncTotalRetrievedTransactions == 0) {
                holder.layoutProgressTxRetreived.visibility = GONE
            } else {
                holder.layoutProgressTxRetreived.visibility = VISIBLE
                holder.tvProgress.text = resources.getString(R.string.sync_total_retrieved_transactions,
                        model.syncTotalRetrievedTransactions)
                holder.ivWhatIsSync.setOnClickListener(whatIsSyncHandler)
            }
        } else {
            holder.tvProgressLayout.visibility = GONE
        }
    }

    private fun updateBalance(holder: AccountViewHolder, model: ViewAccountModel, textColor: Int) {
        if (model.isActive) {
            val balance = model.balance
            holder.tvBalance.visibility = VISIBLE
            val balanceString = balance.spendable.toStringWithUnit(mbwManager.getDenomination(model.coinType))
            holder.tvBalance.text = balanceString
            holder.tvBalance.setTextColor(textColor)

            // Show legacy account with funds warning if necessary
            holder.backupMissing.visibility = if (model.showBackupMissingWarning) VISIBLE else GONE
            if (mbwManager.metadataStorage.getOtherAccountBackupState(model.accountId) === BackupState.NOT_VERIFIED) {
                holder.backupMissing.setText(R.string.backup_not_verified)
            } else {
                holder.backupMissing.setText(R.string.backup_missing)
            }
            holder.tvAccountType.visibility = GONE
        } else {
            // We don't show anything if the account is archived
            holder.tvBalance.visibility = GONE
            holder.backupMissing.visibility = GONE
            if (model.accountType.isInstance(Bip44BCHAccount::class.java)
                    || model.accountType.isInstance(SingleAddressBCHAccount::class.java)) {
                holder.tvAccountType.text = Html.fromHtml(holder.tvAccountType.resources.getString(R.string.bitcoin_cash))
                holder.tvAccountType.visibility = VISIBLE
            } else {
                holder.tvAccountType.visibility = GONE
            }
        }
    }

    private val whatIsSyncHandler = OnClickListener { view ->
        AlertDialog.Builder(view.context, R.style.MyceliumModern_Dialog_BlueButtons)
                .setTitle(resources.getString(R.string.what_is_sync))
                .setMessage(resources.getString(R.string.what_is_sync_description))
                .setPositiveButton(R.string.button_ok, null)
                .create()
                .show()
    }

    @SuppressLint("StringFormatMatches")
    private fun convert(walletAccount: WalletAccount<*>): ViewAccountModel =
            ViewAccountModel().apply {
                accountId = walletAccount.id
                coinType = walletAccount.coinType
                drawableForAccount = Utils.getDrawableForAccount(walletAccount, false, resources)
                drawableForAccountSelected = Utils.getDrawableForAccount(walletAccount, true, resources)
                accountType = walletAccount.javaClass
                syncTotalRetrievedTransactions = walletAccount.syncTotalRetrievedTransactions
                val linked = Utils.getLinkedAccount(walletAccount, mbwManager.getWalletManager(false).getColuAccounts())
                isRMCLinkedAccount = linked?.coinType == RMCCoin || linked?.coinType == RMCCoinTest

                label = mbwManager.metadataStorage.getLabelByAccount(walletAccount.id)
                displayAddress = if (walletAccount.isActive) {
                    when (walletAccount) {
                        is HDPubOnlyAccount -> {
                            walletAccount.getPrivateKeyCount().let { numKeys ->
                                resources.getQuantityString(R.plurals.contains_addresses, numKeys, numKeys)
                            }
                        }
                        is HDAccount -> {
                            walletAccount.getPrivateKeyCount().let { numKeys ->
                                resources.getQuantityString(R.plurals.contains_keys, numKeys, numKeys)
                            }
                        }
                        else -> {
                            val receivingAddress = (walletAccount as WalletBtcAccount).receivingAddress
                            if (receivingAddress.isPresent) {
                                if (label.isEmpty()) {
                                    // Display address in it's full glory, chopping it into three
                                    receivingAddress.get().toMultiLineString()
                                } else {
                                    // Display address in short form
                                    receivingAddress.get().shortAddress
                                }
                            } else {
                                ""
                            }
                        }
                    }
                } else {
                    "" //dont show key count of archived accs
                }
                isActive = walletAccount.isActive
                if (isActive) {
                    balance = walletAccount.accountBalance
                    showBackupMissingWarning = showBackupMissingWarning(walletAccount, mbwManager)
                }
            }

    fun convertList(accounts: List<WalletAccount<*>>): List<ViewAccountModel> = accounts.map { convert(it) }

    companion object {
        private fun showBackupMissingWarning(account: WalletAccount<*>, mbwManager: MbwManager): Boolean {
            if (account.isArchived) {
                return false
            }
            return if (account.canSpend()) {
                if (account.isDerivedFromInternalMasterseed()) {
                    mbwManager.metadataStorage.masterSeedBackupState !== BackupState.VERIFIED
                } else {
                    val backupState = mbwManager.metadataStorage.getOtherAccountBackupState(account.id)
                    backupState !== BackupState.VERIFIED && backupState !== BackupState.IGNORED
                }
            } else {
                false
            }
        }
    }
}