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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.*;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.commonsware.cwac.endless.EndlessAdapter;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.TransactionConfirmationsDisplay;
import com.mycelium.wallet.event.AddressBookChanged;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.event.SelectedCurrencyChanged;
import com.mycelium.wallet.event.SyncStopped;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.model.TransactionSummary;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.squareup.otto.Subscribe;


import java.text.DateFormat;
import java.util.*;

public class TransactionHistoryFragment extends Fragment {

   private MbwManager _mbwManager;
   private MetadataStorage _storage;
   private View _root;
   private ActionMode currentActionMode;
   private volatile Map<Address, String> _addressBook;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = inflater.inflate(R.layout.main_transaction_history_view, container, false);

      _root.findViewById(R.id.btRescan).setOnClickListener(new View.OnClickListener() {
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
   public void onAttach(Activity activity) {
      super.onAttach(activity);
      _mbwManager = MbwManager.getInstance(activity);
      _storage = _mbwManager.getMetadataStorage();


   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      if (_mbwManager.getWalletManager(false).getState() == WalletManager.State.READY) {
         updateTransactionHistory();
      }
      super.onResume();
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Subscribe
   public void syncStopped(SyncStopped event) {
      updateTransactionHistory();
   }

   @Subscribe
   public void exchangeRateChanged(ExchangeRatesRefreshed event) {
      ((ListView) _root.findViewById(R.id.lvTransactionHistory)).invalidateViews();
   }

   @Subscribe
   public void fiatCurrencyChanged(SelectedCurrencyChanged event) {
      ((ListView) _root.findViewById(R.id.lvTransactionHistory)).invalidateViews();
   }

   @Subscribe
   public void addressBookEntryChanged(AddressBookChanged event) {
      cacheAddressBook();
      ((ListView) _root.findViewById(R.id.lvTransactionHistory)).invalidateViews();
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
         return;
      }
      List<TransactionSummary> history = account.getTransactionHistory(0, 20);
      if (history.isEmpty()) {
         _root.findViewById(R.id.llNoRecords).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
      } else {
         _root.findViewById(R.id.llNoRecords).setVisibility(View.GONE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.VISIBLE);
         Wrapper wrapper = new Wrapper(getActivity(), history);
         ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setAdapter(wrapper);
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

   private class TransactionHistoryAdapter extends ArrayAdapter<TransactionSummary> {
      private Context _context;
      private Date _midnight;
      private DateFormat _dayFormat;
      private DateFormat _hourFormat;

      public TransactionHistoryAdapter(Context context, List<TransactionSummary> transactions) {
         super(context, R.layout.transaction_row, transactions);
         _context = context;
         // Get the time at last midnight
         Calendar midnight = Calendar.getInstance();
         midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH),
               0, 0, 0);
         _midnight = midnight.getTime();
         // Create date formats for hourly and day format
         Locale locale = getResources().getConfiguration().locale;
         _dayFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
         _hourFormat = android.text.format.DateFormat.getTimeFormat(_context);

      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         // Only inflate a new view if we are not reusing an old one
         View rowView = convertView;
         if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = Preconditions.checkNotNull(inflater.inflate(R.layout.transaction_row, parent, false));
         }

         // Make sure we are still added
         if (!isAdded()) {
            // We have observed that the fragment can be disconnected at this
            // point
            return rowView;
         }

         final TransactionSummary record = getItem(position);
         final ActionBarActivity actionBarActivity = (ActionBarActivity) getActivity();

         rowView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(final View view) {
               currentActionMode = actionBarActivity.startSupportActionMode(new ActionMode.Callback() {
                  @Override
                  public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                     actionMode.getMenuInflater().inflate(R.menu.transaction_history_context_menu, menu);
                     return true;
                  }

                  @SuppressWarnings("deprecation")
                  @Override
                  public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                     //we only allow address book entries for outgoing transactions
                     if (record.destinationAddress.isPresent()){
                        Preconditions.checkNotNull(menu.findItem(R.id.miAddToAddressBook)).setVisible(true);
                     }else {
                        Preconditions.checkNotNull(menu.findItem(R.id.miAddToAddressBook)).setVisible(false);
                     }
                     currentActionMode = actionMode;
                     view.setBackgroundDrawable(getResources().getDrawable(R.color.selectedrecord));
                     return true;
                  }

                  @Override
                  public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                     final int itemId = menuItem.getItemId();
                     if (itemId == R.id.miShowDetails) {
                        doShowDetails(record);
                        finishActionMode();
                        return true;
                     } else if (itemId == R.id.miSetLabel) {
                        setTransactionLabel(record);
                        finishActionMode();
                     } else if (itemId == R.id.miAddToAddressBook) {
                        EnterAddressLabelUtil.enterAddressLabel(getActivity(), _mbwManager.getMetadataStorage(), record.destinationAddress.get(), "", addressLabelChanged);
                     }
                     return false;
                  }

                  @SuppressWarnings("deprecation")
                  @Override
                  public void onDestroyActionMode(ActionMode actionMode) {
                     view.setBackgroundDrawable(null);
                     currentActionMode = null;
                  }
               });
            }
         });

         // Determine Color
         int color;
         if (record.value < 0) {
            color = getResources().getColor(R.color.red);
         } else {
            color = getResources().getColor(R.color.green);
         }

         // Set Date
         Date date = new Date(record.time * 1000L);
         DateFormat dateFormat = date.before(_midnight) ? _dayFormat : _hourFormat;
         TextView tvDate = (TextView) rowView.findViewById(R.id.tvDate);
         tvDate.setText(dateFormat.format(date));

         // Set value
         TextView tvAmount = (TextView) rowView.findViewById(R.id.tvAmount);
         tvAmount.setText(_mbwManager.getBtcValueString(record.value));
         tvAmount.setTextColor(color);

         // Set fiat value
         TextView tvFiat = (TextView) rowView.findViewById(R.id.tvFiatAmount);
         Double rate = _mbwManager.getCurrencySwitcher().getExchangeRatePrice();
         if (!_mbwManager.hasFiatCurrency() || rate == null) {
            tvFiat.setVisibility(View.GONE);
         } else {
            tvFiat.setVisibility(View.VISIBLE);
            String currency = _mbwManager.getFiatCurrency();
            String converted = Utils.getFiatValueAsString(record.value, rate);
            tvFiat.setText(getResources().getString(R.string.approximate_fiat_value, currency, converted));
            tvFiat.setTextColor(color);
         }



         // Show destination address and address label, if this address is in our address book
         TextView tvAddressLabel = (TextView)rowView.findViewById(R.id.tvAddressLabel);
         TextView tvDestAddress = (TextView)rowView.findViewById(R.id.tvDestAddress);

         if (record.destinationAddress.isPresent() && _addressBook.containsKey(record.destinationAddress.get())) {
            tvDestAddress.setText(record.destinationAddress.get().getShortAddress());
            tvAddressLabel.setText(String.format(_context.getString(R.string.transaction_to_address_prefix), _addressBook.get(record.destinationAddress.get())));
            tvDestAddress.setVisibility(View.VISIBLE);
            tvAddressLabel.setVisibility(View.VISIBLE);
         }else {
            tvDestAddress.setVisibility(View.GONE);
            tvAddressLabel.setVisibility(View.GONE);
         }

         // Show confirmations indicator
         int confirmations = record.confirmations;
         TransactionConfirmationsDisplay tcdConfirmations = (TransactionConfirmationsDisplay) rowView.findViewById(R.id.tcdConfirmations);
         if (record.isOutgoing) {
            // Outgoing, not broadcasted
            tcdConfirmations.setNeedsBroadcast();
         } else {
            tcdConfirmations.setConfirmations(confirmations);
         }

         // Show label or confirmations
         TextView tvLabel = (TextView) rowView.findViewById(R.id.tvTransactionLabel);
         String label = _storage.getLabelByTransaction(record.txid);
         if (label.length() == 0) {
            // if we have no txLabel show the confirmation state instead - to keep they layout ballanced
            String confirmationsText;
            if (record.isOutgoing) {
               confirmationsText = _context.getResources().getString(R.string.transaction_not_broadcasted_info);
            } else {
               if (confirmations > 6){
                  confirmationsText = _context.getResources().getString(R.string.confirmed);
               } else {
                  confirmationsText = _context.getResources().getString(R.string.confirmations, confirmations);
               }
            }
            tvLabel.setText(confirmationsText);
         } else {
            tvLabel.setText(label);
         }


         rowView.setTag(record);
         return rowView;
      }
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
      private int lastOffset;
      private int chunkSize;

      private Wrapper(Context context, List<TransactionSummary> transactions) {
         super(new TransactionHistoryAdapter(context, transactions));
         _toAdd = new ArrayList<TransactionSummary>();
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
         synchronized (_toAdd) {
            lastOffset += chunkSize;
            _toAdd = acc.getTransactionHistory(lastOffset, chunkSize);
         }
         return _toAdd.size() == chunkSize;
      }

      @Override
      protected void appendCachedData() {
         synchronized (_toAdd) {
            TransactionHistoryAdapter a = (TransactionHistoryAdapter) getWrappedAdapter();
            for (TransactionSummary item : _toAdd) {
               a.add(item);
            }
            _toAdd.clear();
         }
      }
   }
}
