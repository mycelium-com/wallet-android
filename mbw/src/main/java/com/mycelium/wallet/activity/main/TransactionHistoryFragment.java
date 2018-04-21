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

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.commonsware.cwac.endless.EndlessAdapter;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.StandardTransactionBuilder.InsufficientFundsException;
import com.mrd.bitlib.StandardTransactionBuilder.UnableToBuildTransactionException;
import com.mrd.bitlib.StandardTransactionBuilder.UnsignedTransaction;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.spvmodule.providers.TransactionContract;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.DataExport;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.WalletApplication;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.activity.main.adapter.TransactionArrayAdapter;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.send.BroadcastTransactionActivity;
import com.mycelium.wallet.activity.send.SignTransactionActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.coinapult.CoinapultTransactionSummary;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.TransactionDetails;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.ConfirmationRiskProfileLocal;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.single.SingleAddressBCHAccount;
import com.squareup.otto.Subscribe;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
   private volatile Map<Address, String> _addressBook;
   @BindView(R.id.no_transaction_message)
   TextView noTransactionMessage;

   @BindView(R.id.btRescan)
   View btnReload;

   private Wrapper wrapper;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = inflater.inflate(R.layout.main_transaction_history_view, container, false);
      ButterKnife.bind(this, _root);
      btnReload.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            _mbwManager.getSelectedAccount().dropCachedData();
            _mbwManager.getWalletManager(false).startSynchronization();
         }
      });
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
      super.onCreate(savedInstanceState);

      // cache the addressbook for faster lookup
      cacheAddressBook();
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
      updateTransactionHistory();
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

  private List<TransactionSummary> getTransactions() {
    List<TransactionSummary> transactionSummaryList = new ArrayList<>();
    WalletAccount account = _mbwManager.getSelectedAccount();
    FragmentActivity context = getActivity();
    Uri uri = TransactionContract.TransactionSummary.CONTENT_URI(
        WalletApplication.getSpvModuleName(account.getType()));
    String selection = null;
    String[] selectionArgs = null;
    if (account instanceof Bip44BCHAccount) {
      selection = TransactionContract.TransactionSummary.SELECTION_ACCOUNT_INDEX;
      int accountIndex = ((Bip44BCHAccount) _mbwManager.getSelectedAccount()).getAccountIndex();
      selectionArgs = new String[]{Integer.toString(accountIndex)};
    }

    if (account instanceof SingleAddressBCHAccount) {
      selection = TransactionContract.TransactionSummary.SELECTION_SINGLE_ADDRESS_ACCOUNT_GUID;
      selectionArgs = new String[]{account.getId().toString()};
    }

    Cursor cursor = null;
    ContentResolver contentResolver = context.getContentResolver();
    try {
      cursor = contentResolver.query(uri, null, selection, selectionArgs, null);
      if (cursor != null) {
        while (cursor.moveToNext()) {
          TransactionSummary transactionSummary = from(cursor);
          transactionSummaryList.add(transactionSummary);
        }
      }
    } finally {
      if (cursor != null) {
        cursor.close();
      }
    }
    return transactionSummaryList;
  }

  private TransactionSummary from(Cursor cursor) {
    String rawTxId = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary._ID));
    Sha256Hash txId = Sha256Hash.fromString(rawTxId);
    String rawValue = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.VALUE));
    CurrencyValue value = ExactCurrencyValue.from(new BigDecimal(rawValue), "BCH");
    int rawIsIncoming = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_INCOMING));
    boolean isIncoming = rawIsIncoming == 1;
    long time = cursor.getLong(cursor.getColumnIndex(TransactionContract.TransactionSummary.TIME));
    int height = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.HEIGHT));
    int confirmations = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATIONS));
    int rawIsQueuedOutgoing = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.IS_QUEUED_OUTGOING));
    boolean isQueuedOutgoing = rawIsQueuedOutgoing == 1;

    ConfirmationRiskProfileLocal confirmationRiskProfile = null;
    int unconfirmedChainLength = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH));
    if (unconfirmedChainLength > -1) {
      boolean hasRbfRisk = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1;
      boolean isDoubleSpend = cursor.getInt(cursor.getColumnIndex(TransactionContract.TransactionSummary.CONFIRMATION_RISK_PROFILE_LENGTH)) == 1;
      confirmationRiskProfile = new ConfirmationRiskProfileLocal(unconfirmedChainLength, hasRbfRisk, isDoubleSpend);
    }

    String rawDestinationAddress = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.DESTINATION_ADDRESS));
    Optional<Address> destinationAddress = Optional.absent();
    if (!TextUtils.isEmpty(rawDestinationAddress)) {
      destinationAddress = Optional.of(Address.fromString(rawDestinationAddress));
    }
    List<Address> toAddresses = new ArrayList<>();
    String rawToAddresses = cursor.getString(cursor.getColumnIndex(TransactionContract.TransactionSummary.TO_ADDRESSES));
    if (!TextUtils.isEmpty(rawToAddresses)) {
      String[] addresses = rawToAddresses.split(",");
      for (String addr : addresses) {
        toAddresses.add(Address.fromString(addr));
      }
    }
    return new TransactionSummary(txId, value, isIncoming, time, height, confirmations, isQueuedOutgoing,
        confirmationRiskProfile, destinationAddress, toAddresses);
  }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      updateTransactionHistory();
   }

   @Subscribe
   public void exchangeRateChanged(ExchangeRatesRefreshed event) {
      refreshList();
   }

   private void refreshList() {
      ((ListView) _root.findViewById(R.id.lvTransactionHistory)).invalidateViews();
   }

   @Subscribe
   public void fiatCurrencyChanged(SelectedCurrencyChanged event) {
      refreshList();
   }

   @Subscribe
   public void addressBookEntryChanged(AddressBookChanged event) {
      cacheAddressBook();
      refreshList();
   }

   private void cacheAddressBook() {
      _addressBook = _mbwManager.getMetadataStorage().getAllAddressLabels();
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

   @SuppressWarnings("unchecked")
   private void updateTransactionHistory() {
      if (!isAdded()) {
         return;
      }
      WalletAccount account = _mbwManager.getSelectedAccount();
      if (account.isArchived()) {
         _root.findViewById(R.id.llNoRecords).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
         return;
      }
     List<TransactionSummary> history;
      if (account.getClass() == Bip44BCHAccount.class
          || account.getClass() == SingleAddressBCHAccount.class) {
         history = getTransactions();
      } else  {
        history = account.getTransactionHistory(0, 20);
      }
      Collections.sort(history);
      Collections.reverse(history);
      if (history.isEmpty()) {
         _root.findViewById(R.id.llNoRecords).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
      } else {
         _root.findViewById(R.id.llNoRecords).setVisibility(View.GONE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.VISIBLE);
         wrapper = new Wrapper(getActivity(), history);
         ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setAdapter(wrapper);
         refreshList();
      }
      getActivity().invalidateOptionsMenu();
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
      if(wrapper != null && wrapper.getCount() > 0) {
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
         super(context, transactions, TransactionHistoryFragment.this, _addressBook, false);
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
                     checkNotNull(menu.findItem(R.id.miAddToAddressBook)).setVisible(record.hasAddressBook());
                     checkNotNull(menu.findItem(R.id.miCancelTransaction)).setVisible(record.canCancel());
                     checkNotNull(menu.findItem(R.id.miShowDetails)).setVisible(record.hasDetails());
                     checkNotNull(menu.findItem(R.id.miShowCoinapultDebug)).setVisible(record.canCoinapult());
                     checkNotNull(menu.findItem(R.id.miRebroadcastTransaction)).setVisible((record.confirmations == 0) && !record.canCoinapult());
                     checkNotNull(menu.findItem(R.id.miShare)).setVisible(!record.canCoinapult());
                     checkNotNull(menu.findItem(R.id.miBumpFee)).setVisible((record.confirmations == 0) && !record.canCoinapult());
                     checkNotNull(menu.findItem(R.id.miDeleteUnconfirmedTransaction)).setVisible(record.confirmations == 0);
                     currentActionMode = actionMode;
                     ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setItemChecked(position, true);
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
                                         updateTransactionHistory();
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
                                         updateTransactionHistory();
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
                           long fee = MinerFee.PRIORITY.getFeePerKb(_mbwManager.getWalletManager(false).getLastFeeEstimations()).getLongValue();
                           final UnsignedTransaction unsigned = tryCreateBumpTransaction(record.txid, fee);
                           if(unsigned != null) {
                              long txFee = unsigned.calculateFee();
                              ExactBitcoinValue txFeeBitcoinValue = ExactBitcoinValue.from(txFee);
                              String txFeeString = Utils.getFormattedValueWithUnit(txFeeBitcoinValue, _mbwManager.getBitcoinDenomination());
                              CurrencyValue txFeeCurrencyValue = CurrencyValue.fromValue(txFeeBitcoinValue, _mbwManager.getFiatCurrency(), _mbwManager.getExchangeRateManager());
                              if(!CurrencyValue.isNullOrZero(txFeeCurrencyValue)) {
                                 txFeeString += " (" + Utils.getFormattedValueWithUnit(txFeeCurrencyValue, _mbwManager.getBitcoinDenomination()) + ")";
                              }
                              new AlertDialog.Builder(getActivity())
                                      .setTitle(_context.getString(R.string.bump_fee_title))
                                      .setMessage(_context.getString(R.string.description_bump_fee, fee / 1000, txFeeString))
                                      .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                         @Override
                                         public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = SignTransactionActivity.getIntent(getActivity(), _mbwManager.getSelectedAccount().getId(), false, unsigned);
                                            startActivityForResult(intent, SIGN_TRANSACTION_REQUEST_CODE);
                                            dialog.dismiss();
                                         }
                                      })
                                      .setNegativeButton(R.string.no, null)
                                      .create().show();
                           }
                           break;
                        case R.id.miShare:
                           new AlertDialog.Builder(getActivity())
                                   .setTitle(R.string.share_transaction_manually_title)
                                   .setMessage(R.string.share_transaction_manually_description)
                                   .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                      @Override
                                      public void onClick(DialogInterface dialog, int which) {
                                         String transaction = HexUtils.toHex(_mbwManager.getSelectedAccount().getTransaction(record.txid).binary);
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
                     ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setItemChecked(position, false);
                     currentActionMode = null;
                  }
               });
            }
         });
         return rowView;
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
         updateTransactionHistory();
      }
   };

   private void setTransactionLabel(TransactionSummary record) {
      EnterAddressLabelUtil.enterTransactionLabel(getActivity(), record.txid, _storage, transactionLabelChanged);
   }

   private EnterAddressLabelUtil.TransactionLabelChangedHandler transactionLabelChanged = new EnterAddressLabelUtil.TransactionLabelChangedHandler() {

      @Override
      public void OnTransactionLabelChanged(Sha256Hash txid, String label) {
         updateTransactionHistory();
      }
   };

   private class Wrapper extends EndlessAdapter {
      private List<TransactionSummary> _toAdd;
      private final Object _toAddLock = new Object();
      private int lastOffset;
      private int chunkSize;

      private Wrapper(Context context, List<TransactionSummary> transactions) {
         super(new TransactionHistoryAdapter(context, transactions));
         _toAdd = new ArrayList<>();
         lastOffset = 0;
         chunkSize = 20;
      }

      @Override
      protected View getPendingView(ViewGroup parent) {
         //this is an empty view, getting more transaction details is fast at the moment
         return LayoutInflater.from(parent.getContext()).inflate(R.layout.transaction_history_fetching, null);
      }

      @Override
      protected boolean cacheInBackground() {
         WalletAccount acc = _mbwManager.getSelectedAccount();
         synchronized (_toAddLock) {
            lastOffset += chunkSize;
            _toAdd = acc.getTransactionHistory(lastOffset, chunkSize);
         }
         return _toAdd.size() == chunkSize;
      }

      @Override
      protected void appendCachedData() {
         synchronized (_toAddLock) {
            TransactionHistoryAdapter a = (TransactionHistoryAdapter) getWrappedAdapter();
            a.addAll(_toAdd);
            _toAdd.clear();
         }
      }
   }

   private void shareTransactionHistory() {
      WalletAccount account = _mbwManager.getSelectedAccount();
      MetadataStorage metaData = _mbwManager.getMetadataStorage();
      try {
         String fileName = "MyceliumExport_" + System.currentTimeMillis() + ".csv";
         File historyData = DataExport.getTxHistoryCsv(account, metaData, getActivity().getFileStreamPath(fileName));
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
