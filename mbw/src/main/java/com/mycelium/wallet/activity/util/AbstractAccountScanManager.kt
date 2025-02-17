package com.mycelium.wallet.activity.util

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.squareup.otto.Bus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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
    private val foundAccounts = ArrayList<AccountScanManager.HdKeyNodeWrapper>()
    @Volatile
    protected var passphraseValue: String? = null
    protected val mainThreadHandler: Handler = Handler(Looper.getMainLooper())

    @Volatile
    var currentAccountState: AccountScanManager.AccountStatus =
        AccountScanManager.AccountStatus.unknown

    @Volatile
    var currentState: AccountScanManager.Status = AccountScanManager.Status.unableToScan

    @Volatile
    private var nextUnusedAccounts = listOf<HdKeyNode>()

    open inner class ScanStatus(
        val state: AccountScanManager.Status,
        val accountState: AccountScanManager.AccountStatus
    )

    inner class FoundAccountStatus(val account: AccountScanManager.HdKeyNodeWrapper) :
        ScanStatus(AccountScanManager.Status.readyToScan, AccountScanManager.AccountStatus.scanning)

    protected abstract fun onBeforeScan(): Boolean

    suspend fun progressUpdate(stateInfo: ScanStatus) {
        withContext(Dispatchers.Main) {
            setState(stateInfo.state, stateInfo.accountState)
            if (stateInfo is FoundAccountStatus) {
                val foundAccount = stateInfo.account
                eventBus.post(AccountScanManager.OnAccountFound(foundAccount))
                foundAccounts.add(foundAccount)
            }
        }
    }

    override fun startBackgroundAccountScan(scanningCallback: AccountScanManager.AccountCallback) {
        if (currentAccountState == AccountScanManager.AccountStatus.scanning || currentAccountState == AccountScanManager.AccountStatus.done) {
            // currently scanning or have already all account - just post the events for all already known accounts
            foundAccounts.forEach { a ->
                eventBus.post(AccountScanManager.OnAccountFound(a))
            }
        } else {
            // start a background task which iterates over all accounts and calls the callback
            // to check if there was activity on it
            scanAsyncTask = GlobalScope.launch(Dispatchers.Default) {
                currentState = AccountScanManager.Status.initializing

                progressUpdate(
                    ScanStatus(
                        AccountScanManager.Status.initializing,
                        AccountScanManager.AccountStatus.unknown
                    )
                )
                if (onBeforeScan()) {
                    progressUpdate(
                        ScanStatus(
                            AccountScanManager.Status.readyToScan,
                            AccountScanManager.AccountStatus.scanning
                        )
                    )
                } else {
                    return@launch
                }

                // scan through the accounts, to find the first unused one
                var lastScannedPath: HdKeyPath? = null
                val lastAccountPubKeyNodes = mutableListOf<HdKeyNode>()
                var wasUsedOrAccountsLookahead = false
                var lastUsedAccountIndex = 0
                do {
                    val accountPathsToScan =
                        getAccountPathsToScan(lastScannedPath, wasUsedOrAccountsLookahead, coinType)

                    // we have scanned all accounts - get out of here...
                    if (accountPathsToScan.isEmpty()) {
                        // remember the last xPub key as the next-unused one
                        nextUnusedAccounts = lastAccountPubKeyNodes
                        break
                    }

                    val accountPubKeyNodes =
                        accountPathsToScan.mapNotNull { getAccountPubKeyNode(it.value, it.key) }
                    lastAccountPubKeyNodes.clear()
                    lastAccountPubKeyNodes.addAll(accountPubKeyNodes)

                    // unable to retrieve the account (eg. device unplugged) - cancel scan
                    if (accountPubKeyNodes.isEmpty()) {
                        progressUpdate(
                            ScanStatus(
                                AccountScanManager.Status.initializing,
                                AccountScanManager.AccountStatus.unknown
                            )
                        )
                        break
                    }
                    // leave accountID empty for now - set it later if it is an already used account
                    val acc = AccountScanManager.HdKeyNodeWrapper(
                        accountPathsToScan.values,
                        accountPubKeyNodes,
                        null
                    )
                    val newAccount = scanningCallback.checkForTransactions(acc)
                    lastScannedPath = accountPathsToScan.values.first()
                    wasUsedOrAccountsLookahead = if (newAccount != null) {
                        lastUsedAccountIndex++
                        progressUpdate(
                            FoundAccountStatus(
                                AccountScanManager.HdKeyNodeWrapper(
                                    accountPathsToScan.values,
                                    accountPubKeyNodes,
                                    newAccount
                                )
                            )
                        )
                        true
                    } else {
                        // for FIO accounts we want to perform accounts lookahead
                        coinType == Utils.getFIOCoinType() && accountPathsToScan.values.iterator()
                            .next().lastIndex < lastUsedAccountIndex + ACCOUNT_LOOKAHEAD
                    }
                    delay(50)
                } while (true)
                progressUpdate(
                    ScanStatus(
                        AccountScanManager.Status.readyToScan,
                        AccountScanManager.AccountStatus.done
                    )
                )
            }
        }
    }

    @Synchronized
    protected fun setState(
        state: AccountScanManager.Status,
        accountState: AccountScanManager.AccountStatus
    ) {
        mainThreadHandler.post {
            eventBus.post(
                AccountScanManager.OnStatusChanged(
                    state,
                    accountState
                )
            )
        }
        currentState = state
        currentAccountState = accountState
    }

    override fun stopBackgroundAccountScan() {
        if (scanAsyncTask != null) {
            scanAsyncTask?.cancel()
            scanAsyncTask = null
            currentAccountState = AccountScanManager.AccountStatus.unknown
        }
    }

    override fun getNextUnusedAccounts() = nextUnusedAccounts

    override fun forgetAccounts() {
        if (currentAccountState == AccountScanManager.AccountStatus.scanning) {
            stopBackgroundAccountScan()
        }
        currentAccountState = AccountScanManager.AccountStatus.unknown
        foundAccounts.clear()
    }

    protected fun waitForPassphrase(): Optional<String> {
        // call external passphrase request ...
        mainThreadHandler.post { eventBus.post(AccountScanManager.OnPassphraseRequest()) }

        // ... and block until we get one
        while (true) {
            passphraseValue?.let {
                return Optional.of(it)
            }
            Thread.sleep(50)
        }
    }

    protected fun postErrorMessage(msg: String): Boolean {
        mainThreadHandler.post { eventBus.post(AccountScanManager.OnScanError(msg)) }
        return true
    }

    protected fun postErrorMessage(msg: String, failureType: TrezorType.FailureType): Boolean {
        mainThreadHandler.post {
            // need to map to the known error types, because wapi does not import the trezor lib
            if (failureType == TrezorType.FailureType.Failure_NotInitialized) {
                eventBus.post(
                    AccountScanManager.OnScanError(
                        msg,
                        AccountScanManager.OnScanError.ErrorType.NOT_INITIALIZED
                    )
                )
            } else {
                eventBus.post(AccountScanManager.OnScanError(msg))
            }
        }
        return true
    }

    override fun setPassphrase(passphrase: String?) {
        this.passphraseValue = passphrase
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
