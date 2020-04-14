package com.mycelium.wallet.activity.modern.model.accounts

import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.Utils
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.AddressUtils
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature
import com.mycelium.wapi.wallet.coins.Balance
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.colu.coins.RMCCoin
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest

/**
 * Model for the account item on the accounts tab.
 */
class AccountViewModel(account: WalletAccount<out Address>, mbwManager: MbwManager?) : AccountListItem {
    val accountId = account.id!!
    val accountType = account::class.java
    val coinType = account.coinType
    val isActive = account.isActive
    val balance: Balance? = if (isActive) account.accountBalance else null
    val syncTotalRetrievedTransactions = account.syncTotalRetrievedTransactions
    val isRMCLinkedAccount = if (mbwManager != null) isRmcAccountLinked(account, mbwManager) else false
    var showBackupMissingWarning = if (mbwManager != null) showBackupMissingWarning(account, mbwManager) else false
    var label: String = mbwManager?.metadataStorage?.getLabelByAccount(accountId) ?: ""
    var displayAddress: String
    val isSyncing = account.isSyncing
    // if need key count for other classes add count logic
    val privateKeyCount = if (account is HDAccount) account.getPrivateKeyCount() else -1
    val canSpend = account.canSpend()
    val externalAccountType = if (account is HDAccountExternalSignature) account.accountType else -1

    init {
        val receivingAddress = account.receiveAddress
        if (label.isBlank()) {
            label = account.label ?: ""
        }
        displayAddress = if (receivingAddress != null) {
            if (label.isEmpty()) {
                // Display address in it's full glory, chopping it into three
                AddressUtils.toMultiLineString(receivingAddress.toString())
            } else {
                // Display address in short form
                AddressUtils.toShortString(receivingAddress.toString())
            }
        } else {
            ""
        }
    }

    constructor(account: HDAccount, mbwManager: MbwManager) : this(account as WalletBtcAccount, mbwManager) {
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
        private fun isRmcAccountLinked(walletAccount: WalletAccount<out Address>, mbwManager: MbwManager): Boolean {
            val linked = Utils.getLinkedAccount(walletAccount, mbwManager.getWalletManager(false).getAccounts())
            return linked is ColuAccount && (linked.coinType == RMCCoin || linked.coinType == RMCCoinTest)
        }

        private fun showBackupMissingWarning(account: WalletAccount<out Address>, mbwManager: MbwManager): Boolean {
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