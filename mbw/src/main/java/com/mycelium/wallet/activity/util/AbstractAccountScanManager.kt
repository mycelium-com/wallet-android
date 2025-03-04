package com.mycelium.wallet.activity.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.AccountScanManager.*
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.squareup.otto.Bus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

abstract class AbstractAccountScanManager @JvmOverloads constructor(
    protected val context: Context,
    protected val network: NetworkParameters,
    protected val eventBus: Bus,
    private val coinType: CryptoCurrency = Utils.getBtcCoinType()
) : AccountScanManager {

    private var scanAsyncTask: Job? = null
    private val foundAccounts = ArrayList<HdKeyNodeWrapper>()

    @Volatile
    protected var passphraseValue: Pair<Boolean, String?> = Pair(false, null)
    protected val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    @Volatile
    var currentAccountState: AccountStatus =
        AccountStatus.unknown

    @Volatile
    var currentState: Status = Status.unableToScan

    @Volatile
    private var nextUnusedAccounts = listOf<HdKeyNode>()

    open inner class ScanStatus(
        val state: Status,
        val accountState: AccountStatus
    )

    inner class FoundAccountStatus(val account: HdKeyNodeWrapper) :
        ScanStatus(Status.readyToScan, AccountStatus.scanning)

    protected abstract fun onBeforeScan(): Boolean

    suspend fun progressUpdate(stateInfo: ScanStatus) {
        withContext(Dispatchers.Main) {
            setState(stateInfo.state, stateInfo.accountState)
            if (stateInfo is FoundAccountStatus) {
                val foundAccount = stateInfo.account
                eventBus.post(OnAccountFound(foundAccount))
                foundAccounts.add(foundAccount)
            }
        }
    }

    override fun startBackgroundAccountScan(checkTxs: suspend (HdKeyNodeWrapper) -> UUID?) {
        if (currentAccountState == AccountStatus.scanning || currentAccountState == AccountStatus.done) {
            // currently scanning or have already all account - just post the events for all already known accounts
            foundAccounts.forEach { a ->
                eventBus.post(OnAccountFound(a))
            }
        } else {
            // start a background task which iterates over all accounts and calls the callback
            // to check if there was activity on it
            scanAsyncTask = GlobalScope.launch(Dispatchers.Default) {
                progressUpdate(ScanStatus(Status.initializing, AccountStatus.unknown))
                if (onBeforeScan()) {
                    progressUpdate(ScanStatus(Status.readyToScan, AccountStatus.scanning))
                } else {
                    progressUpdate(ScanStatus(Status.unableToScan, AccountStatus.unknown))
                    return@launch
                }
                // scan through the accounts, to find the first unused one
                scanAccounts(checkTxs)
                progressUpdate(ScanStatus(Status.readyToScan, AccountStatus.done))
            }
        }
    }

    private suspend fun CoroutineScope.scanAccounts(checkTxs: suspend (HdKeyNodeWrapper) -> UUID?) {
        var lastScannedPath: HdKeyPath? = null
        val lastAccountPubKeyNodes = mutableListOf<HdKeyNode>()
        var wasUsedOrAccountsLookahead = false
        var lastUsedAccountIndex = 0
        while (isActive) {
            val accountPathsToScan =
                getAccountPathsToScan(lastScannedPath, wasUsedOrAccountsLookahead, coinType)

            // we have scanned all accounts - get out of here...
            if (accountPathsToScan.isEmpty()) {
                // remember the last xPub key as the next-unused one
                nextUnusedAccounts = lastAccountPubKeyNodes.toList()
                break
            } else {
                val accountPubKeyNodes =
                    accountPathsToScan.mapNotNull { getAccountPubKeyNode(it.value, it.key) }
                // unable to retrieve the account (eg. device unplugged) - cancel scan
                if (accountPubKeyNodes.isEmpty()) {
                    break
                }
                lastAccountPubKeyNodes.clear()
                lastAccountPubKeyNodes.addAll(accountPubKeyNodes)

                // leave accountID empty for now - set it later if it is an already used account
                val acc =
                    HdKeyNodeWrapper(accountPathsToScan.values, accountPubKeyNodes, null)
                val newAccount = checkTxs(acc)
                lastScannedPath = accountPathsToScan.values.first()
                wasUsedOrAccountsLookahead = if (newAccount != null) {
                    lastUsedAccountIndex++
                    val hdKeyNode = HdKeyNodeWrapper(
                        accountPathsToScan.values,
                        accountPubKeyNodes,
                        newAccount
                    )
                    progressUpdate(FoundAccountStatus(hdKeyNode))
                    true
                } else {
                    // for FIO accounts we want to perform accounts lookahead
                    coinType == Utils.getFIOCoinType() &&
                            accountPathsToScan.values.first().lastIndex < lastUsedAccountIndex + ACCOUNT_LOOKAHEAD
                }
            }
        }
    }

    @Synchronized
    protected fun setState(
        state: Status,
        accountState: AccountStatus
    ) {
        mainThreadHandler.post { eventBus.post(OnStatusChanged(state, accountState)) }
        currentState = state
        currentAccountState = accountState
    }

    override fun stopBackgroundAccountScan() {
        if (scanAsyncTask != null) {
            scanAsyncTask?.cancel()
            scanAsyncTask = null
            currentAccountState = AccountStatus.unknown
        }
    }

    override fun getNextUnusedAccounts() = nextUnusedAccounts

    override fun forgetAccounts() {
        if (currentAccountState == AccountStatus.scanning) {
            stopBackgroundAccountScan()
        }
        currentAccountState = AccountStatus.unknown
        foundAccounts.clear()
    }

    protected fun waitForPassphrase(): String? {
        passphraseValue = passphraseValue.copy(false, null)
        // call external passphrase request ...
        mainThreadHandler.post { eventBus.post(OnPassphraseRequest()) }

        // ... and block until we get one
        while (passphraseValue.first == false) {
            Thread.sleep(50)
        }
        return passphraseValue.second
    }

    protected fun postErrorMessage(msg: String): Boolean {
        mainThreadHandler.post { eventBus.post(OnScanError(msg)) }
        return true
    }

    protected fun postErrorMessage(msg: String, failureType: TrezorType.FailureType): Boolean {
        mainThreadHandler.post {
            // need to map to the known error types, because wapi does not import the trezor lib
            if (failureType == TrezorType.FailureType.Failure_NotInitialized) {
                eventBus.post(
                    OnScanError(
                        msg,
                        OnScanError.ErrorType.NOT_INITIALIZED
                    )
                )
            } else {
                eventBus.post(OnScanError(msg))
            }
        }
        return true
    }

    override fun setPassphrase(passphrase: String?) {
        passphraseValue = passphraseValue.copy(true, passphrase)
    }

    abstract fun upgradeAccount(
        accountRoots: List<HdKeyNode>,
        walletManager: WalletManager,
        uuid: UUID
    ): Boolean

    abstract fun createOnTheFlyAccount(
        accountRoots: List<HdKeyNode>,
        walletManager: WalletManager,
        accountIndex: Int
    ): UUID

    // returns the next Bip44 account based on the last scanned account
    override fun getAccountPathsToScan(
        lastPath: HdKeyPath?,
        wasUsed: Boolean,
        coinType: CryptoCurrency?
    ): Map<BipDerivationType, HdKeyPath> {
        // this is the first call - no lastPath given
        if (lastPath == null) {
            return if (coinType == Utils.getBtcCoinType()) {
                mapOf(
                    BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(0)
                )
            } else {
                mapOf(BipDerivationType.BIP44 to BIP44FIOCOIN_TYPE.getAccount(0))
            }
        }

        // otherwise use the next bip44 account, as long as the last one had activity on it
        // or we perform accounts lookahead
        return if (wasUsed) {
            if (coinType == Utils.getBtcCoinType()) {
                mapOf(
                    BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(lastPath.lastIndex + 1),
                    BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(lastPath.lastIndex + 1),
                    BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(lastPath.lastIndex + 1)
                )
            } else {
                mapOf(BipDerivationType.BIP44 to BIP44FIOCOIN_TYPE.getAccount(lastPath.lastIndex + 1))
            }
        } else {
            emptyMap()
        }
        // if we are already at the bip44 branch and the last account had no activity, then we are done
    }

    companion object {
        const val ACCOUNT_LOOKAHEAD = 20
        val BIP44FIOCOIN_TYPE = HdKeyPath.BIP44.getHardenedChild(235)
        val BIP44COIN_TYPE = HdKeyPath.BIP44.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
        val BIP49COIN_TYPE = HdKeyPath.BIP49.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
        val BIP84COIN_TYPE = HdKeyPath.BIP84.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
    }
}
