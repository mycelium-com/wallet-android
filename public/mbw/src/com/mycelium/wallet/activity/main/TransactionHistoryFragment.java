package com.mycelium.wallet.activity.main;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
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
import android.support.v4.app.Fragment;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Joiner;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.util.TransactionSummaryUtils;
import com.mrd.mbwapi.util.TransactionType;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.api.AbstractCallbackHandler;
import com.mycelium.wallet.api.AndroidAsyncApi;
import com.mycelium.wallet.api.ApiCache;
import com.mycelium.wallet.api.AsyncTask;

public class TransactionHistoryFragment extends Fragment {

   public interface TransactionHistoryFragmentContainer {
      public Wallet getWallet();

      public MbwManager getMbwManager();
   }

   private TransactionHistoryFragmentContainer _container;
   private MbwManager _mbwManager;
   private View _root;
   private AsyncTask _task;
   private TransactionHistoryAdapter _transactionHistoryAdapter;
   private Map<String, String> _invoiceMap;
   private QueryTransactionSummaryResponse _transactions;
   private ApiCache _cache;
   private AddressBookManager _addressBook;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = (ViewGroup) inflater.inflate(R.layout.main_transaction_history_view, container, false);
      return _root;
   }

   @Override
   public void onResume() {
      _container = (TransactionHistoryFragmentContainer) this.getActivity();
      _mbwManager = _container.getMbwManager();
      _addressBook = _mbwManager.getAddressBookManager();
      _cache = _mbwManager.getCache();

      _root.findViewById(R.id.flTitle).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            refresh();
         }
      });

      registerForContextMenu(_root.findViewById(R.id.lvTransactionHistory));

      // Update from cache
      Set<Address> addressSet = _container.getWallet().getAddresses();
      _transactions = _cache.getTransactionSummaryList(addressSet);
      updateTransactionHistory();

      super.onResume();
   }

   @Override
   public void onDestroy() {
      if (_task != null) {
         _task.cancel();
      }
      super.onDestroy();
   }

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);

      // Get selected item
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
      TransactionSummary selected = (TransactionSummary) info.targetView.getTag();

      // Create menu
      Activity activity = TransactionHistoryFragment.this.getActivity();
      MenuInflater inflater = activity.getMenuInflater();
      inflater.inflate(R.menu.transaction_history_context_menu, menu);

      // Only show Set Label if we have a single foreign address in the
      // transaction
      String address = getSingleForeignAddressForTransaction(selected);
      if (address == null) {
         menu.findItem(R.id.miAddToAddressBook).setEnabled(false);
      } else {
         if (_addressBook.hasAddress(address)) {
            // If we have the address in the address book already change the
            // title of the menu
            menu.findItem(R.id.miAddToAddressBook).setTitle(R.string.edit_label);
         }
      }

      // Only show download invoice if we have one
      if (getInvoiceUrl(selected) == null) {
         menu.findItem(R.id.miDownloadInvoice).setVisible(false);
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {

      // Find selected item
      AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
      TransactionSummary selected = (TransactionSummary) info.targetView.getTag();

      if (item.getItemId() == R.id.miAddToAddressBook) {
         doSetLabel(selected);
         return true;
      } else if (item.getItemId() == R.id.miShowDetails) {
         doShowDetails(selected);
         return true;
      } else if (item.getItemId() == R.id.miDownloadInvoice) {
         doDownloadInvoice(selected);
         return true;
      } else {
         return false;
      }
   }

   private String getInvoiceUrl(TransactionSummary t) {
      if (t == null || _invoiceMap == null) {
         return null;
      }
      Wallet wallet = _container.getWallet();
      Set<Address> addressSet = wallet.getAddresses();
      TransactionType type = TransactionSummaryUtils.getTransactionType(t, addressSet);
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
      return _invoiceMap.get(candidates[0]);
   }

   private void doDownloadInvoice(TransactionSummary selected) {
      String urlString = getInvoiceUrl(selected);
      if (urlString == null) {
         return;
      }
      Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(urlString));
      startActivity(intent);
      Toast.makeText(getActivity(), R.string.downloading_invoice, Toast.LENGTH_LONG).show();
   }

   private void doSetLabel(TransactionSummary selected) {
      if (selected == null) {
         return;
      }
      // Set the label of the address
      String address = getSingleForeignAddressForTransaction(selected);
      if (address != null) {
         Utils.showSetAddressLabelDialog(getActivity(), _addressBook, address);
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
      Wallet wallet = _container.getWallet();
      Set<Address> addressSet = wallet.getAddresses();
      TransactionType type = TransactionSummaryUtils.getTransactionType(tx, addressSet);
      return type.singleForeignAddress(tx, addressSet);
   }

   public void refresh() {
      if (_task != null) {
         return;
      }
      if (_root == null) {
         // Not yet initialized
         return;
      }

      // Show cached balance and progress spinner
      _root.findViewById(R.id.pbHistory).setVisibility(View.VISIBLE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.GONE);
      Wallet wallet = _container.getWallet();
      Set<Address> addressSet = wallet.getAddresses();
      _transactions = _cache.getTransactionSummaryList(addressSet);
      updateTransactionHistory();
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
      _task = api.getTransactionSummary(addressSet, new QueryTransactionSummaryHandler());
   }

   class QueryTransactionSummaryHandler implements AbstractCallbackHandler<QueryTransactionSummaryResponse> {

      @Override
      public void handleCallback(QueryTransactionSummaryResponse response, ApiError exception) {
         _root.findViewById(R.id.pbHistory).setVisibility(View.GONE);
         _root.findViewById(R.id.ivRefresh).setVisibility(View.VISIBLE);
         if (exception != null) {
            Utils.toastConnectionError(getActivity());
         } else {
            _transactions = response;
            updateTransactionHistory();
            _task = fetchInvoices(_transactions);
         }
      }

   }

   private AsyncTask fetchInvoices(QueryTransactionSummaryResponse response) {
      Wallet wallet = _container.getWallet();
      Set<Address> addressSet = wallet.getAddresses();
      List<String> addresses = new LinkedList<String>();
      for (TransactionSummary t : response.transactions) {
         TransactionType type = TransactionSummaryUtils.getTransactionType(t, addressSet);
         if (type == TransactionType.SentToOthers) {
            String[] candidates = TransactionSummaryUtils.getReceiversNotMe(t, addressSet);
            if (candidates.length == 1) {
               addresses.add(candidates[0]);
            }
         }
      }
      AndroidAsyncApi api = _mbwManager.getAsyncApi();
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
         _root.findViewById(R.id.tvNoRecords).setVisibility(View.VISIBLE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.GONE);
      } else {
         Collections.sort(_transactions.transactions);
         _root.findViewById(R.id.tvNoRecords).setVisibility(View.GONE);
         _root.findViewById(R.id.lvTransactionHistory).setVisibility(View.VISIBLE);
         _transactionHistoryAdapter = new TransactionHistoryAdapter(getActivity(), _transactions);
         ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setAdapter(_transactionHistoryAdapter);
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
         Wallet wallet = _container.getWallet();
         _addressSet = wallet.getAddresses();
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
         DateFormat dateFormat = date.before(_midnight) ? _hourFormat : _dayFormat;
         TextView tvDate = (TextView) rowView.findViewById(R.id.tvDate);
         tvDate.setText(dateFormat.format(date));

         // Set value
         TextView tvAmount = (TextView) rowView.findViewById(R.id.tvAmount);
         tvAmount.setText(_mbwManager.getBtcValueString(value));
         tvAmount.setTextColor(color);

         // Determine list of addresses
         final String[] addresses = type.relevantAddresses(record, _addressSet);
         // Show/Hide Invoice if we have one
         if (value < 0 && addresses.length == 1 && _invoiceMap.containsKey(addresses[0])) {
            rowView.findViewById(R.id.tvInvoice).setVisibility(View.VISIBLE);
         } else {
            rowView.findViewById(R.id.tvInvoice).setVisibility(View.INVISIBLE);
         }

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

   private void fillInAddressBookNames(String[] addresses) {
      for (int i = 0; i < addresses.length; i++) {
         String name = _addressBook.getNameByAddress(addresses[i]);
         if (name.length() != 0) {
            addresses[i] = name;
         }
      }
   }

}
