/*
 * Copyright 2013 Megion Research and Development GmbH
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

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.util.TransactionSummaryUtils;
import com.mrd.mbwapi.util.TransactionType;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler;
import com.mycelium.wallet.api.ApiCache;
import com.mycelium.wallet.event.TransactionHistoryReady;
import com.squareup.otto.Subscribe;

public class TransactionHistoryFragment extends Fragment {

   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private View _root;
   private ApiCache _cache;
   private AddressBookManager _addressBook;
   private ActionMode currentActionMode;
   private AddressLabelChangedHandler addressLabelChanged = new AddressLabelChangedHandler() {

      @Override
      public void OnAddressLabelChanged(String address, String label) {
         updateTransactionHistory();
      }
   };

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = inflater.inflate(R.layout.main_transaction_history_view, container, false);
      return _root;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      setHasOptionsMenu(true);
      super.onCreate(savedInstanceState);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(activity);
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();
      _cache = _mbwManager.getCache();
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      // Update from cache
      updateTransactionHistory();
      super.onResume();
   }

   @Override
   public void onPause() {
      _mbwManager.getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   public void onDetach() {
      super.onDetach();
   }

   @Subscribe
   public void transactionsUpdated(TransactionHistoryReady transactionHistoryReady) {
      updateTransactionHistory();
   }

   private void doSetLabel(TransactionSummary selected) {
      if (selected == null) {
         return;
      }
      // Set the label of the address
      String address = getSingleForeignAddressForTransaction(selected);
      if (address != null) {
         EnterAddressLabelUtil.enterAddressLabel(getActivity(), _addressBook, address, "", addressLabelChanged);
      }
   }

   private void doShowDetails(TransactionSummary selected) {
      if (selected == null) {
         return;
      }
      // Open transaction details
      Intent intent = new Intent(getActivity(), TransactionDetailsActivity.class);
      ByteWriter writer = new ByteWriter(1024 * 10);
      selected.serialize(writer);
      intent.putExtra("transaction", writer.toBytes());
      startActivity(intent);
   }

   private String getSingleForeignAddressForTransaction(TransactionSummary tx) {
      if (tx == null) {
         return null;
      }
      Wallet wallet = getWallet();
      Set<Address> addressSet = wallet.getAddressSet();
      TransactionType type = TransactionSummaryUtils.getTransactionType(tx, addressSet);
      return type.singleForeignAddress(tx, addressSet);
   }

   private Wallet getWallet() {
      return _recordManager.getWallet(_mbwManager.getWalletMode());
   }

   @SuppressWarnings("unchecked")
   private void updateTransactionHistory() {
      if (!isAdded()) {
         return;
      }
      Set<Address> addressSet = getWallet().getAddressSet();
      new AsyncTransactionHistoryUpdate().execute(addressSet);
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

   private void fillInAddressBookNames(String[] addresses) {
      for (int i = 0; i < addresses.length; i++) {
         String name = _addressBook.getNameByAddress(addresses[i]);
         if (name.length() != 0) {
            addresses[i] = name;
         }
      }
   }

   private class AsyncTransactionHistoryUpdate extends AsyncTask<Set<Address>, Void, QueryTransactionSummaryResponse> {

      @Override
      protected QueryTransactionSummaryResponse doInBackground(Set<Address>... arg0) {
         Set<Address> addressSet = getWallet().getAddressSet();
         QueryTransactionSummaryResponse result = _cache.getTransactionSummaryList(addressSet);
         if (result != null) {
            Collections.sort(result.transactions);
         }
         return result;
      }

      @Override
      protected void onPostExecute(QueryTransactionSummaryResponse result) {
         if (!isAdded()) {
            return;
         }
         if (result == null || result.transactions.size() == 0) {
            _root.findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
            _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
         } else {
            _root.findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
            _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.VISIBLE);
            TransactionHistoryAdapter _transactionHistoryAdapter = new TransactionHistoryAdapter(getActivity(), result);
            ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setAdapter(_transactionHistoryAdapter);
         }
         super.onPostExecute(result);
      }
   }

   private class TransactionHistoryAdapter extends ArrayAdapter<TransactionSummary> {
      private Context _context;
      private Date _midnight;
      private DateFormat _dayFormat;
      private DateFormat _hourFormat;
      private Set<Address> _addressSet;
      private int _chainHeight;

      public TransactionHistoryAdapter(Context context, QueryTransactionSummaryResponse transactions) {
         super(context, R.layout.transaction_row, transactions.transactions);
         _context = context;
         _chainHeight = transactions.chainHeight;
         // Get the time at last midnight
         Calendar midnight = Calendar.getInstance();
         midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH),
               0, 0, 0);
         _midnight = midnight.getTime();
         // Create date formats for hourly and day format
         Locale locale = getResources().getConfiguration().locale;
         _dayFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
         _hourFormat = android.text.format.DateFormat.getTimeFormat(_context);
         Wallet wallet = getWallet();
         _addressSet = wallet.getAddressSet();
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
                     String address = getSingleForeignAddressForTransaction(record);
                     Preconditions.checkNotNull(menu.findItem(R.id.miAddToAddressBook)).setVisible(address != null);
                     currentActionMode = actionMode;
                     view.setBackgroundDrawable(getResources().getDrawable(R.color.selectedrecord));
                     return true;
                  }

                  @Override
                  public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                     final int itemId = menuItem.getItemId();
                     if (itemId == R.id.miAddToAddressBook) {
                        doSetLabel(record);
                        finishActionMode();
                        return true;
                     } else if (itemId == R.id.miShowDetails) {
                        doShowDetails(record);
                        finishActionMode();
                        return true;
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

         TransactionType type = TransactionSummaryUtils.getTransactionType(record, _addressSet);

         // Determine Value
         long value = TransactionSummaryUtils.calculateBalanceChange(record, _addressSet);

         // Determine Color
         int color;
         if (value < 0) {
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
         tvAmount.setText(_mbwManager.getBtcValueString(value));
         tvAmount.setTextColor(color);

         // Determine list of addresses
         final String[] addresses = type.relevantAddresses(record, _addressSet);

         // Replace addresses with known names from the address book
         fillInAddressBookNames(addresses);
         TextView tvAddress = (TextView) rowView.findViewById(R.id.tvAddress);
         tvAddress.setText(Joiner.on(" ").join(addresses));

         // Set confirmations
         int confirmations = record.calculateConfirmatons(_chainHeight);
         TextView tvConfirmations = (TextView) rowView.findViewById(R.id.tvConfirmations);
         tvConfirmations.setText(_context.getResources().getString(R.string.confirmations, confirmations));

         rowView.setTag(record);

         return rowView;
      }

   }

}
