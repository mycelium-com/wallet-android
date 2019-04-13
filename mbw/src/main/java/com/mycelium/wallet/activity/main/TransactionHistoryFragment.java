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

package com.mycelium.wallet.activity.main;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException;
import com.mrd.bitlib.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.DataExport;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.activity.main.adapter.TransactionArrayAdapter;
import com.mycelium.wallet.activity.main.model.transactionhistory.TransactionHistoryModel;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.send.BroadcastTransactionActivity;
import com.mycelium.wallet.activity.send.SignTransactionActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.coinapult.CoinapultTransactionSummary;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.event.TransactionLabelChanged;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.WapiException;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionEx;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import butterknife.BindView;
import butterknife.ButterKnife;

import static android.app.Activity.RESULT_OK;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.makeText;
import static com.google.common.base.Preconditions.checkNotNull;

public class TransactionHistoryFragment extends Fragment {
   private static final int SIGN_TRANSACTION_REQUEST_CODE = 0x12f4;
   private static final int BROADCAST_REQUEST_CODE = SIGN_TRANSACTION_REQUEST_CODE + 1;
   private MbwManager _mbwManager;
   private MetadataStorage _storage;
   private View _root;
   private ActionMode currentActionMode;
   /**
    * This field shows if {@link Preloader} may be started (initial - true).
    * After {@link TransactionHistoryFragment#selectedAccountChanged} it's true
    * Before {@link Preloader} started it's set to false to prevent multiple-loadings.
    * When {@link Preloader#doInBackground(Void...)} finishes it's routine it's setting true if limit was reached, else false
    */
   private final AtomicBoolean isLoadingPossible = new AtomicBoolean(true);
   @BindView(R.id.no_transaction_message)
   TextView noTransactionMessage;
   private List<TransactionSummary> history = new ArrayList<>();

   @BindView(R.id.btRescan)
   View btnReload;

   private TransactionHistoryAdapter adapter;
   private TransactionHistoryModel model;
   private ListView listView;

   @Override
   public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      if (_root == null) {
         _root = inflater.inflate(R.layout.main_transaction_history_view, container, false);
         ButterKnife.bind(this, _root);
         btnReload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               _mbwManager.getSelectedAccount().dropCachedData();
               _mbwManager.getWalletManager(false).startSynchronization();
            }
         });
      }
      return _root;
   }

   @Override
   public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
      listView = _root.findViewById(R.id.lvTransactionHistory);
      if (adapter == null) {
         adapter = new TransactionHistoryAdapter(getActivity(), history);
         updateWrapper(adapter);
         model.getTransactionHistory().observe(this, new Observer<Set<? extends TransactionSummary>>() {
            @Override
            public void onChanged(@Nullable Set<? extends TransactionSummary> transactionSummaries) {
               history.clear();
               history.addAll(transactionSummaries);
               adapter.sort(new Comparator<TransactionSummary>() {
                  @Override
                  public int compare(TransactionSummary ts1, TransactionSummary ts2) {
                     return Long.compare(ts2.time, ts1.time);
                  }
               });
               adapter.notifyDataSetChanged();
               showHistory(!history.isEmpty());
               refreshList();
            }
         });
      }
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      model = ViewModelProviders.of(this).get(TransactionHistoryModel.class);
      setHasOptionsMenu(true);
      super.onCreate(savedInstanceState);
      // cache the addressbook for faster lookup
      model.cacheAddressBook();
   }

   @Override
   public void onAttach(Context context) {
      super.onAttach(context);
      _mbwManager = MbwManager.getInstance(context);
      _storage = _mbwManager.getMetadataStorage();
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SIGN_TRANSACTION_REQUEST_CODE) {
         if (resultCode == RESULT_OK) {
            Transaction transaction = (Transaction) Preconditions.checkNotNull(intent.getSerializableExtra("signedTx"));
            BroadcastTransactionActivity.callMe(getActivity(), _mbwManager.getSelectedAccount().getId(), false, transaction, "CPFP", null, BROADCAST_REQUEST_CODE);
         }
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   @Subscribe
   public void exchangeRateChanged(ExchangeRatesRefreshed event) {
      refreshList();
   }

   void refreshList() {
      listView.invalidateViews();
   }

   @Subscribe
   public void fiatCurrencyChanged(SelectedCurrencyChanged event) {
      refreshList();
   }

   @Subscribe
   public void addressBookEntryChanged(AddressBookChanged event) {
      model.cacheAddressBook();
      refreshList();
   }

   @Subscribe
   public void selectedAccountChanged(SelectedAccountChanged event) {
      isLoadingPossible.set(true);
      listView.setSelection(0);
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      // It's possible that new transactions came. Adapter should allow to try to scroll
      isLoadingPossible.set(true);
   }

   private void doShowDetails(TransactionSummary selected) {
      if (selected == null) {
         return;
      }
      // Open transaction details
      Intent intent = new Intent(getActivity(), TransactionDetailsActivity.class);
      intent.putExtra("transaction", selected.txid);
      startActivity(intent);
   }

   void showHistory(boolean visible) {
      _root.findViewById(R.id.llNoRecords).setVisibility(visible ? View.GONE : View.VISIBLE);
      listView.setVisibility(visible ? View.VISIBLE : View.GONE);
   }

   public void updateWrapper(TransactionHistoryAdapter adapter) {
      this.adapter = adapter;
      listView.setAdapter(adapter);
      listView.setOnScrollListener(new AbsListView.OnScrollListener() {
         private static final int OFFSET = 20;
         private final List<TransactionSummary> toAdd = new ArrayList<>();
         private WalletAccount forAccount = _mbwManager.getSelectedAccount();
         @Override
         public void onScrollStateChanged(AbsListView view, int scrollState) {
            synchronized (toAdd) {
               if (!toAdd.isEmpty() && view.getLastVisiblePosition() == history.size() - 1) {
                  model.getTransactionHistory().appendList(toAdd, forAccount);
                  toAdd.clear();
               }
            }
         }

         @Override
         public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            // We should preload data to provide glitch free experience.
            // If no items loaded we should do nothing, as it's LiveData duty.
            if (firstVisibleItem + visibleItemCount >= totalItemCount - OFFSET && visibleItemCount != 0) {
               boolean toAddEmpty;
               synchronized (toAdd) {
                  toAddEmpty = toAdd.isEmpty();
               }
               if (toAddEmpty && isLoadingPossible.compareAndSet(true, false)) {
                  forAccount = _mbwManager.getSelectedAccount();
                  new Preloader(toAdd, forAccount, _mbwManager, totalItemCount,
                          OFFSET, isLoadingPossible).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
               }
               if (firstVisibleItem + visibleItemCount == totalItemCount && !toAddEmpty) {
                  synchronized (toAdd) {
                     model.getTransactionHistory().appendList(toAdd, forAccount);
                     toAdd.clear();
                  }
               }
            }
         }
      });
   }

   static class Preloader extends AsyncTask<Void, Void, Void> {
      private final List<TransactionSummary> toAdd;
      private final WalletAccount account;
      private final int offset;
      private final int limit;
      private final AtomicBoolean success;
      private final MbwManager _mbwManager;

      Preloader(List<TransactionSummary> toAdd, WalletAccount account, MbwManager _mbwManager
              , int offset, int limit, AtomicBoolean success) {
         this.toAdd = toAdd;
         this.account = account;
         this.offset = offset;
         this.limit = limit;
         this.success = success;
         this._mbwManager = _mbwManager;
      }

      @Override
      protected Void doInBackground(Void... voids) {
         List<TransactionSummary> preloadedData = account.getTransactionHistory(offset, limit);
         if(account.equals(_mbwManager.getSelectedAccount())) {
            synchronized (toAdd) {
               toAdd.addAll(preloadedData);
               success.set(toAdd.size() == limit);
            }
         }
         return null;
      }
   }

   @Override
   public void setUserVisibleHint(boolean isVisibleToUser) {
      super.setUserVisibleHint(isVisibleToUser);
      if (!isVisibleToUser) {
         finishActionMode();
      }
   }

   private void finishActionMode() {
      if (currentActionMode != null) {
         currentActionMode.finish();
      }
   }

   @Override
   public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
      super.onCreateOptionsMenu(menu, inflater);
      if (adapter != null && adapter.getCount() > 0) {
         inflater.inflate(R.menu.export_history, menu);
      }
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      final int itemId = item.getItemId();
      switch (itemId) {
         case R.id.miExportHistory:
            shareTransactionHistory();
            return true;
      }
      return super.onOptionsItemSelected(item);
   }

   private class TransactionHistoryAdapter extends TransactionArrayAdapter {
      TransactionHistoryAdapter(Context context, List<TransactionSummary> transactions) {
         super(context, transactions, TransactionHistoryFragment.this, model.getAddressBook(), false);
      }

      @NonNull
      @Override
      public View getView(final int position, View convertView, ViewGroup parent) {
         View rowView = super.getView(position, convertView, parent);

         // Make sure we are still added
         if (!isAdded()) {
            // We have observed that the fragment can be disconnected at this
            // point
            return rowView;
         }

         final TransactionSummary record = checkNotNull(getItem(position));
         final AppCompatActivity appCompatActivity = (AppCompatActivity) getActivity();

         rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
               currentActionMode = appCompatActivity.startSupportActionMode(new ActionMode.Callback() {
                  @Override
                  public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                     actionMode.getMenuInflater().inflate(R.menu.transaction_history_context_menu, menu);
                     //we only allow address book entries for outgoing transactions
                     updateActionBar(actionMode, menu);
                     return true;
                  }

                  @Override
                  public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                     updateActionBar(actionMode, menu);
                     return true;
                  }

                  private void updateActionBar(ActionMode actionMode, Menu menu) {
                     checkNotNull(menu.findItem(R.id.miShowDetails)).setVisible(record.hasDetails());
                     checkNotNull(menu.findItem(R.id.miShowCoinapultDebug)).setVisible(record.canCoinapult());
                     checkNotNull(menu.findItem(R.id.miAddToAddressBook)).setVisible(record.hasAddressBook());
                     if((_mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHBIP44
                         || _mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHSINGLEADDRESS)) {
                       checkNotNull(menu.findItem(R.id.miCancelTransaction)).setVisible(false);
                       checkNotNull(menu.findItem(R.id.miRebroadcastTransaction)).setVisible(false);
                       checkNotNull(menu.findItem(R.id.miBumpFee)).setVisible(false);
                       checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction)).setVisible(false);
                       checkNotNull(menu.findItem(R.id.miShare)).setVisible(false);
                     } else {
                       checkNotNull(menu.findItem(R.id.miCancelTransaction)).setVisible(record.canCancel());
                       checkNotNull(menu.findItem(R.id.miRebroadcastTransaction))
                           .setVisible((record.confirmations == 0) && !record.canCoinapult());
                       checkNotNull(menu.findItem(R.id.miBumpFee))
                           .setVisible((record.confirmations == 0) && !record.canCoinapult() && (_mbwManager.getSelectedAccount().canSpend()));
                       checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction))
                           .setVisible(record.confirmations == 0);
                       checkNotNull(menu.findItem(R.id.miShare)).setVisible(!record.canCoinapult());

                     }
                     currentActionMode = actionMode;
                     listView.setItemChecked(position, true);
                  }

                  @Override
                  public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                     final int itemId = menuItem.getItemId();
                     if (itemId == R.id.miShowCoinapultDebug) {
                        if (record instanceof CoinapultTransactionSummary) {
                           final CoinapultTransactionSummary summary = (CoinapultTransactionSummary) record;
                           new AlertDialog.Builder(_context)
                                 .setMessage(summary.input.toString())
                                 .setNeutralButton(R.string.copy, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                       Utils.setClipboardString(
                                             summary.input.toString(),
                                             TransactionHistoryFragment.this.getActivity());
                                       Toast.makeText(
                                             TransactionHistoryFragment.this.getActivity(),
                                             R.string.copied_to_clipboard, Toast.LENGTH_SHORT)
                                             .show();

                                       dialog.dismiss();
                                    }
                                 })
                                 .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                       dialog.dismiss();
                                    }
                                 })
                                 .show();
                        }
                        return true;
                     }
                     switch (itemId) {
                        case R.id.miShowDetails:
                           doShowDetails(record);
                           finishActionMode();
                           return true;
                        case R.id.miSetLabel:
                           setTransactionLabel(record);
                           finishActionMode();
                           break;
                        case R.id.miAddToAddressBook:
                           String defaultName = "";
                           if (_mbwManager.getSelectedAccount() instanceof ColuAccount) {
                              defaultName = ((ColuAccount) _mbwManager.getSelectedAccount()).getColuAsset().name;
                           }
                           EnterAddressLabelUtil.enterAddressLabel(getActivity(), _mbwManager.getMetadataStorage(), record.destinationAddress.get(), defaultName, addressLabelChanged);
                           break;
                        case R.id.miCancelTransaction:
                           new AlertDialog.Builder(getActivity())
                                   .setTitle(_context.getString(R.string.remove_queued_transaction_title))
                                   .setMessage(_context.getString(R.string.remove_queued_transaction))
                                   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         boolean okay = _mbwManager.getSelectedAccount().cancelQueuedTransaction(record.txid);
                                         dialog.dismiss();
                                         if (okay) {
                                            Utils.showSimpleMessageDialog(getActivity(), _context.getString(R.string.remove_queued_transaction_hint));
                                         } else {
                                            new Toaster(getActivity()).toast(_context.getString(R.string.remove_queued_transaction_error), false);
                                         }
                                      }
                                   })
                                   .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         dialog.dismiss();
                                      }
                                   })
                                   .create().show();
                           break;
                        case R.id.miDeleteUnconfirmedTransaction:
                           new AlertDialog.Builder(getActivity())
                                   .setTitle(_context.getString(R.string.delete_unconfirmed_transaction_title))
                                   .setMessage(_context.getString(R.string.warning_delete_unconfirmed_transaction))
                                   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         _mbwManager.getSelectedAccount().deleteTransaction(record.txid);
                                         dialog.dismiss();
                                      }
                                   })
                                   .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         dialog.dismiss();
                                      }
                                   })
                                   .create().show();
                           break;
                        case R.id.miRebroadcastTransaction:
                           new AlertDialog.Builder(getActivity())
                                   .setTitle(_context.getString(R.string.rebroadcast_transaction_title))
                                   .setMessage(_context.getString(R.string.description_rebroadcast_transaction))
                                   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         boolean success = BroadcastTransactionActivity.callMe(getActivity(), _mbwManager.getSelectedAccount(), record.txid);
                                         if (!success) {
                                            Utils.showSimpleMessageDialog(getActivity(), _context.getString(R.string.message_rebroadcast_failed));
                                         }
                                         dialog.dismiss();
                                      }
                                   })
                                   .setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         dialog.dismiss();
                                      }
                                   })
                                   .create().show();
                           break;
                        case R.id.miBumpFee:
                           AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                                   .setTitle(_context.getString(R.string.bump_fee_title))
                                   .setMessage(_context.getString(R.string.description_bump_fee_placeholder))
                                   .setPositiveButton(R.string.yes, null)
                                   .setNegativeButton(R.string.no, null).create();

                           final AsyncTask<Void, Void, Boolean> updateParentTask = new UpdateParentTask(record.txid, alertDialog, _context);
                           alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
                              @Override
                              public void onDismiss(DialogInterface dialog) {
                                 updateParentTask.cancel(true);
                              }
                           });

                           alertDialog.show();
                           alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                           updateParentTask.execute();
                           break;
                        case R.id.miShare:
                           new AlertDialog.Builder(getActivity())
                                   .setTitle(R.string.share_transaction_manually_title)
                                   .setMessage(R.string.share_transaction_manually_description)
                                   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         String transaction;
                                         if(_mbwManager.getSelectedAccount().getType() == WalletAccount.Type.BCHBIP44
                                             || _mbwManager.getSelectedAccount().getType()
                                             == WalletAccount.Type.BCHSINGLEADDRESS) {
                                            //TODO Module should provide full bytes of transaction.
                                            transaction = HexUtils.toHex(_mbwManager.getSelectedAccount()
                                                .getTransactionSummary(record.txid).txid.getBytes());
                                         } else {
                                            transaction = HexUtils.toHex(_mbwManager
                                                .getSelectedAccount()
                                                .getTransaction(record.txid).binary);
                                         }

                                         Intent shareIntent = new Intent(Intent.ACTION_SEND);
                                         shareIntent.setType("text/plain");
                                         shareIntent.putExtra(Intent.EXTRA_TEXT, transaction);
                                         startActivity(Intent.createChooser(shareIntent, getString(R.string.share_transaction)));
                                         dialog.dismiss();
                                      }
                                   })
                                   .setNegativeButton(R.string.no, null)
                                   .create().show();
                           break;
                     }
                     return false;
                  }

                  @Override
                  public void onDestroyActionMode(ActionMode actionMode) {
                     listView.setItemChecked(position, false);
                     currentActionMode = null;
                  }
               });
            }
         });
         return rowView;
      }
   }

   /**
    * Async task to perform fetching parent transactions of current transaction from server
    */
   @SuppressLint("StaticFieldLeak")
   private class UpdateParentTask extends AsyncTask<Void, Void, Boolean> {
      private final Sha256Hash txid;
      private final AlertDialog alertDialog;
      private final Context context;

      UpdateParentTask(Sha256Hash txid, AlertDialog alertDialog, Context context) {
         this.txid = txid;
         this.alertDialog = alertDialog;
         this.context = context;
      }

      @Override
      protected Boolean doInBackground(Void... voids) {
         if (_mbwManager.getSelectedAccount() instanceof AbstractAccount) {
            AbstractAccount selectedAccount = (AbstractAccount) _mbwManager.getSelectedAccount();
            TransactionEx transactionEx = selectedAccount.getTransaction(txid);
            Transaction transaction = TransactionEx.toTransaction(transactionEx);
            try {
               selectedAccount.fetchStoreAndValidateParentOutputs(Collections.singletonList(transaction), true);
            } catch (WapiException e) {
               _mbwManager.retainingWapiLogger.logError("Can't load parent", e);
               return false;
            }
         }
         return true;
      }

      @Override
      protected void onPostExecute(Boolean isResultOk) {
         super.onPostExecute(isResultOk);
         if (isResultOk) {
            long fee = MinerFee.PRIORITY.getFeePerKb(_mbwManager.getWalletManager(false).getLastFeeEstimations()).getLongValue();
            final UnsignedTransaction unsigned = tryCreateBumpTransaction(txid, fee);
            if (unsigned != null) {
               long txFee = unsigned.calculateFee();
               ExactBitcoinValue txFeeBitcoinValue = ExactBitcoinValue.from(txFee);
               String txFeeString = Utils.getFormattedValueWithUnit(txFeeBitcoinValue, _mbwManager.getBitcoinDenomination());
               CurrencyValue txFeeCurrencyValue = CurrencyValue.fromValue(txFeeBitcoinValue, _mbwManager.getFiatCurrency(), _mbwManager.getExchangeRateManager());
               if (!CurrencyValue.isNullOrZero(txFeeCurrencyValue)) {
                  txFeeString += " (" + Utils.getFormattedValueWithUnit(txFeeCurrencyValue, _mbwManager.getBitcoinDenomination()) + ")";
               }
               alertDialog.setMessage(context.getString(R.string.description_bump_fee, fee / 1000, txFeeString));
               alertDialog.setButton(DialogInterface.BUTTON_POSITIVE, context.getString(R.string.yes), new DialogInterface.OnClickListener() {
                  @Override
                  @SuppressWarnings("all")
                  public void onClick(DialogInterface dialog, int which) {
                     // 'unsigned' Object might become null when the dialog is displayed and not used for a long time
                     if (unsigned != null) {
                        Intent intent = SignTransactionActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), false, unsigned);
                        startActivityForResult(intent, SIGN_TRANSACTION_REQUEST_CODE);
                     } else {
                        new Toaster(getActivity()).toast("Bumping fee failed", false);
                     }
                     dialog.dismiss();
                  }
               });
               alertDialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
            } else {
               alertDialog.dismiss();
            }
         } else {
            alertDialog.dismiss();
         }
      }
   }
   /**
    * This method determins the parent's size and fee and builds a transaction that spends from its outputs but with a fee that lifts the parent and the child to high priority.
    * TODO: consider upstream chains of unconfirmed
    * TODO: consider parallel attempts to PFP
    */
   private UnsignedTransaction tryCreateBumpTransaction(Sha256Hash txid, long feePerKB) {
      WalletAccount walletAccount = _mbwManager.getSelectedAccount();
      TransactionDetails transaction = walletAccount.getTransactionDetails(txid);
      long txFee = 0;
      for(TransactionDetails.Item i : transaction.inputs) {
         txFee += i.value;
      }
      for(TransactionDetails.Item i : transaction.outputs) {
         txFee -= i.value;
      }
      if(txFee * 1000 / transaction.rawSize >= feePerKB) {
         makeText(getActivity(), "bumping not necessary", LENGTH_LONG).show();
         return null;
      }
      if (walletAccount instanceof AbstractAccount) {
         AbstractAccount account = (AbstractAccount) walletAccount;
         try {
            return account.createUnsignedCPFPTransaction(txid, feePerKB, txFee);
         } catch (InsufficientFundsException e) {
            makeText(getActivity(), getResources().getString(R.string.insufficient_funds), LENGTH_LONG).show();
         } catch (UnableToBuildTransactionException e) {
            makeText(getActivity(), getResources().getString(R.string.unable_to_build_tx), LENGTH_LONG).show();
         }
      }
      return null;
   }

   private EnterAddressLabelUtil.AddressLabelChangedHandler addressLabelChanged = new EnterAddressLabelUtil.AddressLabelChangedHandler() {
      @Override
      public void OnAddressLabelChanged(Address address, String label) {
         _mbwManager.getEventBus().post(new AddressBookChanged());
      }
   };

   private void setTransactionLabel(TransactionSummary record) {
      EnterAddressLabelUtil.enterTransactionLabel(getActivity(), record.txid, _storage, transactionLabelChanged);
   }

   private EnterAddressLabelUtil.TransactionLabelChangedHandler transactionLabelChanged = new EnterAddressLabelUtil.TransactionLabelChangedHandler() {

      @Override
      public void OnTransactionLabelChanged(Sha256Hash txid, String label) {
         _mbwManager.getEventBus().post(new TransactionLabelChanged());
      }
   };



   private void shareTransactionHistory() {
      WalletAccount account = _mbwManager.getSelectedAccount();
      MetadataStorage metaData = _mbwManager.getMetadataStorage();
      try {
         String fileName = "MyceliumExport_" + System.currentTimeMillis() + ".csv";

         List<TransactionSummary> history = account.getTransactionHistory(0, Integer.MAX_VALUE);

         File historyData = DataExport.getTxHistoryCsv(account, history, metaData,
             getActivity().getFileStreamPath(fileName));
         PackageManager packageManager = Preconditions.checkNotNull(getActivity().getPackageManager());
         PackageInfo packageInfo = packageManager.getPackageInfo(getActivity().getPackageName(), PackageManager.GET_PROVIDERS);
         for (ProviderInfo info : packageInfo.providers) {
            if (info.name.equals("android.support.v4.content.FileProvider")) {
               String authority = info.authority;
               Uri uri = FileProvider.getUriForFile(getActivity(), authority, historyData);
               Intent intent = ShareCompat.IntentBuilder.from(getActivity())
                       .setStream(uri)  // uri from FileProvider
                       .setType("text/plain")
                       .setSubject(getResources().getString(R.string.transaction_history_title))
                       .setText(getResources().getString(R.string.transaction_history_title))
                       .getIntent()
                       .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
               List<ResolveInfo> resInfoList = packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
               for (ResolveInfo resolveInfo : resInfoList) {
                  String packageName = resolveInfo.activityInfo.packageName;
                  getActivity().grantUriPermission(packageName, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
               }
               startActivity(Intent.createChooser(intent, getResources().getString(R.string.share_transaction_history)));
            }
         }
      } catch (IOException | PackageManager.NameNotFoundException e) {
         new Toaster(getActivity()).toast("Export failed. Check your logs", false);
         e.printStackTrace();
      }
   }

}
