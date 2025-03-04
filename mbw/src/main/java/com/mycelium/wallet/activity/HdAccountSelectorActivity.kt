package com.mycelium.wallet.activity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.VisibleForTesting
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.common.collect.Iterables
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.AbstractAccountScanManager
import com.mycelium.wallet.activity.util.MasterseedPasswordSetter
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.AccountScanManager.OnAccountFound
import com.mycelium.wapi.wallet.AccountScanManager.OnPassphraseRequest
import com.mycelium.wapi.wallet.AccountScanManager.OnScanError
import com.mycelium.wapi.wallet.AccountScanManager.OnStatusChanged
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignatureProvider
import com.mycelium.wapi.wallet.btc.bip44.ExternalSignaturesAccountConfig
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.squareup.otto.Subscribe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Serializable
import java.util.UUID
import javax.annotation.Nonnull

abstract class HdAccountSelectorActivity<AccountScanManager : AbstractAccountScanManager> :
    AppCompatActivity(), MasterseedPasswordSetter {
    protected var accounts = ArrayList<HdAccountWrapper>()
    protected var accountsAdapter: AccountsAdapter? = null
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    var masterseedScanManager: AccountScanManager? = null
    protected var txtStatus: TextView? = null
    protected var coinType: CryptoCurrency? = null
    protected abstract fun initMasterseedManager(): AccountScanManager
    private var passDialog: MasterseedPasswordDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        coinType = intent.getSerializableExtra(COIN_TYPE) as CryptoCurrency?
        super.onCreate(savedInstanceState)
        setView()
        val lvAccounts = findViewById<ListView>(R.id.lvAccounts)
        txtStatus = findViewById(R.id.txtStatus)

        // Accounts listview + adapter
        accountsAdapter = AccountsAdapter(this, R.id.lvAccounts, accounts)
        lvAccounts.adapter = accountsAdapter
        lvAccounts.onItemClickListener = accountClickListener()
        // ask user from what blockchain he/she wants to spend from
        masterseedScanManager = initMasterseedManager()
    }

    override fun onStart() {
        super.onStart()
        startBackgroundScan()
        updateUi()
    }

    override fun onDestroy() {
        masterseedScanManager?.stopBackgroundAccountScan()
        super.onDestroy()
    }

    protected fun startBackgroundScan() {
        masterseedScanManager!!.startBackgroundAccountScan { account ->
            val walletManager = MbwManager.getInstance(applicationContext).getWalletManager(true)
            val id = masterseedScanManager!!.createOnTheFlyAccount(
                account.accountsRoots,
                walletManager,
                account.keysPaths.iterator().next().lastIndex
            )
            return@startBackgroundAccountScan id?.let { walletManager.getAccount(id) }?.let {
                it.synchronize(SyncMode.NORMAL_WITHOUT_TX_LOOKUP)
                if (it.hasHadActivity()) {
                    id
                } else {
                    it.dropCachedData()
                    null
                }
            }
        }
    }

    protected abstract fun accountClickListener(): AdapterView.OnItemClickListener?
    protected abstract fun setView()

    override fun onResume() {
        super.onResume()
        MbwManager.getEventBus().register(this)
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    protected open fun updateUi() {
        if (masterseedScanManager!!.currentAccountState == com.mycelium.wapi.wallet.AccountScanManager.AccountStatus.scanning) {
            findViewById<View>(R.id.llStatus).visibility =
                View.VISIBLE
            if (accounts.isNotEmpty()) {
                txtStatus!!.text =
                    String.format(getString(R.string.account_found), Iterables.getLast(accounts).name)
                findViewById<View>(R.id.llSelectAccount).visibility =
                    View.VISIBLE
            }
        } else if (masterseedScanManager!!.currentAccountState == com.mycelium.wapi.wallet.AccountScanManager.AccountStatus.done) {
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

    override fun setPassphrase(passphrase: String?) {
        masterseedScanManager!!.setPassphrase(passphrase)
    }

    data class HdAccountWrapper(
        val id: UUID?,
        val name: String,
        val accountHdKeysPaths: Collection<HdKeyPath>,
        val publicKeyNodes: List<HdKeyNode>,
    ) : Serializable

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

    protected fun createAccountAndFinish(hdKeyNodes: List<HdKeyNode>, accountIndex: Int) {
        lifecycleScope.launch(Dispatchers.Default) {
            val mbwManager = MbwManager.getInstance(this@HdAccountSelectorActivity)
            val acc = mbwManager.getWalletManager(false).createAccounts(
                ExternalSignaturesAccountConfig(
                    hdKeyNodes,
                    (masterseedScanManager as? ExternalSignatureProvider)!!,
                    accountIndex
                )
            ).first()
            mbwManager.metadataStorage.setOtherAccountBackupState(
                acc,
                MetadataStorage.BackupState.IGNORED
            )
            withContext(Dispatchers.Main) {
                setResult(RESULT_OK, Intent().putExtra("account", acc))
                finish()
            }
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
        val path = event.account.keysPaths.iterator().next()
        val acc = HdAccountWrapper(
            event.account.accountId,
            if (path == HdKeyPath.BIP32_ROOT) {
                getString(R.string.bip32_root_account)
            } else {
                String.format(getString(R.string.account_number), path.lastIndex + 1)
            }, event.account.keysPaths,
            event.account.accountsRoots
        )
        if (!accounts.contains(acc)) {
            accountsAdapter!!.add(acc)
            updateUi()
        }
    }

    @Subscribe
    open fun onPassphraseRequest(event: OnPassphraseRequest?) {
        if (passDialog?.isAdded == true) {
            passDialog?.dismissAllowingStateLoss()
        }
        passDialog = MasterseedPasswordDialog()
        passDialog?.show(supportFragmentManager, PASSPHRASE_FRAGMENT_TAG)
    }

    companion object {
        const val REQUEST_SEND = 1
        const val PASSPHRASE_FRAGMENT_TAG = "passphrase"
        const val COIN_TYPE: String = "coinType"

        fun Context.selectCoin(call: (CryptoCurrency) -> Unit) {
            var selectedItem = 0
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.choose_blockchain))
                .setSingleChoiceItems(arrayOf("BTC", "FIO"), 0) { _: DialogInterface?, i: Int ->
                    selectedItem = i
                }
                .setPositiveButton(this.getString(R.string.ok)) { _: DialogInterface?, _: Int ->
                    call(
                        if (selectedItem == 0) {
                            Utils.getBtcCoinType()
                        } else {
                            Utils.getFIOCoinType()
                        }
                    )
                }
                .setNegativeButton(this.getString(R.string.cancel)) { dialog: DialogInterface?, _: Int ->
                    dialog?.dismiss()
                }
                .setCancelable(false)
                .show()
        }
    }
}