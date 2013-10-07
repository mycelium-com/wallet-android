package com.mycelium.wallet.activity.main;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
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
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.TransactionSummary;
import com.mrd.mbwapi.util.TransactionSummaryUtils;
import com.mrd.mbwapi.util.TransactionType;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;
import com.mycelium.wallet.activity.TransactionDetailsActivity;
import com.mycelium.wallet.activity.addressbook.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.addressbook.EnterAddressLabelUtil.AddressLabelChangedHandler;
import com.mycelium.wallet.api.ApiCache;

public class TransactionHistoryFragment extends Fragment implements WalletFragmentObserver {

   public interface TransactionHistoryFragmentContainer {
      public MbwManager getMbwManager();

      public void requestTransactionHistoryRefresh();

      public void addObserver(WalletFragmentObserver observer);

      public void removeObserver(WalletFragmentObserver observer);

   }

   private TransactionHistoryFragmentContainer _container;
   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private View _root;
   private TransactionHistoryAdapter _transactionHistoryAdapter;
   private Map<String, String> _invoiceMap;
   private ApiCache _cache;
   private AddressBookManager _addressBook;
   AsyncTransactionHistoryUpdate _updater;

   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      _root = (ViewGroup) inflater.inflate(R.layout.main_transaction_history_view, container, false);
      return _root;
   }

   @Override
   public void onResume() {
      _container = (TransactionHistoryFragmentContainer) this.getActivity();
      _mbwManager = _container.getMbwManager();
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();
      _cache = _mbwManager.getCache();

      _root.findViewById(R.id.flTitle).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            _container.requestTransactionHistoryRefresh();
         }
      });

      registerForContextMenu(_root.findViewById(R.id.lvTransactionHistory));

      // Update from cache
      updateTransactionHistory();
      _container.addObserver(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      _container.removeObserver(this);
      super.onPause();
   }

   @Override
   public void onDestroy() {
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
      Wallet wallet = getWallet();
      Set<Address> addressSet = wallet.getAddressSet();
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
         EnterAddressLabelUtil.enterAddressLabel(getActivity(), _addressBook, address, addressLabelChanged);
      }
   }

   private AddressLabelChangedHandler addressLabelChanged = new AddressLabelChangedHandler() {

      @Override
      public void OnAddressLabelChanged(String address, String label) {
         updateTransactionHistory();
      }
   };

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
      if (_updater != null) {
         // Attempt to cancel the old one if it is still running
         _updater.cancel(false);
      }
      Set<Address> addressSet = getWallet().getAddressSet();
      _updater = new AsyncTransactionHistoryUpdate();
      _updater.execute(addressSet);
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
            _transactionHistoryAdapter = new TransactionHistoryAdapter(getActivity(), result);
            ((ListView) _root.findViewById(R.id.lvTransactionHistory)).setAdapter(_transactionHistoryAdapter);
         }
         super.onPostExecute(result);
      }
   }

   private class TransactionHistoryAdapter extends ArrayAdapter<TransactionSummary> {
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
         Wallet wallet = getWallet();
         _addressSet = wallet.getAddressSet();
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

   @Override
   public void walletChanged(Wallet wallet) {
      if (!isAdded()) {
         return;
      }
      updateTransactionHistory();
   }

   @Override
   public void balanceUpdateStarted() {
   }

   @Override
   public void balanceUpdateStopped() {
   }

   @Override
   public void balanceChanged(BalanceInfo info) {
   }

   @Override
   public void transactionHistoryUpdateStarted() {
      if (!isAdded()) {
         return;
      }
      // Show progress spinner
      _root.findViewById(R.id.pbHistory).setVisibility(View.VISIBLE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.GONE);
   }

   @Override
   public void transactionHistoryUpdateStopped() {
      if (!isAdded()) {
         return;
      }
      // Show refresh icon
      _root.findViewById(R.id.pbHistory).setVisibility(View.GONE);
      _root.findViewById(R.id.ivRefresh).setVisibility(View.VISIBLE);
   }

   @Override
   public void transactionHistoryChanged() {
      if (!isAdded()) {
         return;
      }
      updateTransactionHistory();
   }

   @Override
   public void invoiceMapChanged(Map<String, String> invoiceMap) {
      if (!isAdded()) {
         return;
      }
      _invoiceMap = invoiceMap;
      if (_transactionHistoryAdapter != null) {
         _transactionHistoryAdapter.setInvoiceMap(invoiceMap);
      }
   }

   @Override
   public void newExchangeRate(Double oneBtcInFiat) {
   }

}
