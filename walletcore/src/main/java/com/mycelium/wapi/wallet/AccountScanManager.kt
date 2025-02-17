package com.mycelium.wapi.wallet

import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wapi.wallet.AccountScanManager.AccountStatus
import com.mycelium.wapi.wallet.AccountScanManager.HdKeyNodeWrapper
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.UUID

interface AccountScanManager {
    fun startBackgroundAccountScan(scanningCallback: AccountCallback)
    fun stopBackgroundAccountScan()
    fun forgetAccounts()
    fun getNextUnusedAccounts(): List<HdKeyNode>
    fun getAccountPubKeyNode(
        keyPath: HdKeyPath,
        derivationType: BipDerivationType
    ): HdKeyNode?

    fun getAccountPathsToScan(
        lastPath: HdKeyPath?,
        wasUsed: Boolean,
        coinType: CryptoCurrency?
    ): Map<BipDerivationType, HdKeyPath>

    fun setPassphrase(passphrase: String?)

    enum class Status {
        unableToScan, initializing, readyToScan
    }

    enum class AccountStatus {
        unknown, scanning, done
    }

    data class HdKeyNodeWrapper(
        val keysPaths: Collection<HdKeyPath>,
        val accountsRoots: List<HdKeyNode>,
        val accountId: UUID?
    )

    // Classes for the EventBus
    data class OnAccountFound(val account: HdKeyNodeWrapper)

    data class OnStatusChanged(
        @JvmField
        val state: Status?,
        @JvmField
        val accountState: AccountStatus?
    )

    class OnScanError(
        @JvmField
        val errorMessage: String?,
        @JvmField
        val errorType: ErrorType = ErrorType.UNKNOWN
    ) {
        enum class ErrorType {
            UNKNOWN, NOT_INITIALIZED
        }
    }

    class OnPassphraseRequest

    interface AccountCallback {
        // gets called from a background thread
        suspend fun checkForTransactions(account: HdKeyNodeWrapper): UUID?
    }
}
