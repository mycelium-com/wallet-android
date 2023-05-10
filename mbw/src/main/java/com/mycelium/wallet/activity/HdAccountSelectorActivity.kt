/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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
package com.mycelium.wallet.activity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.common.collect.Iterables
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.AccountScanManager.AccountCallback
import com.mycelium.wapi.wallet.AccountScanManager.HdKeyNodeWrapper
import com.mycelium.wallet.MbwManager
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.AccountScanManager
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mrd.bitlib.crypto.HdKeyNode
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.SyncMode
import com.squareup.otto.Subscribe
import kotlinx.coroutines.runBlocking
import java.io.Serializable
import java.util.*
import javax.annotation.Nonnull

abstract class HdAccountSelectorActivity : AppCompatActivity(), MasterseedPasswordSetter {
    @JvmField
    protected var accounts = ArrayList<HdAccountWrapper>()
    @JvmField
    protected var accountsAdapter: AccountsAdapter? = null
    @JvmField
    protected var masterseedScanManager: AbstractAccountScanManager? = null
    @JvmField
    protected var txtStatus: TextView? = null
    @JvmField
    protected var coinType: CryptoCurrency? = null
    protected abstract fun initMasterseedManager(): AbstractAccountScanManager?
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setView()
        val lvAccounts = findViewById<ListView>(R.id.lvAccounts)
        txtStatus = findViewById(R.id.txtStatus)

        // Accounts listview + adapter
        accountsAdapter = AccountsAdapter(this, R.id.lvAccounts, accounts)
        lvAccounts.adapter = accountsAdapter
        lvAccounts.onItemClickListener = accountClickListener()
        // ask user from what blockchain he/she wants to spend from
        val selectedItem = IntArray(1)
        val choices = arrayOfNulls<CharSequence>(2)
        choices[0] = "BTC"
        choices[1] = "FIO"
        AlertDialog.Builder(this)
            .setTitle("Choose blockchain")
            .setSingleChoiceItems(choices, 0) { _: DialogInterface?, i: Int ->
                selectedItem[0] = i
            }
            .setPositiveButton(this.getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                coinType = if (selectedItem[0] == 0) {
                    Utils.getBtcCoinType()
                } else {
                    Utils.getFIOCoinType()
                }
                masterseedScanManager = initMasterseedManager()
                startBackgroundScan()
                updateUi()
            }
            .setNegativeButton(this.getString(R.string.cancel)) { _: DialogInterface?, _: Int -> super.finish() }
            .setCancelable(false)
            .show()
    }

    protected fun startBackgroundScan() {
        masterseedScanManager!!.startBackgroundAccountScan(object : AccountCallback {
            override fun checkForTransactions(account: HdKeyNodeWrapper): UUID? {
                val mbwManager = MbwManager.getInstance(applicationContext)
                val walletManager = mbwManager.getWalletManager(true)
                val id = masterseedScanManager!!.createOnTheFlyAccount(
                    account.accountsRoots,
                    walletManager,
                    account.keysPaths.iterator().next().lastIndex)
                val walletAccount = walletManager.getAccount(id)
                if (walletAccount is HDAccount) {
                    runBlocking {
                        walletAccount.doSynchronization(SyncMode.NORMAL_WITHOUT_TX_LOOKUP)
                    }
                    return if (walletAccount.hasHadActivity()) {
                        id
                    } else {
                        walletAccount.dropCachedData()
                        null
                    }
                } else if (walletAccount is FioAccount) {
                    runBlocking {
                        walletAccount.synchronize(SyncMode.NORMAL_WITHOUT_TX_LOOKUP)
                    }
                    return if (walletAccount.hasHadActivity()) {
                        id
                    } else {
                        walletAccount.dropCachedData()
                        null
                    }
                }
                return null
            }
        })
    }

    protected abstract fun accountClickListener(): AdapterView.OnItemClickListener?
    protected abstract fun setView()
    override fun finish() {
        super.finish()
        masterseedScanManager!!.stopBackgroundAccountScan()
    }

    override fun onResume() {
        super.onResume()
        MbwManager.getEventBus().register(this)
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    protected open fun updateUi() {
        if (masterseedScanManager!!.currentAccountState == AccountScanManager.AccountStatus.scanning) {
            findViewById<View>(R.id.llStatus).visibility =
                View.VISIBLE
            if (accounts.isNotEmpty()) {
                txtStatus!!.text =
                    String.format(getString(R.string.account_found), Iterables.getLast(accounts).name)
                findViewById<View>(R.id.llSelectAccount).visibility =
                    View.VISIBLE
            }
        } else if (masterseedScanManager!!.currentAccountState == AccountScanManager.AccountStatus.done) {
            // DONE
            findViewById<View>(R.id.llStatus).visibility = View.GONE
            findViewById<View>(R.id.llSelectAccount).visibility = View.VISIBLE
            if (accounts.isEmpty()) {
                // no accounts found
                findViewById<View>(R.id.tvNoAccounts).visibility = View.VISIBLE
                findViewById<View>(R.id.lvAccounts).visibility =
                    View.GONE
            } else {
                findViewById<View>(R.id.tvNoAccounts).visibility = View.GONE
                findViewById<View>(R.id.lvAccounts).visibility = View.VISIBLE
            }
        }
        accountsAdapter!!.notifyDataSetChanged()
    }

    override fun onBackPressed() {
        super.onBackPressed()
        clearTempData()
    }

    protected fun clearTempData() {
        // remove all account-data from the tempWalletManager, to improve privacy
        MbwManager.getInstance(this).forgetColdStorageWalletManager()
        masterseedScanManager!!.forgetAccounts()
    }

    override fun setPassphrase(passphrase: String) {
        masterseedScanManager!!.setPassphrase(passphrase)
        // close the dialog fragment
        val fragPassphrase = fragmentManager.findFragmentByTag(PASSPHRASE_FRAGMENT_TAG)
        if (fragPassphrase != null) {
            fragmentManager.beginTransaction().remove(fragPassphrase).commit()
        }
    }

    protected class HdAccountWrapper : Serializable {
        @JvmField
        var id: UUID? = null
        @JvmField
        var accountHdKeysPaths: Collection<HdKeyPath>? = null
        @JvmField
        var publicKeyNodes: List<HdKeyNode>? = null
        var name: String? = null
        override fun equals(o: Any?): Boolean {
            if (this === o) return true
            if (o == null || javaClass != o.javaClass) return false
            val that = o as HdAccountWrapper
            return id == that.id
        }

        override fun hashCode(): Int {
            return if (id != null) id.hashCode() else 0
        }
    }

    protected inner class AccountsAdapter(context: Context, resource: Int, objects: List<HdAccountWrapper>) :
        ArrayAdapter<HdAccountWrapper?>(context, resource, objects) {
        private val inflater: LayoutInflater = LayoutInflater.from(getContext())

        @Nonnull
        override fun getView(position: Int, convertView: View?, @Nonnull parent: ViewGroup): View {
            val row: View = convertView ?: inflater.inflate(R.layout.record_row, parent, false)
            val account = getItem(position)
            (row.findViewById<View>(R.id.tvLabel) as TextView).text = account!!.name
            val mbwManager = MbwManager.getInstance(context)
            val walletAccount = mbwManager.getWalletManager(true).getAccount(account.id!!)
            val balance = walletAccount!!.accountBalance
            var balanceString =
                balance.spendable.toStringWithUnit(mbwManager.getDenomination(walletAccount.coinType))
            if (balance.sendingToForeignAddresses.isPositive()) {
                balanceString += " " + String.format(getString(R.string.account_balance_sending_amount), balance.sendingToForeignAddresses.toStringWithUnit(mbwManager.getDenomination(walletAccount.coinType)))
            }
            val drawableForAccount = Utils.getDrawableForAccount(walletAccount, true, resources)
            (row.findViewById<View>(R.id.tvBalance) as TextView).text = balanceString
            row.findViewById<View>(R.id.tvAddress).visibility = View.GONE
            (row.findViewById<View>(R.id.ivIcon) as ImageView).setImageDrawable(drawableForAccount)
            row.findViewById<View>(R.id.tvProgressLayout).visibility =
                View.GONE
            row.findViewById<View>(R.id.tvBackupMissingWarning).visibility =
                View.GONE
            row.findViewById<View>(R.id.tvAccountType).visibility = View.GONE
            return row
        }

    }

    @Subscribe
    open fun onScanError(event: OnScanError) {
        Utils.showSimpleMessageDialog(this, event.errorMessage)
    }

    @Subscribe
    open fun onStatusChanged(event: OnStatusChanged?) {
        updateUi()
    }

    @Subscribe
    open fun onAccountFound(event: OnAccountFound) {
        val acc = HdAccountWrapper()
        acc.id = event.account.accountId
        acc.accountHdKeysPaths = event.account.keysPaths
        val path = event.account.keysPaths.iterator().next()
        if (path == HdKeyPath.BIP32_ROOT) {
            acc.name = getString(R.string.bip32_root_account)
        } else {
            acc.name = String.format(getString(R.string.account_number), path.lastIndex + 1)
        }
        acc.publicKeyNodes = event.account.accountsRoots
        if (!accounts.contains(acc)) {
            accountsAdapter!!.add(acc)
            updateUi()
        }
    }

    @Subscribe
    open fun onPassphraseRequest(event: OnPassphraseRequest?) {
        val pwd = MasterseedPasswordDialog()
        pwd.show(fragmentManager, PASSPHRASE_FRAGMENT_TAG)
    }

    companion object {
        protected const val REQUEST_SEND = 1
        const val PASSPHRASE_FRAGMENT_TAG = "passphrase"
    }
}