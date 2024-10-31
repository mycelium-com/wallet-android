package com.mycelium.wallet.activity.main

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.widget.AbsListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.core.app.ShareCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import com.google.common.base.Preconditions
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientBtcException
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException.BuildError
import com.mrd.bitlib.UnsignedTransaction
import com.mrd.bitlib.util.HexUtils
import com.mrd.bitlib.util.Sha256Hash
import com.mycelium.wallet.DataExport
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.main.adapter.TransactionArrayAdapter
import com.mycelium.wallet.activity.main.loader.Preloader
import com.mycelium.wallet.activity.main.model.transactionhistory.TransactionHistoryModel
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.send.BroadcastDialog
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.activity.txdetails.TransactionDetailsActivity
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.TransactionLabelChangedHandler
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.MainTransactionHistoryViewBinding
import com.mycelium.wallet.event.*
import com.mycelium.wapi.api.WapiException
import com.mycelium.wapi.model.TransactionEx
import com.mycelium.wapi.wallet.*
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.WalletBtcAccount
import com.mycelium.wapi.wallet.coins.Value.Companion.isNullOrZero
import com.mycelium.wapi.wallet.coins.Value.Companion.valueOf
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account
import com.mycelium.wapi.wallet.fio.FIOOBTransaction
import com.mycelium.wapi.wallet.fio.FioAccount
import com.squareup.otto.Subscribe
import kotlinx.coroutines.*
import java.io.IOException
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

// TODO move to recyclerview and redesign tx loading
class TransactionHistoryFragment : Fragment() {
    private var currentActionMode: ActionMode? = null
    private val accountsWithPartialHistory: MutableSet<UUID> = HashSet()

    /**
     * This field shows if [Preloader] may be started (initial - true).
     * After [TransactionHistoryFragment.selectedAccountChanged] it's true
     * Before [Preloader] started it's set to false to prevent multiple-loadings.
     * When [Preloader]#doInBackground() finishes it's routine it's setting true if limit was reached, else false
     */
    private val isLoadingPossible = AtomicBoolean(true)

    private val history = mutableListOf<TransactionSummary>()

    private var adapter: TransactionHistoryAdapter? = null
    private val model: TransactionHistoryModel by viewModels()
    private var binding: MainTransactionHistoryViewBinding? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        // cache the addressbook for faster lookup
        model.cacheAddressBook()
        val accountId = arguments?.getSerializable("accountId") as UUID?
        model.account.value = if (accountId != null) model.mbwManager.getWalletManager(false).getAccount(accountId)!! else model.mbwManager.selectedAccount
        MbwManager.getEventBus().register(this)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
            MainTransactionHistoryViewBinding.inflate(inflater).apply {
                binding = this
            }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        if (adapter == null) {
            adapter = TransactionHistoryAdapter(activity, history, model.fioMetadataMap)
            updateWrapper(adapter)
            model.transactionHistory.observe(viewLifecycleOwner, Observer { transaction ->
                history.clear()
                history.addAll(transaction)
                adapter?.sort { ts1, ts2 ->
                    if (ts1.confirmations == 0 && ts2.confirmations == 0) {
                        ts2.timestamp.compareTo(ts1.timestamp)
                    } else if (ts1.confirmations == 0) {
                        -1
                    } else if (ts2.confirmations == 0) {
                        1
                    } else {
                        ts2.timestamp.compareTo(ts1.timestamp)
                    }
                }
                adapter?.notifyDataSetChanged()
                showHistory(history.isNotEmpty())
                refreshList()
            })
        }
        binding?.btRescan?.setOnClickListener {
            model.account.value?.let { account ->
                account.dropCachedData()
                model.mbwManager.getWalletManager(false)
                        .startSynchronization(SyncMode.NORMAL_FORCED, listOf(account))
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == SIGN_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val signedTransaction = Preconditions.checkNotNull(intent!!.getSerializableExtra(SendCoinsActivity.SIGNED_TRANSACTION)) as Transaction
                model.storage.storeTransactionLabel(HexUtils.toHex(signedTransaction.id), "CPFP")
                BroadcastDialog.create(model.account.value!!, false, signedTransaction)
                        .show(requireFragmentManager(), "ActivityResultDialog")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    override fun onDestroy() {
        MbwManager.getEventBus().unregister(this)
        super.onDestroy()
    }

    @Subscribe
    fun exchangeRateChanged(event: ExchangeRatesRefreshed) {
        refreshList()
    }

    fun refreshList() {
        binding?.lvTransactionHistory?.invalidateViews()
    }

    @Subscribe
    fun fiatCurrencyChanged(event: SelectedCurrencyChanged) {
        refreshList()
    }

    @Subscribe
    fun addressBookEntryChanged(event: AddressBookChanged) {
        model.cacheAddressBook()
        refreshList()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged) {
        if (arguments?.containsKey("accountId") != true) {
            model.account.value = model.mbwManager.selectedAccount
        }
        isLoadingPossible.set(true)
        binding?.lvTransactionHistory?.setSelection(0)
        updateWrapper(adapter)
    }

    @Subscribe
    fun syncStopped(event: SyncStopped) {
        // It's possible that new transactions came. Adapter should allow to try to scroll
        isLoadingPossible.set(true)
    }

    @Subscribe
    fun tooManyTx(event: TooManyTransactions) {
        accountsWithPartialHistory.add(event.accountId)
    }

    private fun doShowDetails(selected: TransactionSummary?) {
        if (selected == null) {
            return
        }
        // Open transaction details
        startActivity(Intent(activity, TransactionDetailsActivity::class.java)
                .putExtra(TransactionDetailsActivity.EXTRA_TXID, selected.id)
                .putExtra(TransactionDetailsActivity.ACCOUNT_ID, model.account.value!!.id))
    }

    private fun showHistory(hasHistory: Boolean) {
        binding?.llNoRecords?.visibility = if (hasHistory) View.GONE else View.VISIBLE
        binding?.lvTransactionHistory?.visibility = if (hasHistory) View.VISIBLE else View.GONE
        binding?.tvWarningNotFullHistory?.visibility = if (accountsWithPartialHistory.contains(model.account.value!!.id)) View.VISIBLE else View.GONE
    }

    private fun updateWrapper(adapter: TransactionHistoryAdapter?) {
        this.adapter = adapter
        binding?.lvTransactionHistory?.adapter = adapter
        binding?.lvTransactionHistory?.setOnScrollListener(object : AbsListView.OnScrollListener {

            private val toAdd = mutableListOf<TransactionSummary>()

            override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {
                synchronized(toAdd) {
                    if (toAdd.isNotEmpty() && view.lastVisiblePosition == history.size - 1) {
                        model.txs?.appendList(toAdd)
                        toAdd.clear()
                    }
                }
            }

            override fun onScroll(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                // We should preload data to provide glitch free experience.
                // If no items loaded we should do nothing, as it's LiveData duty.
                if (firstVisibleItem + visibleItemCount >= totalItemCount - OFFSET && visibleItemCount != 0) {
                    var toAddEmpty: Boolean
                    synchronized(toAdd) { toAddEmpty = toAdd.isEmpty() }
                    if (toAddEmpty && isLoadingPossible.compareAndSet(true, false)) {
                        Preloader(toAdd, model.fioMetadataMap, model.account.value!!, model.mbwManager, totalItemCount,
                                OFFSET, isLoadingPossible).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
                    }
                    if (firstVisibleItem + visibleItemCount == totalItemCount && !toAddEmpty) {
                        synchronized(toAdd) {
                            model.txs?.appendList(toAdd)
                            toAdd.clear()
                        }
                    }
                }
            }
        })
    }

    override fun setUserVisibleHint(isVisibleToUser: Boolean) {
        super.setUserVisibleHint(isVisibleToUser)
        if (!isVisibleToUser) {
            finishActionMode()
        }
    }

    private fun finishActionMode() {
        currentActionMode?.finish()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        if (adapter?.count ?: 0 > 0) {
            inflater.inflate(R.menu.export_history, menu)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
            when (item.itemId) {
                R.id.miExportHistory -> {
                    shareTransactionHistory()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }

    private inner class TransactionHistoryAdapter(context: Context?,
                                                  transactions: List<TransactionSummary?>?,
                                                  private val fioMetadataMap: Map<String, FIOOBTransaction>)
        : TransactionArrayAdapter(context, transactions, this@TransactionHistoryFragment, model.addressBook, false) {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val rowView = super.getView(position, convertView, parent)

            // Make sure we are still added
            if (!isAdded) {
                // We have observed that the fragment can be disconnected at this
                // point
                return rowView
            }
            val record = Preconditions.checkNotNull(getItem(position))
            val otherFioName = rowView.findViewById<TextView>(R.id.otherFioName)
            val fioIcon = rowView.findViewById<View>(R.id.fioIcon)
            val tvFioMemo = rowView.findViewById<TextView>(R.id.tvFioMemo)
            fioMetadataMap[record!!.idHex]?.let { fioObTransaction ->
                if (record.isIncoming) {
                    otherFioName.text = getString(R.string.transaction_from_address_prefix, fioObTransaction.fromFioName)
                } else {
                    otherFioName.text = getString(R.string.transaction_to_address_prefix, fioObTransaction.toFioName)
                }
                if (fioObTransaction.memo.isEmpty()) {
                    tvFioMemo.visibility = View.GONE
                } else {
                    tvFioMemo.visibility = View.VISIBLE
                    tvFioMemo.text = fioObTransaction.memo
                }
                otherFioName.visibility = View.VISIBLE
                fioIcon.visibility = View.VISIBLE
            } ?: run {
                tvFioMemo.visibility = View.GONE
                otherFioName.visibility = View.GONE
                fioIcon.visibility = View.GONE
            }
            rowView.setOnClickListener {
                currentActionMode = (activity as AppCompatActivity?)?.startSupportActionMode(object : ActionMode.Callback {
                    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                        actionMode.menuInflater.inflate(R.menu.transaction_history_context_menu, menu)
                        //we only allow address book entries for outgoing transactions
                        updateActionBar(actionMode, menu)
                        return true
                    }

                    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
                        updateActionBar(actionMode, menu)
                        return true
                    }

                    //We need implementations of GenericTransactionSummary for using something like
                    //hasDetails|canCancel
                    //I set default values
                    private fun updateActionBar(actionMode: ActionMode, menu: Menu) {
                        Preconditions.checkNotNull(menu.findItem(R.id.miShowDetails))
                        Preconditions.checkNotNull(menu.findItem(R.id.miAddToAddressBook)).isVisible = !record.isIncoming && record.destinationAddresses.size > 0
                        if (model.account.value is Bip44BCHAccount || model.account.value is SingleAddressBCHAccount
                                || model.account.value is AbstractEthERC20Account || model.account.value is FioAccount) {
                            Preconditions.checkNotNull(menu.findItem(R.id.miCancelTransaction)).isVisible = false
                            Preconditions.checkNotNull(menu.findItem(R.id.miRebroadcastTransaction)).isVisible = false
                            Preconditions.checkNotNull(menu.findItem(R.id.miBumpFee)).isVisible = false
                            Preconditions.checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction)).isVisible = false
                            Preconditions.checkNotNull(menu.findItem(R.id.miShare)).isVisible = false
                        } else {
                            Preconditions.checkNotNull(menu.findItem(R.id.miCancelTransaction)).isVisible = record.canCancel()
                            Preconditions.checkNotNull(menu.findItem(R.id.miRebroadcastTransaction)).isVisible = record.confirmations == 0
                            Preconditions.checkNotNull(menu.findItem(R.id.miBumpFee)).isVisible = record.confirmations == 0 && model.account.value?.canSpend() ?: false
                            Preconditions.checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction)).isVisible = record.confirmations == 0
                            Preconditions.checkNotNull(menu.findItem(R.id.miShare)).isVisible = true
                        }
                        if (model.account.value is AbstractEthERC20Account) {
                            Preconditions.checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction)).isVisible = record.confirmations == 0
                        }
                        currentActionMode = actionMode
                        binding?.lvTransactionHistory?.setItemChecked(position, true)
                    }

                    override fun onActionItemClicked(actionMode: ActionMode, menuItem: MenuItem): Boolean {
                        when (menuItem.itemId) {
                            R.id.miShowDetails -> {
                                doShowDetails(record)
                                finishActionMode()
                                return true
                            }
                            R.id.miSetLabel -> {
                                setTransactionLabel(record)
                                finishActionMode()
                            }
                            R.id.miAddToAddressBook -> {
                                var defaultName: String? = ""
                                if (model.account.value is ColuAccount) {
                                    defaultName = (model.account.value as ColuAccount).coluLabel
                                }
                                if(record.destinationAddresses.size > 0) {
                                    val address = record.destinationAddresses[0]
                                    EnterAddressLabelUtil.enterAddressLabel(requireContext(), model.storage,
                                            address, defaultName, addressLabelChanged)
                                }
                            }
                            R.id.miCancelTransaction -> AlertDialog.Builder(context)
                                    .setTitle(_context.getString(R.string.remove_queued_transaction_title))
                                    .setMessage(_context.getString(R.string.remove_queued_transaction))
                                    .setPositiveButton(R.string.yes) { dialog, which ->
                                        val okay = (model.account.value as WalletBtcAccount).cancelQueuedTransaction(Sha256Hash.of(record.id))
                                        dialog.dismiss()
                                        if (okay) {
                                            Utils.showSimpleMessageDialog(context, _context.getString(R.string.remove_queued_transaction_hint))
                                        } else {
                                            Toaster(requireActivity()).toast(_context.getString(R.string.remove_queued_transaction_error), false)
                                        }
                                        finishActionMode()
                                    }
                                    .setNegativeButton(R.string.no) { dialog, _ -> dialog.dismiss() }
                                    .create().show()
                            R.id.miDeleteUnconfirmedTransaction -> AlertDialog.Builder(context)
                                    .setTitle(_context.getString(R.string.delete_unconfirmed_transaction_title))
                                    .setMessage(_context.getString(R.string.warning_delete_unconfirmed_transaction))
                                    .setPositiveButton(R.string.yes) { dialog, _ ->
                                        if (model.account.value is WalletBtcAccount) {
                                            (model.account.value as WalletBtcAccount).deleteTransaction(Sha256Hash.of(record.id))
                                            dialog.dismiss()
                                            finishActionMode()
                                        } else if (model.account.value is AbstractEthERC20Account) {
                                            (model.account.value as AbstractEthERC20Account).deleteTransaction("0x" + HexUtils.toHex(record.id))
                                            dialog.dismiss()
                                            finishActionMode()
                                        }
                                    }
                                    .setNegativeButton(R.string.no) { dialog, which -> dialog.dismiss() }
                                    .create().show()
                            R.id.miRebroadcastTransaction -> AlertDialog.Builder(activity)
                                    .setTitle(_context.getString(R.string.rebroadcast_transaction_title))
                                    .setMessage(_context.getString(R.string.description_rebroadcast_transaction))
                                    .setPositiveButton(R.string.yes) { dialog, _ ->
                                        BroadcastDialog.create(model.account.value!!, false, record)
                                                .show(requireFragmentManager(), "broadcast")
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton(R.string.no) { dialog, which -> dialog.dismiss() }
                                    .create().show()
                            R.id.miBumpFee -> {
                                val alertDialog = AlertDialog.Builder(activity)
                                        .setTitle(_context.getString(R.string.bump_fee_title))
                                        .setMessage(_context.getString(R.string.description_bump_fee_placeholder))
                                        .setPositiveButton(R.string.yes, null)
                                        .setNegativeButton(R.string.no, null).create()
                                val job = GlobalScope.launch(Dispatchers.Main) {
                                    val result = withContext(Dispatchers.IO) {
                                        updateParentTask(Sha256Hash.of(record.id))
                                    }
                                    updateUI(result, Sha256Hash.of(record.id), alertDialog, _context)
                                }
                                alertDialog.setOnDismissListener { job.cancel() }
                                alertDialog.show()
                                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = false
                            }
                            R.id.miShare -> AlertDialog.Builder(activity)
                                    .setTitle(R.string.share_transaction_manually_title)
                                    .setMessage(R.string.share_transaction_manually_description)
                                    .setPositiveButton(R.string.yes) { dialog, _ ->
                                        // TODO refactor: exit dialog if getTx() returns null
                                        val transaction = HexUtils.toHex(model.account.value!!.getTx(record.id)!!.txBytes())
                                        val shareIntent = Intent(Intent.ACTION_SEND)
                                        shareIntent.type = "text/plain"
                                        shareIntent.putExtra(Intent.EXTRA_TEXT, transaction)
                                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_transaction)))
                                        dialog.dismiss()
                                    }
                                    .setNegativeButton(R.string.no, null)
                                    .create().show()
                        }
                        return false
                    }

                    override fun onDestroyActionMode(actionMode: ActionMode) {
                        binding?.lvTransactionHistory?.setItemChecked(position, false)
                        currentActionMode = null
                    }
                })
            }
            return rowView
        }
    }

    /**
     * Async task to perform fetching parent transactions of current transaction from server
     */
    private suspend fun updateParentTask(txid: Sha256Hash): Boolean {
        val logger = Logger.getLogger(TransactionHistoryFragment::class.java.simpleName)

        if (model.account.value is AbstractBtcAccount) {
            val currentAccount = model.account.value as AbstractBtcAccount
            val transactionEx = currentAccount.getTransaction(txid)
            val transaction = TransactionEx.toTransaction(transactionEx)
            try {
                currentAccount.fetchStoreAndValidateParentOutputs(listOf(transaction), true)
            } catch (e: WapiException) {
                logger.log(Level.SEVERE, "Can't load parent", e)
                return false
            }
        }
        return true
    }

    private fun updateUI(
        isResultOk: Boolean,
        txid: Sha256Hash,
        alertDialog: AlertDialog,
        context: Context
    ) {
        if (isResultOk) {
            val fee = model.mbwManager.getFeeProvider(model.account.value!!.coinType)
                .estimation
                .high
                .valueAsLong
            val unsigned = tryCreateBumpTransaction(txid, fee)
            if (unsigned != null) {
                val txFee = unsigned.calculateFee()
                val txFeeBitcoinValue = valueOf(Utils.getBtcCoinType(), txFee)
                var txFeeString = txFeeBitcoinValue.toStringWithUnit(model.mbwManager.getDenomination(model.account.value!!.coinType))
                val txFeeCurrencyValue = model.mbwManager.exchangeRateManager[txFeeBitcoinValue, model.mbwManager.getFiatCurrency(model.account.value!!.coinType)]
                if (!isNullOrZero(txFeeCurrencyValue)) {
                    txFeeString += " (${txFeeCurrencyValue.toStringWithUnit(model.mbwManager.getDenomination(model.account.value!!.coinType))}"
                }
                alertDialog.setMessage(context.getString(R.string.description_bump_fee, fee / 1000, txFeeString))
                alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.yes)) { dialog: DialogInterface, which: Int ->
                    model.mbwManager.runPinProtectedFunction(activity) {
                        val cryptoCurrency = model.account.value!!.coinType
                        val unsignedTransaction = BtcTransaction(cryptoCurrency, unsigned)
                        val intent = SignTransactionActivity.getIntent(activity, model.account.value!!.id, false, unsignedTransaction)
                        startActivityForResult(intent, SIGN_TRANSACTION_REQUEST_CODE)
                        dialog.dismiss()
                        finishActionMode()
                    }
                }
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).isEnabled = true
            } else {
                alertDialog.dismiss()
            }
        } else {
            alertDialog.dismiss()
        }
    }

    /**
     * This method determins the parent's size and fee and builds a transaction that spends from its outputs but with a fee that lifts the parent and the child to high priority.
     * TODO: consider upstream chains of unconfirmed
     * TODO: consider parallel attempts to PFP
     */
    private fun tryCreateBumpTransaction(txid: Sha256Hash, feePerKB: Long): UnsignedTransaction? {
        val transaction = model.account.value!!.getTxSummary(txid.bytes) // TODO refactor: return if getTxSummary() returns null
        val txFee = transaction!!.inputs.map { it.value.valueAsLong }.sum() -
                transaction.outputs.map { it.value.valueAsLong }.sum()
        if (txFee * 1000 / transaction.rawSize >= feePerKB) {
            Toaster(requireActivity()).toast(resources.getString(R.string.bumping_not_necessary), false)
            return null
        }
        try {
            return (model.account.value as AbstractBtcAccount).createUnsignedCPFPTransaction(txid, feePerKB, txFee)
        } catch (e: InsufficientBtcException) {
            Toaster(requireActivity()).toast(R.string.insufficient_funds, false)
        } catch (e: UnableToBuildTransactionException) {
            val message = when (e.code) {
                BuildError.NO_UTXO -> resources.getString(R.string.no_utxo)
                BuildError.PARENT_NEEDS_NO_BOOSTING -> resources.getString(R.string.parent_needs_no_boosting)
                else -> resources.getString(R.string.unable_to_build_tx, e.message)
            }
            Toaster(requireActivity()).toast(message, false)
        }
        return null
    }

    private val addressLabelChanged = AddressLabelChangedHandler { address, label -> MbwManager.getEventBus().post(AddressBookChanged()) }
    private fun setTransactionLabel(record: TransactionSummary?) {
        EnterAddressLabelUtil.enterTransactionLabel(requireContext(), Sha256Hash.of(record!!.id), model.storage, transactionLabelChanged)
    }

    private val transactionLabelChanged = TransactionLabelChangedHandler { _, _ -> MbwManager.getEventBus().post(TransactionLabelChanged()) }
    private fun shareTransactionHistory() {
        try {
            val accountLabel = model.storage.getLabelByAccount(model.account.value!!.id).replace("[^A-Za-z0-9]".toRegex(), "_")
            val fileName = "MyceliumExport_" + accountLabel + "_" + System.currentTimeMillis() + ".csv"
            val history = model.account.value!!.getTransactionSummaries(0, Int.MAX_VALUE)
            val historyData = DataExport.getTxHistoryCsv(model.account.value, history, model.storage,
                    requireActivity().getFileStreamPath(fileName))
            val packageManager = Preconditions.checkNotNull(requireActivity().packageManager)
            packageManager.getPackageInfo(requireActivity().packageName, PackageManager.GET_PROVIDERS)
                    .providers?.find { it.name == "androidx.core.content.FileProvider" }?.authority?.let { authority ->
                        val uri = FileProvider.getUriForFile(requireContext(), authority, historyData)
                        val intent = ShareCompat.IntentBuilder.from(requireActivity())
                                .setStream(uri) // uri from FileProvider
                                .setType("text/plain")
                                .setSubject(resources.getString(R.string.transaction_history_title))
                                .setText(resources.getString(R.string.transaction_history_title))
                                .intent
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
                                .forEach { resolveInfo ->
                                    requireActivity().grantUriPermission(resolveInfo.activityInfo.packageName,
                                            uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                }
                        startActivity(Intent.createChooser(intent, resources.getString(R.string.share_transaction_history)))
                    }
        } catch (e: IOException) {
            Toaster(requireActivity()).toast("Export failed. Check your logs", false)
            e.printStackTrace()
        } catch (e: PackageManager.NameNotFoundException) {
            Toaster(requireActivity()).toast("Export failed. Check your logs", false)
            e.printStackTrace()
        }
    }

    companion object {
        private const val SIGN_TRANSACTION_REQUEST_CODE = 0x12f4
        private const val OFFSET = 20
    }
}