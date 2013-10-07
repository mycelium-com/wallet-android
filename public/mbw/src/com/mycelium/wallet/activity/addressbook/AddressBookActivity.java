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

package com.mycelium.wallet.activity.addressbook;

import java.util.LinkedList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.AddressBookManager.Entry;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.addressbook.EnterAddressLabelUtil.AddressLabelChangedHandler;

public class AddressBookActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;
   public static final String ADDRESS_RESULT_NAME = "address_result";

   private String mSelectedAddress;
   private MbwManager _mbwManager;
   private RecordManager _recordManager;
   private AddressBookManager _addressBook;
   private AlertDialog _qrCodeDialog;
   private Dialog _addDialog;
   private boolean _selectMode;

   public static void callMeForResult(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, AddressBookActivity.class);
      intent.putExtra("selectMode", true);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, AddressBookActivity.class);
      currentActivity.startActivity(intent);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.address_book_activity);

      _mbwManager = MbwManager.getInstance(getApplication());
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();

      // Get intent parameters
      _selectMode = getIntent().getBooleanExtra("selectMode", false);

      int currentTab = 1;
      // Load state
      if (savedInstanceState != null) {
         currentTab = savedInstanceState.getInt("selectedTab");
      }
      ListView myList = (ListView) findViewById(R.id.lvMyAddresses);
      ListView foreignList = (ListView) findViewById(R.id.lvForeignAddresses);
      if (!_selectMode) {
         registerForContextMenu(myList);
         registerForContextMenu(foreignList);
      }
      myList.setLongClickable(false);
      foreignList.setLongClickable(false);

      myList.setOnItemClickListener(myListClickListener);
      foreignList.setOnItemClickListener(foreignListClickListener);
      // Show hide Add button
      if (!_selectMode) {
         findViewById(R.id.btAdd).setOnClickListener(new AddClicked());
      } else {
         findViewById(R.id.btAdd).setVisibility(View.GONE);
      }

      // Add tabs
      TabHost tabs = (TabHost) findViewById(R.id.TabHost);
      tabs.setup();
      addTab(tabs, R.string.my_addresses, "my_addresses", R.id.lvMyAddresses);
      addTab(tabs, R.string.foreign_addresses, "foreign_addresses", R.id.llForeignAddresses);
      tabs.setCurrentTab(currentTab);
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      outState.putInt("selectedTab", ((TabHost) findViewById(R.id.TabHost)).getCurrentTab());
   };

   private void addTab(TabHost tabHost, int titleResourceId, String tag, int contentResourceId) {

      View view = LayoutInflater.from(this).inflate(R.layout.tab_with_text, null);
      TextView tv = (TextView) view.findViewById(R.id.tabsText);
      tv.setText(titleResourceId);
      TabHost.TabSpec spec = tabHost.newTabSpec(tag);
      spec.setContent(contentResourceId);
      spec.setIndicator(view);
      tabHost.addTab(spec);
   }

   private class AddClicked implements OnClickListener {

      @Override
      public void onClick(View v) {
         _addDialog = new AddDialog(AddressBookActivity.this);
         _addDialog.show();
      }
   }

   @Override
   public void onResume() {
      super.onResume();
      updateEntries();
   }

   @Override
   protected void onDestroy() {
      if (_qrCodeDialog != null) {
         _qrCodeDialog.dismiss();
      }
      if (_addDialog != null && _addDialog.isShowing()) {
         _addDialog.dismiss();
      }
      super.onDestroy();
   }

   private void updateEntries() {
      updateForeignEntries();
      updateMyEntries();
   }

   private void updateForeignEntries() {
      List<Entry> all = _addressBook.getEntries();
      List<Entry> foreign = new LinkedList<Entry>();
      for (Entry entry : all) {
         if (_recordManager.getRecord(entry.getAddress()) == null) {
            foreign.add(entry);
         }
      }
      ListView foreignList = (ListView) findViewById(R.id.lvForeignAddresses);
      foreignList.setAdapter(new ForeignAddressBookAdapter(this, R.layout.address_book_foreign_row, foreign));
   }

   private void updateMyEntries() {
      // Build list of rows. Each wallet and record get a row
      List<Row> rows = new LinkedList<Row>();
      for (Record record : _recordManager.getRecords(Tag.ACTIVE)) {
         // Add wallet names if we have more then one wallet
         String rowName = _addressBook.getNameByAddress(record.address.toString());
         rows.add(new Row(rowName, record));
      }

      ListView myList = (ListView) findViewById(R.id.lvMyAddresses);
      myList.setAdapter(new MyAddressBookAdapter(this, R.layout.address_book_my_address_row, rows));
   }

   OnItemClickListener myListClickListener = new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
         Row row = (Row) view.getTag();
         if (row.record == null) {
            // Wallet clicked
            return;
         }
         if (_selectMode) {
            Intent result = new Intent();
            result.putExtra(ADDRESS_RESULT_NAME, row.record.address.toString());
            setResult(RESULT_OK, result);
            finish();
         } else {
            mSelectedAddress = row.record.address.toString();
            listView.showContextMenuForChild(view);
         }
      }
   };

   OnItemClickListener foreignListClickListener = new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> listView, View view, int position, long id) {
         if (_selectMode) {
            String value = (String) view.getTag();
            Intent result = new Intent();
            result.putExtra(ADDRESS_RESULT_NAME, value);
            setResult(RESULT_OK, result);
            finish();
         } else {
            mSelectedAddress = (String) view.getTag();
            listView.showContextMenuForChild(view);
         }
      }
   };

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.addressbook_context_menu, menu);
      // hide delete for addresses we own
      if (findViewById(R.id.lvMyAddresses) == v) {
         menu.findItem(R.id.miDeleteAddress).setVisible(false);
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      if (item.getItemId() == R.id.miDeleteAddress) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedDeleteEntry);
         return true;
      } else if (item.getItemId() == R.id.miEditAddress) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedEditEntry);
         return true;
      } else if (item.getItemId() == R.id.miShowQrCode) {
         doShowQrCode();
         return true;
      } else {
         return false;
      }
   }

   final Runnable pinProtectedEditEntry = new Runnable() {

      @Override
      public void run() {
         doEditEntry();
      }
   };

   private void doEditEntry() {
      EnterAddressLabelUtil.enterAddressLabel(this, _addressBook, mSelectedAddress, addressLabelChanged);

   }

   private void doShowQrCode() {
      String address = "bitcoin:" + mSelectedAddress;
      Bitmap bitmap = Utils.getLargeQRCodeBitmap(address, _mbwManager);
      _qrCodeDialog = Utils.showQrCode(this, R.string.bitcoin_address_title, bitmap, mSelectedAddress,
            R.string.copy_address_to_clipboard, _mbwManager.getPulsingQrCodes());
   }

   final Runnable pinProtectedDeleteEntry = new Runnable() {

      @Override
      public void run() {
         doDeleteEntry();
      }
   };

   private void doDeleteEntry() {
      AlertDialog.Builder builder = new AlertDialog.Builder(this);
      builder.setMessage(R.string.delete_address_confirmation).setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  _addressBook.deleteEntry(mSelectedAddress);
                  updateEntries();
               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   private static class Row {
      public String name;
      public Record record;

      public Row(String name, Record record) {
         this.name = name;
         this.record = record;
      }
   }

   private class MyAddressBookAdapter extends ArrayAdapter<Row> {

      private LayoutInflater _inflater;

      public MyAddressBookAdapter(Context context, int textViewResourceId, List<Row> objects) {
         super(context, textViewResourceId, objects);
         _inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         Row e = getItem(position);

         View v;
         if (e.record == null) {
            // Wallet row
            v = Preconditions.checkNotNull(_inflater.inflate(R.layout.address_book_my_wallet_row, null));
            ((TextView) v.findViewById(R.id.tvName)).setText(e.name);
         } else {
            // Record row
            v = Preconditions.checkNotNull(_inflater.inflate(R.layout.address_book_my_address_row, null));
            ((TextView) v.findViewById(R.id.tvName)).setText(e.name);
            TextView tvAddress = (TextView) v.findViewById(R.id.tvAddress);
            tvAddress.setText(Address.fromString(e.record.address.toString()).toMultiLineString());
         }

         v.setTag(e);
         return v;
      }
   }

   private class ForeignAddressBookAdapter extends ArrayAdapter<Entry> {

      public ForeignAddressBookAdapter(Context context, int textViewResourceId, List<Entry> objects) {
         super(context, textViewResourceId, objects);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = Preconditions.checkNotNull(vi.inflate(R.layout.address_book_row, null));
         }
         TextView tvName = (TextView) v.findViewById(R.id.address_book_name);
         TextView tvAddress = (TextView) v.findViewById(R.id.address_book_address);
         Entry e = getItem(position);
         tvName.setText(e.getName());
         tvAddress.setText(Address.fromString(e.getAddress()).toMultiLineString());
         v.setTag(e.getAddress());
         return v;
      }
   }

   private class AddDialog extends Dialog {

      public AddDialog(final Activity activity) {
         super(activity);
         this.setContentView(R.layout.add_to_address_book_dialog);
         this.setTitle(R.string.add_to_address_book_dialog_title);

         findViewById(R.id.btScan).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Utils.startScannerIntent(activity, SCANNER_RESULT_CODE, _mbwManager.getContinuousFocus());
               AddDialog.this.dismiss();
            }

         });

         Address address = Utils.addressFromString(Utils.getClipboardString(AddressBookActivity.this));
         findViewById(R.id.btClipboard).setEnabled(address != null);
         findViewById(R.id.btClipboard).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               String addressString = Utils.getClipboardString(AddressBookActivity.this);
               addFromString(addressString);
               AddDialog.this.dismiss();
            }

         });

      }

   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCANNER_RESULT_CODE && resultCode == RESULT_OK) {
         if (!"QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
            return;
         }
         String contents = intent.getStringExtra("SCAN_RESULT").trim();
         addFromString(contents);
      }
   }

   private void addFromString(String addressString) {
      Address address = Utils.addressFromString(addressString);
      if (address == null) {
         return;
      }

      EnterAddressLabelUtil.enterAddressLabel(AddressBookActivity.this, _addressBook, address.toString(),
            addressLabelChanged);
   }

   private AddressLabelChangedHandler addressLabelChanged = new AddressLabelChangedHandler() {

      @Override
      public void OnAddressLabelChanged(String address, String label) {
         updateEntries();
      }
   };

}
