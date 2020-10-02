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
package com.mycelium.wallet.activity.main

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.view.*
import android.widget.Button
import android.widget.ExpandableListView
import android.widget.TextView
import androidx.appcompat.view.ActionMode
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.ButterKnife
import com.google.common.base.Preconditions
import com.mrd.bitlib.util.HexUtils
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.TransactionDetailsActivity
import com.mycelium.wapi.wallet.fio.FioGroup
import com.mycelium.wallet.activity.main.adapter.FioRequestArrayAdapter
import com.mycelium.wallet.activity.main.model.fiorequestshistory.FioRequestsHistoryModel
import com.mycelium.wallet.activity.send.BroadcastDialog.Companion.create
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.TransactionLabelChangedHandler
import com.mycelium.wallet.activity.util.getActiveBTCSingleAddressAccounts
import com.mycelium.wallet.event.*
import com.mycelium.wallet.persistence.MetadataStorage
import com.mycelium.wapi.wallet.SyncMode
import com.mycelium.wapi.wallet.Transaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.fio.FioAccount
import com.mycelium.wapi.wallet.fio.getActiveFioAccounts
import com.mycelium.wapi.wallet.fio.getFioAccounts
import com.squareup.otto.Subscribe
import fiofoundation.io.fiosdk.models.TokenPublicAddress
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import kotlinx.android.synthetic.main.fragment_fio_account_mapping.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class FioRequestsHistoryFragment : Fragment() {
    private var _mbwManager: MbwManager? = null
    private var _storage: MetadataStorage? = null
    private var rootView: View? = null
    private val currentActionMode: ActionMode? = null
    private val accountsWithPartialHistory: MutableSet<UUID> = HashSet()

    private val isLoadingPossible = AtomicBoolean(true)

    @JvmField
    @BindView(R.id.tvNoTransactions)
    var noTransactionMessage: TextView? = null


    @JvmField
    @BindView(R.id.btCreateFioRequest)
    var btCreateFioRequest: Button? = null

    private val history: MutableList<FioGroup> = mutableListOf()

    @JvmField
    @BindView(R.id.btRescan)
    var btnReload: View? = null
    private lateinit var adapter: FioRequestArrayAdapter
    private lateinit var model: FioRequestsHistoryModel
    private lateinit var listView: ExpandableListView
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        if (rootView == null) {
            rootView = inflater.inflate(R.layout.fio_request_history_view, container, false)
            ButterKnife.bind(this, rootView!!)
            btnReload!!.setOnClickListener {
                val account = _mbwManager!!.getWalletManager(false).getFioAccounts()[0]
                account.dropCachedData()
                _mbwManager!!.getWalletManager(false)
                        .startSynchronization(SyncMode.NORMAL_FORCED, listOf(account))
            }
            btCreateFioRequest?.setOnClickListener {
                GlobalScope.launch(IO) {

                    //const feeResult = await fioSdk.getFeeForAddPublicAddress(fioAddress);
                    //const btcAddress = 'mrr4GZ7rZwEqJsTQJitdQzsLb6KH3ovehd';
                    //const response = await fioSdk.addPublicAddress(fioAddress, 'BTC', 'BTC', btcAddress, feeResult.fee);


                    val walletManager = MbwManager.getInstance(requireContext()).getWalletManager(false)
                    val receiveAddress = walletManager.getActiveBTCSingleAddressAccounts().first().receiveAddress as BtcAddress
                    val payee = receiveAddress.toString()
                    val selectedAccount = walletManager.getActiveFioAccounts()[0] as FioAccount
                    val fioAddress = Date().time.toString() + "@fiotestnet"
                    selectedAccount.registerFIOAddress(fioAddress)
                    val addPubAddress = selectedAccount.addPubAddress(fioAddress, listOf(TokenPublicAddress(payee, "BTC", "BTC")))
                    selectedAccount.registerFIOAddress(fioAddress)
                    val feeForFunds = selectedAccount.getFeeForFunds(fioAddress)
                    val requestFunds = selectedAccount.requestFunds(
                            "eosdac@fiotestnet",
                            fioAddress,
                            payee,
                            2.0,
                            "BTC",
                            "BTC",
                            feeForFunds.fee)
                    println(requestFunds)
                }
            }

        }

        return rootView
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        listView = rootView!!.findViewById(R.id.lvTransactionHistory)

        adapter = FioRequestArrayAdapter(requireActivity(), history)
        updateWrapper(adapter);
        model!!.fioRequestHistory.observe(this, Observer {
            history.clear()
            history.addAll(it!!)
            //               adapter.sort(new Comparator<FIORequestContent>() {
//                  @Override
//                  public int compare(FIORequestContent ts1, FIORequestContent ts2) {
//                     //TODO migrate to real compare
//                     return ts1.getTimeStamp().compareTo(ts2.getTimeStamp());
//                  }
//               });
            adapter.notifyDataSetChanged()
            showHistory(!history.isEmpty())
            refreshList()
        })

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        model = ViewModelProviders.of(this).get(FioRequestsHistoryModel::class.java)
        setHasOptionsMenu(true)
        super.onCreate(savedInstanceState)
        // cache the addressbook for faster lookup
        model.cacheAddressBook()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        _mbwManager = MbwManager.getInstance(context)
        _storage = _mbwManager!!.metadataStorage
    }

    override fun onResume() {
        MbwManager.getEventBus().register(this)
        super.onResume()
    }

    override fun onPause() {
        MbwManager.getEventBus().unregister(this)
        super.onPause()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (requestCode == SIGN_TRANSACTION_REQUEST_CODE) {
            if (resultCode == Activity.RESULT_OK) {
                val signedTransaction = Preconditions.checkNotNull(intent!!.getSerializableExtra(SendCoinsActivity.SIGNED_TRANSACTION)) as Transaction
                _mbwManager!!.metadataStorage.storeTransactionLabel(HexUtils.toHex(signedTransaction.id), "CPFP")
                val broadcastDialog = create(_mbwManager!!.selectedAccount, false, signedTransaction)
                broadcastDialog!!.show(requireFragmentManager(), "ActivityResultDialog")
            }
        } else {
            super.onActivityResult(requestCode, resultCode, intent)
        }
    }

    @Subscribe
    fun exchangeRateChanged(event: ExchangeRatesRefreshed?) {
        refreshList()
    }

    fun refreshList() {
        listView!!.invalidateViews()
    }

    @Subscribe
    fun fiatCurrencyChanged(event: SelectedCurrencyChanged?) {
        refreshList()
    }

    @Subscribe
    fun addressBookEntryChanged(event: AddressBookChanged?) {
        model!!.cacheAddressBook()
        refreshList()
    }

    @Subscribe
    fun selectedAccountChanged(event: SelectedAccountChanged?) {
        isLoadingPossible.set(true)
        listView!!.setSelection(0)
        //      updateWrapper(adapter);
    }

    @Subscribe
    fun syncStopped(event: SyncStopped?) {
        // It's possible that new transactions came. Adapter should allow to try to scroll
        isLoadingPossible.set(true)
    }

    @Subscribe
    fun tooManyTx(event: TooManyTransactions) {
        accountsWithPartialHistory.add(event.accountId)
    }

    private fun doShowDetails(selected: FIORequestContent?) {
        if (selected == null) {
            return
        }
        // Open transaction details
        val intent = Intent(activity, TransactionDetailsActivity::class.java)
                .putExtra(TransactionDetailsActivity.EXTRA_TXID, selected.fioRequestId)
        startActivity(intent)
    }

    private fun showHistory(hasHistory: Boolean) {
        rootView!!.findViewById<View>(R.id.llNoRecords).visibility = if (hasHistory) View.GONE else View.VISIBLE
        listView!!.visibility = if (hasHistory) View.VISIBLE else View.GONE
        if (accountsWithPartialHistory.contains(_mbwManager!!.selectedAccount.id)) {
            rootView!!.findViewById<View>(R.id.tvWarningNotFullHistory).visibility = View.VISIBLE
        } else {
            rootView!!.findViewById<View>(R.id.tvWarningNotFullHistory).visibility = View.GONE
        }
    }

    private fun updateWrapper(adapter: FioRequestArrayAdapter) {
        this.adapter = adapter;
        listView.setAdapter(adapter);
//        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
//            private static final int OFFSET = 20;
//            private final List<FIORequestContent> toAdd = new ArrayList<>();
//            @Override
//            public void onScrollStateChanged(AbsListView view, int scrollState) {
//                synchronized (toAdd) {
//                    if (!toAdd.isEmpty() && view.getLastVisiblePosition() == history.size() - 1) {
//                        model.getFioRequestHistory().appendList(toAdd);
//                        toAdd.clear();
//                    }
//                }
//            }
//
//            @Override
//            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//                // We should preload data to provide glitch free experience.
//                // If no items loaded we should do nothing, as it's LiveData duty.
//                if (firstVisibleItem + visibleItemCount >= totalItemCount - OFFSET && visibleItemCount != 0) {
//                    boolean toAddEmpty;
//                    synchronized (toAdd) {
//                        toAddEmpty = toAdd.isEmpty();
//                    }
//                    if (toAddEmpty && isLoadingPossible.compareAndSet(true, false)) {
//                        new Preloader(toAdd, _mbwManager.getSelectedAccount(), _mbwManager, totalItemCount,
//                        OFFSET, isLoadingPossible).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
//                    }
//                    if (firstVisibleItem + visibleItemCount == totalItemCount && !toAddEmpty) {
//                        synchronized (toAdd) {
//                            model.getFioRequestHistory().appendList(toAdd);
//                            toAdd.clear();
//                        }
//                    }
//                }
//            }
//        });
    }

    internal class Preloader(private val toAdd: MutableList<FioGroup>, private val account: WalletAccount<*>, private val _mbwManager: MbwManager
                             , private val offset: Int, private val limit: Int, private val success: AtomicBoolean) : AsyncTask<Void?, Void?, Void?>() {
        protected override fun doInBackground(vararg voids: Void?): Void? {
            val account = account as FioAccount
            val preloadedData = account.getRequestsGroups()
            if (this.account == _mbwManager.selectedAccount) {
                synchronized(toAdd) {
                    toAdd.addAll(preloadedData)
                    success.set(toAdd.size == limit)
                }
            }
            return null
        }

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
        //      if (adapter != null && adapter.getCount() > 0) {
//         inflater.inflate(R.menu.export_history, menu);
//      }
    }
    //   @Override
    //   public boolean onOptionsItemSelected(MenuItem item) {
    //      final int itemId = item.getItemId();
    //      switch (itemId) {
    //         case R.id.miExportHistory:
    //            shareTransactionHistory();
    //            return true;
    //      }
    //      return super.onOptionsItemSelected(item);
    //   }
    /**
     * Async task to perform fetching parent transactions of current transaction from server
     */
    //   @SuppressLint("StaticFieldLeak")
    //   private class UpdateParentTask extends AsyncTask<Void, Void, Boolean> {
    //      private Logger logger = Logger.getLogger(UpdateParentTask.class.getSimpleName());
    //      private final Sha256Hash txid;
    //      private final AlertDialog alertDialog;
    //      private final Context context;
    //
    //      UpdateParentTask(Sha256Hash txid, AlertDialog alertDialog, Context context) {
    //         this.txid = txid;
    //         this.alertDialog = alertDialog;
    //         this.context = context;
    //      }
    //
    //      @Override
    //      protected Boolean doInBackground(Void... voids) {
    //         if (_mbwManager.getSelectedAccount() instanceof AbstractBtcAccount) {
    //            AbstractBtcAccount selectedAccount = (AbstractBtcAccount) _mbwManager.getSelectedAccount();
    //            TransactionEx transactionEx = selectedAccount.getTransaction(txid);
    //            BitcoinTransaction transaction = TransactionEx.toTransaction(transactionEx);
    //            try {
    //               selectedAccount.fetchStoreAndValidateParentOutputs(Collections.singletonList(transaction), true);
    //            } catch (WapiException e) {
    //               logger.log(Level.SEVERE, "Can't load parent", e);
    //               return false;
    //            }
    //         }
    //         return true;
    //      }
    //
    //      @Override
    //      protected void onPostExecute(Boolean isResultOk) {
    //         super.onPostExecute(isResultOk);
    //         if (isResultOk) {
    //            final long fee = _mbwManager.getFeeProvider(_mbwManager.getSelectedAccount().getCoinType())
    //                    .getEstimation()
    //                    .getHigh()
    //                    .getValueAsLong();
    //            final UnsignedTransaction unsigned = tryCreateBumpTransaction(txid, fee);
    //            if(unsigned != null) {
    //               long txFee = unsigned.calculateFee();
    //               Value txFeeBitcoinValue = Value.valueOf(Utils.getBtcCoinType(), txFee);
    //               String txFeeString = ValueExtensionsKt.toStringWithUnit(txFeeBitcoinValue,
    //                       _mbwManager.getDenomination(_mbwManager.getSelectedAccount().getCoinType()));
    //               Value txFeeCurrencyValue = _mbwManager.getExchangeRateManager().get(txFeeBitcoinValue,
    //                       _mbwManager.getFiatCurrency(_mbwManager.getSelectedAccount().getCoinType()));
    //               if(!Value.isNullOrZero(txFeeCurrencyValue)) {
    //                  txFeeString += " (" + ValueExtensionsKt.toStringWithUnit(txFeeCurrencyValue,
    //                          _mbwManager.getDenomination(_mbwManager.getSelectedAccount().getCoinType())) + ")";
    //               }
    //               alertDialog.setMessage(context.getString(R.string.description_bump_fee, fee / 1000, txFeeString));
    //               alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.yes), (dialog, which) -> _mbwManager.runPinProtectedFunction(getActivity(), () -> {
    //                  CryptoCurrency cryptoCurrency = _mbwManager.getSelectedAccount().getCoinType();
    //                  BtcTransaction unsignedTransaction = new BtcTransaction(cryptoCurrency, unsigned);
    //                  Intent intent = SignTransactionActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), false, unsignedTransaction);
    //                  startActivityForResult(intent, SIGN_TRANSACTION_REQUEST_CODE);
    //                  dialog.dismiss();
    //                  finishActionMode();
    //               }));
    //               alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
    //            } else {
    //               alertDialog.dismiss();
    //            }
    //         } else {
    //            alertDialog.dismiss();
    //         }
    //      }
    //   }
    //   /**
    //    * This method determins the parent's size and fee and builds a transaction that spends from its outputs but with a fee that lifts the parent and the child to high priority.
    //    * TODO: consider upstream chains of unconfirmed
    //    * TODO: consider parallel attempts to PFP
    //    */
    //   private UnsignedTransaction tryCreateBumpTransaction(Sha256Hash txid, long feePerKB) {
    //      FIORequestContent transaction = _mbwManager.getSelectedAccount().getTxSummary(txid.getBytes());
    //      long txFee = 0;
    //      for(OutputViewModel i : transaction.getInputs()) {
    //         txFee += i.getValue().getValueAsLong();
    //      }
    //      for(OutputViewModel i : transaction.getOutputs()) {
    //         txFee -= i.getValue().getValueAsLong();
    //      }
    //      if(txFee * 1000 / transaction.getRawSize() >= feePerKB) {
    //         makeText(getActivity(), getResources().getString(R.string.bumping_not_necessary), LENGTH_LONG).show();
    //         return null;
    //      }
    //
    //      try {
    //         return ((AbstractBtcAccount)_mbwManager.getSelectedAccount()).createUnsignedCPFPTransaction(txid, feePerKB, txFee);
    //      } catch (InsufficientBtcException e) {
    //         makeText(getActivity(), getResources().getString(R.string.insufficient_funds), LENGTH_LONG).show();
    //      } catch (UnableToBuildTransactionException e) {
    //         makeText(getActivity(), getResources().getString(R.string.unable_to_build_tx), LENGTH_LONG).show();
    //      }
    //      return null;
    //   }
    private val addressLabelChanged = AddressLabelChangedHandler { address, label -> MbwManager.getEventBus().post(AddressBookChanged()) }

    //   private void setTransactionLabel(FIORequestContent record) {
    //      EnterAddressLabelUtil.enterTransactionLabel(requireContext(), Sha256Hash.of(record.getId()), _storage, transactionLabelChanged);
    //   }
    private val transactionLabelChanged = TransactionLabelChangedHandler { txid, label -> MbwManager.getEventBus().post(TransactionLabelChanged()) } //   private void shareTransactionHistory() {

    //      WalletAccount account = _mbwManager.getSelectedAccount();
    //      MetadataStorage metaData = _mbwManager.getMetadataStorage();
    //      try {
    //         String accountLabel = _storage.getLabelByAccount(account.getId()).replaceAll("[^A-Za-z0-9]", "_");
    //
    //         String fileName = "MyceliumExport_" + accountLabel + "_" + System.currentTimeMillis() + ".csv";
    //
    //         List<FIORequestContent> history = account.getTransactionSummaries(0, Integer.MAX_VALUE);
    //
    //         File historyData = DataExport.getTxHistoryCsv(account, history, metaData,
    //             requireActivity().getFileStreamPath(fileName));
    //         PackageManager packageManager = Preconditions.checkNotNull(requireActivity().getPackageManager());
    //         PackageInfo packageInfo = packageManager.getPackageInfo(requireActivity().getPackageName(), PackageManager.GET_PROVIDERS);
    //         for (ProviderInfo info : packageInfo.providers) {
    //            if (info.name.equals("androidx.core.content.FileProvider")) {
    //               String authority = info.authority;
    //               Uri uri = FileProvider.getUriForFile(requireContext(), authority, historyData);
    //               Intent intent = ShareCompat.IntentBuilder.from(requireActivity())
    //                       .setStream(uri)  // uri from FileProvider
    //                       .setType("text/plain")
    //                       .setSubject(getResources().getString(R.string.transaction_history_title))
    //                       .setText(getResources().getString(R.string.transaction_history_title))
    //                       .getIntent()
    //                       .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
    //               List<ResolveInfo> resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
    //               for (ResolveInfo resolveInfo : resInfoList) {
    //                  String packageName = resolveInfo.activityInfo.packageName;
    //                  getActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
    //               }
    //               startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_transaction_history)));
    //            }
    //         }
    //      } catch (IOException | PackageManager.NameNotFoundException e) {
    //         new Toaster(requireActivity()).toast("Export failed. Check your logs", false);
    //         e.printStackTrace();
    //      }
    //   }
    companion object {
        private const val SIGN_TRANSACTION_REQUEST_CODE = 0x12f4
    }
}