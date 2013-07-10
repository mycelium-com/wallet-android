/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mycelium.wallet.activity;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.StringUtils;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.NetworkConnectionWatcher.ConnectionObserver;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.SimpleGestureFilter;
import com.mycelium.wallet.SimpleGestureFilter.SimpleGestureListener;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.ApiCache;
import com.mycelium.wallet.api.AsyncTask;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.util.TransactionSummaryUtils;
import com.mrd.mbwapi.util.TransactionSummaryUtils.TransactionType;

public class TransactionHistoryActivity extends Activity implements ConnectionObserver, SimpleGestureListener {

   private static final String[] EMPTY_STRING_ARRAY = new String[0];

   private Record _record;
   private AsyncTask _task;
   private TransactionHistoryAdapter _transactionHistoryAdapter;
   private Map<String, String> _invoiceMap;
   private QueryTransactionSummaryResponse _transactions;
   private ApiCache _cache;
   private SimpleGestureFilter _gestureFilter;
   private AddressBookManager _addressBook;
   private TransactionSummary _selectedTransaction;
   private MbwManager _mbwManager;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.transaction_history_activity);

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _cache = _mbwManager.getCache();
      _addressBook = _mbwManager.getAddressBookManager();

      // Get intent parameters
      _record = (Record) getIntent().getSerializableExtra("record");

      ((ListView) findViewById(R.id.lvTransactionHistory)).setOnItemClickListener(new OnItemClickListener() {

         @Override
         public void onItemClick(AdapterView<?> list, View v, int position, long id) {

            if (v.getTag() == null || !(v.getTag() instanceof TransactionSummary)) {
               return;
            }
            _selectedTransaction = (TransactionSummary) v.getTag();
            list.showContextMenuForChild(v);
         }
      });

      registerForContextMenu(findViewById(R.id.lvTransactionHistory));
   }

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.transaction_history_context_menu, menu);
      // Only show Set Label if we have a single foreign address in the
      // transaction
      if (_selectedTransaction == null || getSingleForeignAddressForTransaction(_selectedTransaction) == null) {
         menu.findItem(R.id.miAddToAddressBook).setEnabled(false);
      }
      if (getInvoiceUrl(_selectedTransaction) == null) {
         menu.findItem(R.id.miDownloadInvoice).setVisible(false);
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      if (item.getItemId() == R.id.miAddToAddressBook) {
         doSetLabel();
         return true;
      } else if (item.getItemId() == R.id.miShowDetails) {
         doShowDetails();
         return true;
      } else if (item.getItemId() == R.id.miDownloadInvoice) {
         doDownloadInvoice();
         return true;
      } else {
         return false;
      }
   }

   private String getInvoiceUrl(TransactionSummary t) {
      if (t == null || _invoiceMap == null) {
         return null;
      }
      Set<Address> addressSet = new HashSet<Address>();
      addressSet.add(_record.address);
      TransactionSummaryUtils.TransactionType type = TransactionSummaryUtils.getTransactionType(t, addressSet);
      if (type != TransactionType.SentToOthers) {
         return null;
      }
      String[] candidates = TransactionSummaryUtils.getReceiversNotMe(t, addressSet);
      if (candidates.length != 1) {
         return null;
      }
      if (!_invoiceMap.containsKey(candidates[0])) {
         return null;
      }
      String urlString = _invoiceMap.get(candidates[0]);
      return urlString;
   }

   private void doDownloadInvoice() {
      String urlString = getInvoiceUrl(_selectedTransaction);
      if (urlString == null) {
         return;
      }
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
      startActivity(intent);
      Toast.makeText(TransactionHistoryActivity.this, R.string.downloading_invoice, Toast.LENGTH_LONG).show();
   }

   private void doSetLabel() {
      if (_selectedTransaction == null) {
         return;
      }
      // Set the label of the address
      String address = getSingleForeignAddressForTransaction(_selectedTransaction);
      if (address != null) {
         Utils.showSetAddressLabelDialog(TransactionHistoryActivity.this, _addressBook, address);
      }
   }

   private void doShowDetails() {
      if (_selectedTransaction == null) {
         return;
      }
      // Open transaction details
      Intent intent = new Intent(TransactionHistoryActivity.this, TransactionDetailsActivity.class);
      ByteWriter writer = new ByteWriter(1024 * 10);
      _selectedTransaction.serialize(writer);
      intent.putExtra("transaction", writer.toBytes());
      startActivity(intent);
   }

   private String getSingleForeignAddressForTransaction(TransactionSummary tx) {
      Set<Address> addressSet = new HashSet<Address>();
      addressSet.add(_record.address);
      TransactionSummaryUtils.TransactionType type = TransactionSummaryUtils.getTransactionType(tx, addressSet);
      if (type == TransactionType.SentToOthers) {
         String[] candidates = TransactionSummaryUtils.getReceiversNotMe(tx, addressSet);
         if (candidates.length != 1) {
            return null;
         }
         return candidates[0];
      } else if (type == TransactionType.ReceivedFromOthers) {
         String[] candidates = TransactionSummaryUtils.getSenders(tx);
         if (candidates.length != 1) {
            return null;
         }
         return candidates[0];
      }
      return null;
   }

   @Override
   protected void onDestroy() {
      cancelEverything();
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      if (Utils.isConnected(this)) {
         refresh();
      } else {
         Utils.toastConnectionError(this);
      }
      _gestureFilter = new SimpleGestureFilter(this, this);
      // Register for network going up/down callbacks
      MbwManager.getInstance(this.getApplication()).getNetworkConnectionWatcher().addObserver(this);
      animateSwipe();
      super.onResume();
   }

   private void animateSwipe() {
      // Visible swipe disabled for now
      // long speed = 250;
      // long delay = 100;
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow1), delay * 0, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow2), delay * 1, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow3), delay * 2, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow4), delay * 3, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow5), delay * 4, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow6), delay * 5, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow7), delay * 6, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvRightArrow8), delay * 7, speed,
      // 0);
   }

   @Override
   protected void onPause() {
      // Unregister for network going up/down callbacks
      MbwManager.getInstance(this.getApplication()).getNetworkConnectionWatcher().removeObserver(this);
      super.onPause();
   }

   private void cancelEverything() {
      if (_task != null) {
         _task.cancel();
      }
   }

   private void refresh() {
      if (_task != null) {
         return;
      }
      findViewById(R.id.pbHistory).setVisibility(View.VISIBLE);
      _transactions = _cache.getTransactionSummaryList(_record.address);
      updateTransactionHistory();
      AndroidAsyncApi api = MbwManager.getInstance(getApplication()).getAsyncApi();
      _task = api.getTransactionSummary(_record.address, new QueryTransactionSummaryHandler());
   }

   class QueryTransactionSummaryHandler implements AbstractCallbackHandler<QueryTransactionSummaryResponse> {

      @Override
      public void handleCallback(QueryTransactionSummaryResponse response, ApiError exception) {
         findViewById(R.id.pbHistory).setVisibility(View.INVISIBLE);
         if (exception != null) {
            Utils.toastConnectionError(TransactionHistoryActivity.this);
         } else {
            _transactions = response;
            updateTransactionHistory();
            _task = fetchInvoices(_transactions);
         }
      }

   }

   private AsyncTask fetchInvoices(QueryTransactionSummaryResponse response) {
      Set<Address> addressSet = new HashSet<Address>();
      addressSet.add(_record.address);
      List<String> addresses = new LinkedList<String>();
      for (TransactionSummary t : response.transactions) {
         TransactionSummaryUtils.TransactionType type = TransactionSummaryUtils.getTransactionType(t, addressSet);
         if (type == TransactionType.SentToOthers) {
            String[] candidates = TransactionSummaryUtils.getReceiversNotMe(t, addressSet);
            if (candidates.length == 1) {
               addresses.add(candidates[0]);
            }
         }
      }
      AndroidAsyncApi api = MbwManager.getInstance(getApplication()).getAsyncApi();
      return api.lookupInvoices(addresses, new LookupInvoicesHandler());
   }

   class LookupInvoicesHandler implements AbstractCallbackHandler<Map<String, String>> {

      @Override
      public void handleCallback(Map<String, String> response, ApiError exception) {
         if (exception != null) {
            // Ignore
            _task = null;
         } else {
            _invoiceMap = response;
            if (_transactionHistoryAdapter != null) {
               _transactionHistoryAdapter.setInvoiceMap(response);
            }
         }
         _task = null;
      }
   }

   private void updateTransactionHistory() {
      if (_transactions == null || _transactions.transactions.size() == 0) {
         findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
         findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
      } else {
         Collections.sort(_transactions.transactions);
         findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         findViewById(R.id.lvTransactionHistory).setVisibility(View.VISIBLE);
         _transactionHistoryAdapter = new TransactionHistoryAdapter(this, _transactions);
         ((ListView) findViewById(R.id.lvTransactionHistory)).setAdapter(_transactionHistoryAdapter);
      }
   }

   class TransactionHistoryAdapter extends ArrayAdapter<TransactionSummary> {
      private Context _context;
      private Date _midnight;
      private DateFormat _hourFormat;
      private DateFormat _dayFormat;
      private Set<Address> _addressSet;
      private int _chainHeight;
      private Map<String, String> _invoiceMap;

      public void setInvoiceMap(Map<String, String> invoiceMap) {
         _invoiceMap = invoiceMap;
         if (_invoiceMap.size() > 0) {
            this.notifyDataSetChanged();
         }
      }

      public TransactionHistoryAdapter(Context context, QueryTransactionSummaryResponse transactions) {
         super(context, R.layout.transaction_row, transactions.transactions);
         _context = context;
         _chainHeight = transactions.chainHeight;
         _invoiceMap = new HashMap<String, String>();
         // Get the time at last midnight
         Calendar midnight = Calendar.getInstance();
         midnight.set(midnight.get(Calendar.YEAR), midnight.get(Calendar.MONTH), midnight.get(Calendar.DAY_OF_MONTH),
               0, 0, 0);
         _midnight = midnight.getTime();
         // Create date formats for hourly and day format
         Locale locale = getResources().getConfiguration().locale;
         _hourFormat = DateFormat.getDateInstance(DateFormat.SHORT, locale);
         _dayFormat = DateFormat.getTimeInstance(DateFormat.SHORT, locale);
         _addressSet = new HashSet<Address>();
         _addressSet.add(_record.address);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         // Only inflate a new view if we are not reusing an old one
         View rowView = convertView;
         if (rowView == null) {
            LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            rowView = inflater.inflate(R.layout.transaction_row, parent, false);
         }

         TransactionSummary record = getItem(position);
         TransactionSummaryUtils.TransactionType type = TransactionSummaryUtils.getTransactionType(record, _addressSet);

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
         DateFormat dateFormat = date.before(_midnight) ? _hourFormat : _dayFormat;
         TextView tvDate = (TextView) rowView.findViewById(R.id.tvDate);
         tvDate.setText(dateFormat.format(date));

         // Set value
         TextView tvAmount = (TextView) rowView.findViewById(R.id.tvAmount);
         tvAmount.setText(_mbwManager.getBtcValueString(value));
         tvAmount.setTextColor(color);

         // Determine list of addresses
         String[] addresses;
         if (type == TransactionType.SentToOthers) {
            addresses = TransactionSummaryUtils.getReceiversNotMe(record, _addressSet);
         } else if (type == TransactionType.ReceivedFromOthers) {
            addresses = TransactionSummaryUtils.getSenders(record);
         } else {
            addresses = EMPTY_STRING_ARRAY;
         }

         // Show/Hide Invoice if we have one
         if (value < 0 && addresses.length == 1 && _invoiceMap.containsKey(addresses[0])) {
            rowView.findViewById(R.id.tvInvoice).setVisibility(View.VISIBLE);
         } else {
            rowView.findViewById(R.id.tvInvoice).setVisibility(View.INVISIBLE);
         }

         // Replace addresses with known names from the address book
         fillInAddressBookNames(addresses);
         TextView tvAddress = (TextView) rowView.findViewById(R.id.tvAddress);
         tvAddress.setText(StringUtils.join(addresses, " "));

         // Set confirmations
         int confirmations = record.calculateConfirmatons(_chainHeight);
         TextView tvConfirmations = (TextView) rowView.findViewById(R.id.tvConfirmations);
         tvConfirmations.setText(_context.getResources().getString(R.string.confirmations, confirmations));

         rowView.setTag(record);

         return rowView;
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

   @Override
   public void OnNetworkConnected() {
      if (isFinishing()) {
         return;
      }
      new Handler().post(new Runnable() {
         @Override
         public void run() {
            refresh();
         }
      });
   }

   @Override
   public void OnNetworkDisconnected() {

   }

   @Override
   public void onBackPressed() {
      super.onBackPressed();
      this.overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
   }

   @Override
   public void onSwipe(int direction) {

      if (direction == SimpleGestureFilter.SWIPE_RIGHT) {
         finish();
         this.overridePendingTransition(R.anim.left_to_right_enter, R.anim.left_to_right_exit);
      }
   }

   @Override
   public void onDoubleTap() {
      // Do nothing
   }

   @Override
   public boolean dispatchTouchEvent(MotionEvent me) {
      this._gestureFilter.onTouchEvent(me);
      return super.dispatchTouchEvent(me);
   }

}