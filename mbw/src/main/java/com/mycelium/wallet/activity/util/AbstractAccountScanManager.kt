/*
 * Copyright 2013 - 2018 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.AsyncTask
import android.os.Handler
import android.os.Looper
import com.google.common.base.Optional
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.BuildConfig
import com.mycelium.wapi.wallet.AccountScanManager
import com.mycelium.wapi.wallet.WalletManager
import com.satoshilabs.trezor.lib.protobuf.TrezorType
import com.squareup.otto.Bus
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

abstract class AbstractAccountScanManager(protected val context: Context, protected val network: NetworkParameters, protected val eventBus: Bus) : AccountScanManager {
    private var scanAsyncTask: AsyncTask<Void, ScanStatus, Void>? = null
    private val foundAccounts = ArrayList<AccountScanManager.HdKeyNodeWrapper>()
    protected val passphraseSyncQueue = LinkedBlockingQueue<Optional<String>>(1)
    protected val mainThreadHandler: Handler

    @Volatile
    var currentAccountState: AccountScanManager.AccountStatus = AccountScanManager.AccountStatus.unknown
    @Volatile
    var currentState: AccountScanManager.Status = AccountScanManager.Status.unableToScan
    @Volatile
    private var nextUnusedAccounts = listOf<HdKeyNode>()

    init {
        mainThreadHandler = Handler(Looper.getMainLooper())
    }

    open inner class ScanStatus(val state: AccountScanManager.Status, val accountState: AccountScanManager.AccountStatus)

    inner class FoundAccountStatus(val account: AccountScanManager.HdKeyNodeWrapper) : ScanStatus(AccountScanManager.Status.readyToScan, AccountScanManager.AccountStatus.scanning)

    protected abstract fun onBeforeScan(): Boolean

    override fun startBackgroundAccountScan(scanningCallback: AccountScanManager.AccountCallback) {
        if (currentAccountState == AccountScanManager.AccountStatus.scanning || currentAccountState == AccountScanManager.AccountStatus.done) {
            // currently scanning or have already all account - just post the events for all already known accounts
            for (a in foundAccounts) {
                eventBus.post(AccountScanManager.OnAccountFound(a))
            }
        } else {
            // start a background task which iterates over all accounts and calls the callback
            // to check if there was activity on it
            @SuppressLint("StaticFieldLeak")
            scanAsyncTask = object : AsyncTask<Void, ScanStatus, Void>() {
                override fun doInBackground(vararg voids: Void): Void? {
                    publishProgress(ScanStatus(AccountScanManager.Status.initializing, AccountScanManager.AccountStatus.unknown))
                    if (onBeforeScan()) {
                        publishProgress(ScanStatus(AccountScanManager.Status.readyToScan, AccountScanManager.AccountStatus.scanning))
                    } else {
                        return null
                    }

                    // scan through the accounts, to find the first unused one
                    var lastScannedPath: HdKeyPath? = null
                    val lastAccountPubKeyNodes = mutableListOf<HdKeyNode>()
                    var wasUsed = false
                    do {
                        val rootNodes: List<HdKeyNode>
                        val accountPathsToScan = getAccountPathsToScan(lastScannedPath, wasUsed)

                        // we have scanned all accounts - get out of here...
                        if (accountPathsToScan.isEmpty()) {
                            // remember the last xPub key as the next-unused one
                            nextUnusedAccounts = lastAccountPubKeyNodes
                            break
                        }

                        val accountPubKeyNodes = accountPathsToScan.map { getAccountPubKeyNode(it.value, it.key) }
                                .filter { it.isPresent }
                                .map { it.get() }
                        lastAccountPubKeyNodes.clear()
                        lastAccountPubKeyNodes.addAll(accountPubKeyNodes)


                        // unable to retrieve the account (eg. device unplugged) - cancel scan
                        if (accountPubKeyNodes.isEmpty()) {
                            publishProgress(ScanStatus(AccountScanManager.Status.initializing, AccountScanManager.AccountStatus.unknown))
                            break
                        }

                        rootNodes = accountPubKeyNodes

                        // leave accountID empty for now - set it later if it is an already used account
                        val acc = AccountScanManager.HdKeyNodeWrapper(accountPathsToScan.values, rootNodes, null)
                        val newAccount = scanningCallback.checkForTransactions(acc)
                        lastScannedPath = accountPathsToScan.values.first()

                        wasUsed = if (newAccount != null) {
                            val foundAccount = AccountScanManager.HdKeyNodeWrapper(accountPathsToScan.values, rootNodes, newAccount)

                            publishProgress(FoundAccountStatus(foundAccount))
                            true
                        } else {
                            false
                        }
                    } while (!isCancelled)
                    publishProgress(ScanStatus(AccountScanManager.Status.readyToScan, AccountScanManager.AccountStatus.done))
                    return null
                }


                override fun onPreExecute() {
                    super.onPreExecute()
                    currentState = AccountScanManager.Status.initializing
                }

                override fun onProgressUpdate(vararg stateInfo: ScanStatus) {
                    for (si in stateInfo) {
                        setState(si.state, si.accountState)

                        if (si is FoundAccountStatus) {
                            val foundAccount = si.account
                            eventBus.post(AccountScanManager.OnAccountFound(foundAccount))
                            foundAccounts.add(foundAccount)
                        }
                    }
                }
            }
            scanAsyncTask!!.execute()
        }
    }

    @Synchronized
    protected fun setState(state: AccountScanManager.Status, accountState: AccountScanManager.AccountStatus) {
        mainThreadHandler.post { eventBus.post(AccountScanManager.OnStatusChanged(state, accountState)) }
        currentState = state
        currentAccountState = accountState
    }

    override fun stopBackgroundAccountScan() {
        if (scanAsyncTask != null) {
            scanAsyncTask!!.cancel(true)
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
            try {
                return passphraseSyncQueue.take()
            } catch (ignore: InterruptedException) {
            }

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
                eventBus.post(AccountScanManager.OnScanError(msg, AccountScanManager.OnScanError.ErrorType.NOT_INITIALIZED))
            } else {
                eventBus.post(AccountScanManager.OnScanError(msg))
            }
        }
        return true
    }

    override fun setPassphrase(passphrase: String?) {
        passphraseSyncQueue.add(Optional.fromNullable(passphrase))
    }

    abstract fun upgradeAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager, uuid: UUID): Boolean

    abstract fun createOnTheFlyAccount(accountRoots: List<HdKeyNode>, walletManager: WalletManager, accountIndex: Int): UUID

    // returns the next Bip44 account based on the last scanned account
    override fun getAccountPathsToScan(lastPath: HdKeyPath?, wasUsed: Boolean): Map<BipDerivationType, HdKeyPath> {
        // this is the first call - no lastPath given
        if (lastPath == null) {
            return mapOf(BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(0),
                    BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(0))
        }

        // otherwise use the next bip44 account, as long as the last one had activity on it
        return if (wasUsed) {
            mapOf(BipDerivationType.BIP44 to BIP44COIN_TYPE.getAccount(lastPath.lastIndex + 1),
                    BipDerivationType.BIP49 to BIP49COIN_TYPE.getAccount(lastPath.lastIndex + 1),
                    BipDerivationType.BIP84 to BIP84COIN_TYPE.getAccount(lastPath.lastIndex + 1))
        } else {
            emptyMap()
        }
        // if we are already at the bip44 branch and the last account had no activity, then we are done
    }

    companion object {
        val BIP44COIN_TYPE = HdKeyPath.BIP44.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
        val BIP49COIN_TYPE = HdKeyPath.BIP49.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
        val BIP84COIN_TYPE = HdKeyPath.BIP84.getCoinTypeBitcoin(BuildConfig.FLAVOR == "btctestnet")
    }
}
