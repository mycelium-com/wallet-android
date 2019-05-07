package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.Utils
import com.mycelium.wallet.colu.ColuAccount
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bip44.HDAccount
import com.mycelium.wapi.wallet.currency.CurrencyBasedBalance

/**
 * Model for the account item on the accounts tab.
 */
class AccountViewModel(account: WalletAccount, mbwManager: MbwManager) : AccountListItem {
    val accountId = account.id!!
    val accountType = account.type!!
    val isActive = account.isActive
    val balance: CurrencyBasedBalance? = if (isActive) account.currencyBasedBalance else null
    val syncTotalRetrievedTransactions = account.syncTotalRetrievedTransactions
    val isRMCLinkedAccount = isRmcAccountLinked(account, mbwManager)
    var showBackupMissingWarning = showBackupMissingWarning(account, mbwManager)
    var label: String = mbwManager.metadataStorage.getLabelByAccount(accountId)
    var displayAddress: String
    val isSyncing= account.isSyncing

    init {
        val receivingAddress = account.receivingAddress
        displayAddress = if (receivingAddress.isPresent) {
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

    constructor(account: HDAccount, mbwManager: MbwManager) : this(account as WalletAccount, mbwManager) {
        displayAddress = Integer.toString(account.getPrivateKeyCount())
    }

    override fun getType() = AccountListItem.Type.ACCOUNT_TYPE

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AccountViewModel

        if (accountId != other.accountId) return false
        if (accountType != other.accountType) return false
        if (isActive != other.isActive) return false
        if (balance != other.balance) return false
        if (syncTotalRetrievedTransactions != other.syncTotalRetrievedTransactions) return false
        if (isRMCLinkedAccount != other.isRMCLinkedAccount) return false
        if (showBackupMissingWarning != other.showBackupMissingWarning) return false
        if (label != other.label) return false
        if (displayAddress != other.displayAddress) return false
        if (isSyncing != other.isSyncing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = accountId.hashCode()
        result = 31 * result + accountType.hashCode()
        result = 31 * result + isActive.hashCode()
        result = 31 * result + (balance?.hashCode() ?: 0)
        result = 31 * result + syncTotalRetrievedTransactions
        result = 31 * result + isRMCLinkedAccount.hashCode()
        result = 31 * result + showBackupMissingWarning.hashCode()
        result = 31 * result + label.hashCode()
        result = 31 * result + displayAddress.hashCode()
        result = 31 * result + isSyncing.hashCode()
        return result
    }

    companion object {
        private fun isRmcAccountLinked(walletAccount: WalletAccount, mbwManager: MbwManager): Boolean {
            val linked = Utils.getLinkedAccount(walletAccount, mbwManager.coluManager.accounts.values)
            if (linked != null && linked.type == WalletAccount.Type.COLU
                    && (linked as ColuAccount).coluAsset.assetType == ColuAccount.ColuAssetType.RMC) {
                return true
            }
            return false
        }

        private fun showBackupMissingWarning(account: WalletAccount, mbwManager: MbwManager): Boolean {
            if (account.isArchived) {
                return false
            }

            var showBackupMissingWarning = false
            if (account.canSpend()) {
                showBackupMissingWarning = if (account.isDerivedFromInternalMasterseed) {
                    mbwManager.metadataStorage.masterSeedBackupState != MetadataStorage.BackupState.VERIFIED
                } else {
                    val backupState = mbwManager.metadataStorage.getOtherAccountBackupState(account.id)
                    backupState != MetadataStorage.BackupState.VERIFIED && backupState != MetadataStorage.BackupState.IGNORED
                }
            }

            return showBackupMissingWarning
        }
    }
}