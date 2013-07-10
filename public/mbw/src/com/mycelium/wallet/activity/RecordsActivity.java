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

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SpinnerPrivateUri;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.SimpleGestureFilter;
import com.mycelium.wallet.SimpleGestureFilter.SimpleGestureListener;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.addressbook.AddressBookActivity;
import com.mycelium.wallet.activity.export.ExportActivity;

public class RecordsActivity extends Activity implements SimpleGestureListener {

   public static final int SCANNER_RESULT_CODE = 0;
   public static final int CREATE_RESULT_CODE = 1;

   private RecordManager _recordManager;
   private SimpleGestureFilter _gestureFilter;
   private RecordsAdapter _recordsAdapter;
   private AddressBookManager _addressBook;
   private MbwManager _mbwManager;
   private Dialog _dialog;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.records_activity);

      findViewById(R.id.btAdd).setOnClickListener(new AddClicked());

      ((ListView) findViewById(R.id.lvRecords)).setOnItemClickListener(new RecordClicked());
      ((ListView) findViewById(R.id.lvRecords)).setOnItemLongClickListener(new RecordLongClicked());

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();

      registerForContextMenu(findViewById(R.id.lvRecords));

   }

   private class AddClicked implements OnClickListener {

      @Override
      public void onClick(View v) {
         _dialog = new AddDialog(RecordsActivity.this);
         _dialog.show();
      }
   }

   class RecordClicked implements OnItemClickListener {

      @Override
      public void onItemClick(AdapterView<?> list, View v, int position, long id) {
         if (v.getTag() == null || !(v.getTag() instanceof Record)) {
            return;
         }

         Record record = (Record) v.getTag();
         _recordManager.setSelectedRecord(record.address);
         _recordsAdapter.setSelected(_recordManager.getSelectedRecord());
         _recordsAdapter.notifyDataSetChanged();
         list.showContextMenuForChild(v);
      }

   }

   class RecordLongClicked implements OnItemLongClickListener {

      @Override
      public boolean onItemLongClick(AdapterView<?> list, View v, int position, long id) {
         if (v.getTag() == null || !(v.getTag() instanceof Record)) {
            return true;
         }

         Record record = (Record) v.getTag();
         _recordManager.setSelectedRecord(record.address);
         _recordsAdapter.setSelected(_recordManager.getSelectedRecord());
         _recordsAdapter.notifyDataSetChanged();
         return false; // If we return true here the context menu is not shown
      }

   }

   private class AddDialog extends Dialog {

      public AddDialog(final Activity activity) {
         super(activity);
         this.setContentView(R.layout.add_dialog);
         this.setTitle(R.string.add_dialog_title);

         findViewById(R.id.btScan).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Utils.startScannerIntent(activity, SCANNER_RESULT_CODE);
               AddDialog.this.dismiss();
            }

         });

         findViewById(R.id.btClipboard).setEnabled(
               isBitcoinAddressOrPrivateKey(Utils.getClipboardString(RecordsActivity.this)));
         findViewById(R.id.btClipboard).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Record record = addRecordFromString(Utils.getClipboardString(RecordsActivity.this), false);
               // If the record has a private key delete the contents of the
               // clipboard
               if (record.hasPrivateKey()) {
                  Utils.clearClipboardString(RecordsActivity.this);
               }
               AddDialog.this.dismiss();
            }

         });

         findViewById(R.id.btRandom).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Intent intent = new Intent(activity, CreateKeyActivity.class);
               activity.startActivityForResult(intent, CREATE_RESULT_CODE);
               AddDialog.this.dismiss();
            }

         });
      }

   }

   @Override
   protected void onDestroy() {
      if (_dialog != null && _dialog.isShowing()) {
         _dialog.dismiss();
      }
      super.onDestroy();
   }

   @Override
   protected void onResume() {
      _gestureFilter = new SimpleGestureFilter(this, this);
      // if (_recordManager.numRecords() == 0) {
      // new AddDialog(RecordsActivity.this).show();
      // }
      update();
      animateSwipe();
      super.onResume();
   }

   private void animateSwipe() {
      // Visible swipe disabled for now
      // long speed = 250;
      // long delay = 100;
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow1), delay * 7, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow2), delay * 6, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow3), delay * 5, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow4), delay * 4, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow5), delay * 3, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow6), delay * 2, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow7), delay * 1, speed,
      // 0);
      // Utils.fadeViewInOut(findViewById(R.id.tvLeftArrow8), delay * 0, speed,
      // 0);
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCANNER_RESULT_CODE && resultCode == RESULT_OK) {
         handleScannerIntentResult(intent);
      } else if (requestCode == CREATE_RESULT_CODE && resultCode == RESULT_OK) {
         handleCreateKeyIntentResult(intent);
      }
   }

   private void deleteRecord(final Record record) {
      if (record == null) {
         return;
      }
      AlertDialog.Builder deleteDialog = new AlertDialog.Builder(RecordsActivity.this);
      String title = record.hasPrivateKey() ? "Delete Private Key?" : "Delete Bitcoin Address?";
      String message = record.hasPrivateKey() ? "Do you want to delete this private key?"
            : "Do you want to delete this address?";
      deleteDialog.setTitle(title);
      deleteDialog.setMessage(message);
      deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
            if (record.hasPrivateKey()) {
               AlertDialog.Builder confirmDialog = new AlertDialog.Builder(RecordsActivity.this);
               String title = "Are You Sure?";
               String message = "This cannot be undone!\rDo you want to delete this private key?";
               confirmDialog.setTitle(title);
               confirmDialog.setMessage(message);
               confirmDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                     _recordManager.forgetPrivateKeyForRcordByAddress(record.address);
                     update();
                     Toast.makeText(RecordsActivity.this, R.string.private_key_deleted, Toast.LENGTH_LONG).show();
                  }
               });
               confirmDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                  }
               });
               confirmDialog.show();
            } else {
               _recordManager.deleteRecord(record.address);
               _addressBook.deleteEntry(record.address.toString());
               update();
               Toast.makeText(RecordsActivity.this, R.string.bitcoin_address_deleted, Toast.LENGTH_LONG).show();
            }
         }
      });
      deleteDialog.setNegativeButton("No", new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
         }
      });
      deleteDialog.show();

   }

   private void handleCreateKeyIntentResult(Intent intent) {
      String base58Key = intent.getStringExtra("base58key");
      Record record = recordFromBase58Key(base58Key);
      if (record != null) {
         addRecord(record);
      }
   }

   private void handleScannerIntentResult(final Intent intent) {
      if (!"QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
         return;
      }
      String contents = intent.getStringExtra("SCAN_RESULT").trim();
      Record record = addRecordFromString(contents, true);

      // Scanner leaves the result on the clipboard, delete it if the record has
      // a private key
      if (record != null && record.hasPrivateKey()) {
         // todo prevent the code from filling the clipboad in the first place
         Utils.clearClipboardString(this);
      }
   }

   private Record addRecordFromString(String string, boolean allowSeeds) {
      // Do we have a Bitcoin address
      Record record;
      record = recordFromBitcoinAddressString(string);
      if (record != null) {
         addRecord(record);
         return record;
      }

      // Do we have a Base58 private key
      record = recordFromBase58Key(string);
      if (record != null) {
         addRecord(record);
         return record;
      }

      // Do we have a mini private key
      record = recordFromBase58KeyMimiFormat(string);
      if (record != null) {
         addRecord(record);
         return record;
      }

      // Do we have a Bitcoin Spinner backup key
      record = recordFromBitcoinSpinnerBackup(string);
      if (record != null) {
         addRecord(record);
         return record;
      }

      // // Treat it as a random seed
      // record = recordFromRandomSeed(string);
      // if (record != null) {
      // addRecord(record);
      // return;
      // }
      Toast.makeText(RecordsActivity.this, R.string.unrecognized_format, Toast.LENGTH_SHORT).show();
      return null;
   }

   private boolean addRecord(Record record) {
      Record existing = _recordManager.getRecord(record.address);

      if (existing == null) {
         // We have a new record
         if (record.hasPrivateKey()) {
            Toast.makeText(this, R.string.imported_private_key, Toast.LENGTH_LONG).show();
         } else {
            Toast.makeText(this, R.string.imported_bitcoin_address, Toast.LENGTH_LONG).show();
         }
         _recordManager.addRecord(record);
         _recordManager.setSelectedRecord(record.address);
         update();
         return true;
      }

      // The record has the same address as one we have already
      if (existing.hasPrivateKey() && record.hasPrivateKey()) {
         // Nothing to do as we already have the record with the private key
         Toast.makeText(this, R.string.key_already_present, Toast.LENGTH_LONG).show();
         return false;
      } else if (!existing.hasPrivateKey() && !record.hasPrivateKey()) {
         Toast.makeText(this, R.string.address_already_present, Toast.LENGTH_LONG).show();
         // Nothing to do as none of the records have the private key
         return false;
      } else if (!existing.hasPrivateKey() && record.hasPrivateKey()) {
         // We upgrade to a record with a private key
         Toast.makeText(this, R.string.upgraded_address_to_key, Toast.LENGTH_LONG).show();
         _recordManager.addRecord(record);
         _recordManager.setSelectedRecord(record.address);
         update();
         return true;
      } else if (existing.hasPrivateKey() && !record.hasPrivateKey()) {
         Toast.makeText(this, R.string.address_already_present, Toast.LENGTH_LONG).show();
         // The new record does not have a private key, do nothing as we do not
         // do downgrades
         return false;
      }

      // We never get here
      return false;
   }

   private static boolean isBitcoinAddressOrPrivateKey(String string) {
      if (recordFromBitcoinAddressString(string) != null) {
         return true;
      }
      if (recordFromBase58Key(string) != null) {
         return true;
      }
      if (recordFromBase58KeyMimiFormat(string) != null) {
         return true;
      }
      if (recordFromBitcoinSpinnerBackup(string) != null) {
         return true;
      }
      return false;
   }

   private static Record recordFromBitcoinAddressString(String string) {
      // Is it an address?
      Address address = Utils.addressFromString(string);
      if (address != null) {
         // We have an address
         return new Record(address, System.currentTimeMillis());
      }
      return null;
   }

   private static Record recordFromBase58Key(String string) {
      // Is it a private key?
      try {
         InMemoryPrivateKey key = new InMemoryPrivateKey(string, Constants.network);
         return new Record(key, System.currentTimeMillis());
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static Record recordFromBase58KeyMimiFormat(String string) {
      // Is it a mini private key on the format proposed by Casascius?
      if (string == null || string.length() < 2 || !string.startsWith("S")) {
         return null;
      }
      // Check that the string has a valid checksum
      String withQuestionMark = string + "?";
      byte[] checkHash = HashUtils.sha256(withQuestionMark.getBytes());
      if (checkHash[0] != 0x00) {
         return null;
      }
      // Now get the Sha256 hash and use it as the private key
      byte[] privateKeyBytes = HashUtils.sha256(string.getBytes());
      try {
         InMemoryPrivateKey key = new InMemoryPrivateKey(privateKeyBytes, false);
         return new Record(key, System.currentTimeMillis());
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   private static Record recordFromBitcoinSpinnerBackup(String string) {
      try {
         SpinnerPrivateUri spinnerKey = SpinnerPrivateUri.fromSpinnerUri(string);
         if (!spinnerKey.network.equals(Constants.network)) {
            return null;
         }
         return new Record(spinnerKey.key, System.currentTimeMillis());
      } catch (IllegalArgumentException e) {
         return null;
      }
   }

   /**
    * Add a record from a seed using the same mechanism as brainwallet.org
    */
   @SuppressWarnings("unused")
   private static Record recordFromRandomSeed(String seed, boolean compressed) {
      byte[] bytes = null;
      bytes = HashUtils.sha256(seed.getBytes());
      InMemoryPrivateKey key = new InMemoryPrivateKey(bytes, compressed);
      return new Record(key, System.currentTimeMillis());
   }

   private void update() {
      // Disable Add button if we have reached the limit of how many
      // addresses/keys to manage
      findViewById(R.id.btAdd).setEnabled(_recordManager.numRecords() < MyceliumWalletApi.MAXIMUM_ADDRESSES_PER_REQUEST);

      ListView listView = (ListView) findViewById(R.id.lvRecords);
      _recordsAdapter = new RecordsAdapter(this, _recordManager.getRecords(), _recordManager.getSelectedRecord());
      listView.setAdapter(_recordsAdapter);
      // Find selected position, and scroll to it
      int position = _recordsAdapter.getPosition(_recordManager.getSelectedRecord());
      if (position != -1) {
         listView.smoothScrollToPosition(position);
      }
   }

   class RecordsAdapter extends ArrayAdapter<Record> {
      private Context _context;
      private Record _selected;

      public RecordsAdapter(Context context, List<Record> records, Record selected) {
         super(context, R.layout.record_row, records);
         _context = context;
         _selected = selected;

      }

      public void setSelected(Record selected) {
         _selected = selected;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         View rowView = inflater.inflate(R.layout.record_row, parent, false);
         Record record = getItem(position);

         // Show/hide key icon
         if (!record.hasPrivateKey()) {
            rowView.findViewById(R.id.ivkey).setVisibility(View.INVISIBLE);
         }

         // Set Label
         String address = record.address.toString();
         String name = _addressBook.getNameByAddress(address);
         if (name.length() == 0) {
            ((TextView) rowView.findViewById(R.id.tvLabel)).setVisibility(View.GONE);
         } else {
            // Display name
            TextView tvLabel = ((TextView) rowView.findViewById(R.id.tvLabel));
            tvLabel.setVisibility(View.VISIBLE);
            tvLabel.setText(name);
         }

         String displayAddress;
         if (name.length() == 0) {
            // Display address in it's full glory, chopping it into three
            displayAddress = getChoppedAddress(address);
         } else {
            // Display address in short form
            displayAddress = getShortAddress(address);
         }
         ((TextView) rowView.findViewById(R.id.tvAddress)).setText(displayAddress);

         // Set tag
         rowView.setTag(record);

         // Show selected adderss with a different color
         if (record.address.equals(_selected.address)) {
            rowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_blue));
         } else {
            rowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black));
         }
         return rowView;
      }

      private String getShortAddress(String address) {
         StringBuilder sb = new StringBuilder();
         sb.append(address.substring(0, 6));
         sb.append("...");
         sb.append(address.substring(address.length() - 6));
         return sb.toString();
      }

      private String getChoppedAddress(String address) {
         StringBuilder sb = new StringBuilder();
         sb.append(address.substring(0, 12)).append("\r\n");
         sb.append(address.substring(12, 24)).append("\r\n");
         sb.append(address.substring(24)).toString();
         return sb.toString();
      }
   }

   @Override
   public void onSwipe(int direction) {
      if (direction == SimpleGestureFilter.SWIPE_LEFT) {
         openBalanceView();
      }
   }

   @Override
   public void onDoubleTap() {
   }

   @Override
   public boolean dispatchTouchEvent(MotionEvent me) {
      this._gestureFilter.onTouchEvent(me);
      return super.dispatchTouchEvent(me);
   }

   /** Called when menu button is pressed. */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.record_options_menu, menu);
      return true;
   }

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.records_context_menu, menu);

      if (_recordManager.getSelectedRecord().hasPrivateKey()) {
         menu.findItem(R.id.miDeleteAddress).setVisible(false);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(true);
         menu.findItem(R.id.miExport).setVisible(true);
      } else {
         menu.findItem(R.id.miDeleteAddress).setVisible(true);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(false);
         menu.findItem(R.id.miExport).setVisible(false);
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      if (item.getItemId() == R.id.miOpen) {
         openBalanceView();
         return true;
      } else if (item.getItemId() == R.id.miSetLabel) {
         Utils.showSetAddressLabelDialog(this, _addressBook, _recordManager.getSelectedRecord().address.toString());
         return true;
      } else if (item.getItemId() == R.id.miDeleteAddress || item.getItemId() == R.id.miDeletePrivateKey) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedDeleteRecord);
         return true;
      } else if (item.getItemId() == R.id.miExport) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedExport);
         return true;
      } else {
         return false;
      }
   }

   private void openBalanceView() {
      Intent intent = new Intent(RecordsActivity.this, BalanceActivity.class);
      startActivity(intent);
      finish();
      this.overridePendingTransition(R.anim.right_to_left_enter, R.anim.right_to_left_exit);
   }

   @Override
   public boolean onKeyDown(int keyCode, KeyEvent event) {
      if (keyCode == KeyEvent.KEYCODE_BACK) {
         openBalanceView();
         return true;
      }
      return super.onKeyDown(keyCode, event);
   }

   private void exportPrivateKey(final Record record) {
      AlertDialog.Builder builder = new AlertDialog.Builder(RecordsActivity.this);
      builder.setMessage(R.string.export_private_key_warning).setCancelable(false)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
                  if (record == null) {
                     return;
                  }
                  Intent intent = new Intent(RecordsActivity.this, ExportActivity.class);
                  startActivity(intent);

               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miSettings) {
         Intent intent = new Intent(RecordsActivity.this, SettingsActivity.class);
         startActivity(intent);
         return true;
      } else if (item.getItemId() == R.id.miAddressBook) {
         Intent intent = new Intent(RecordsActivity.this, AddressBookActivity.class);
         startActivity(intent);
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   final Runnable pinProtectedDeleteRecord = new Runnable() {

      @Override
      public void run() {
         deleteRecord(_recordManager.getSelectedRecord());
      }
   };

   final Runnable pinProtectedExport = new Runnable() {

      @Override
      public void run() {
         Record record = _recordManager.getSelectedRecord();
         if (record == null) {
            return;
         }
         exportPrivateKey(record);
      }
   };

}