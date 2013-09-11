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

package com.mycelium.wallet.activity;

import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.Wallet.BalanceInfo;

public class RecordsActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;
   public static final int CREATE_RESULT_CODE = 1;
   public static final int MANUAL_RESULT_CODE = 2;

   private RecordManager _recordManager;
   private AddressBookManager _addressBook;
   private MbwManager _mbwManager;
   private Dialog _dialog;
   private LayoutInflater _layoutInflater;
   private int _separatorColor;
   private LayoutParams _separatorLayoutParameters;
   private LayoutParams _titleLayoutParameters;
   private LayoutParams _outerLayoutParameters;
   private LayoutParams _innerLayoutParameters;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.records_activity);
      _layoutInflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      // findViewById(R.id.btAdd).setOnClickListener(new AddClicked());

      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();
      _separatorColor = getResources().getColor(R.color.darkgrey);
      _separatorLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, getDipValue(1), 1);
      _titleLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      _outerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      _outerLayoutParameters.bottomMargin = getDipValue(8);
      _innerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
   }

   private int getDipValue(int dip) {
      return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
   }

   private class AddClicked implements OnClickListener {

      @Override
      public void onClick(View v) {
         _dialog = new AddDialog(RecordsActivity.this);
         _dialog.show();
      }
   }

   private class AddDialog extends Dialog {

      public AddDialog(final Activity activity) {
         super(activity);
         this.setContentView(R.layout.add_dialog);
         this.setTitle(R.string.add_dialog_title);
         TextView title = (TextView) this.findViewById(android.R.id.title);
         title.setSingleLine(false);

         findViewById(R.id.btScan).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Utils.startScannerIntent(activity, SCANNER_RESULT_CODE, _mbwManager.getContinuousFocus());
               AddDialog.this.dismiss();
            }

         });

         findViewById(R.id.btClipboard).setEnabled(Record.isRecord(Utils.getClipboardString(RecordsActivity.this)));
         findViewById(R.id.btClipboard).setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
               Record record = addRecordFromString(Utils.getClipboardString(RecordsActivity.this));
               // If the record has a private key delete the contents of the
               // clipboard
               if (record != null && record.hasPrivateKey()) {
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
         /*
          * findViewById(R.id.btManual).setOnClickListener(new
          * View.OnClickListener() {
          * 
          * @Override public void onClick(View view) { Intent intent = new
          * Intent(activity, ManualAddressEntry.class);
          * activity.startActivityForResult(intent, MANUAL_RESULT_CODE); } });
          */
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
      update();
      super.onResume();
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == SCANNER_RESULT_CODE && resultCode == RESULT_OK) {
         handleScannerIntentResult(intent);
      } else if (requestCode == CREATE_RESULT_CODE && resultCode == RESULT_OK) {
         handleCreateKeyIntentResult(intent);
      } else if (requestCode == MANUAL_RESULT_CODE && resultCode == RESULT_OK) {
         String base58Key = intent.getStringExtra("base58key");
         Record record = Record.recordFromBitcoinAddressString(base58Key);
         if (record != null) {
            addRecord(record);
         }
      }
   }

   private void deleteRecord(final Record record) {
      if (record == null) {
         return;
      }
      final View checkBoxView = View.inflate(this, R.layout.delkey_checkbox, null);
      final CheckBox keepAddrCheckbox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
      keepAddrCheckbox.setText(getString(R.string.keep_address));
      keepAddrCheckbox.setChecked(false);

      final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(RecordsActivity.this);
      String title = record.hasPrivateKey() ? "Delete Private Key?" : "Delete Bitcoin Address?";
      String message = record.hasPrivateKey() ? "Do you want to delete this private key?"
            : "Do you want to delete this address?";
      deleteDialog.setTitle(title);
      deleteDialog.setMessage(message);

      if (record.hasPrivateKey()) { // add checkbox only if private key is
         // present
         deleteDialog.setView(checkBoxView);
      }

      deleteDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
            if (record.hasPrivateKey()) {
               AlertDialog.Builder confirmDialog = new AlertDialog.Builder(RecordsActivity.this);
               String title = "Are You Sure?";
               confirmDialog.setTitle(title);
               confirmDialog.setMessage(getString(R.string.question_delete_pk));
               confirmDialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                     if (keepAddrCheckbox.isChecked()) {
                        _recordManager.forgetPrivateKeyForRecordByAddress(record.address);
                     } else {
                        _recordManager.deleteRecord(record.address);
                        _addressBook.deleteEntry(record.address.toString());
                     }
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
      Record record = Record.recordFromBase58Key(base58Key);
      if (record != null) {
         addRecord(record);
      }
   }

   private void handleScannerIntentResult(final Intent intent) {
      if (!"QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT"))) {
         return;
      }
      String contents = intent.getStringExtra("SCAN_RESULT").trim();
      Record record = addRecordFromString(contents);

      // Scanner leaves the result on the clipboard, delete it if the record has
      // a private key
      if (record != null && record.hasPrivateKey()) {
         // todo prevent the code from filling the clipboad in the first place
         Utils.clearClipboardString(this);
      }
   }

   private Record addRecordFromString(String string) {
      // Do we have a Bitcoin address
      Record record = Record.fromString(string);
      if (record != null) {
         addRecord(record);
         return record;
      }
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

   private void update() {

      LinearLayout linearLayout = (LinearLayout) findViewById(R.id.llRecords);
      linearLayout.removeAllViews();

      List<Record> activeRecords = _recordManager.getRecords(Tag.ACTIVE);
      List<Record> archivedRecords = _recordManager.getRecords(Tag.ARCHIVE);
      Record selectedRecord = _recordManager.getSelectedRecord();
      LinearLayout active = createRecordViewList(activeRecords.isEmpty() ? R.string.active_empty : R.string.active,
            activeRecords, selectedRecord, true);
      linearLayout.addView(active);
      linearLayout.addView(createRecordViewList(archivedRecords.isEmpty() ? R.string.archive_empty : R.string.archive,
            archivedRecords, selectedRecord, false));

      // Disable Add button if we have reached the limit of how many
      // addresses/keys to manage
      findViewById(R.id.btAdd).setEnabled(
            _recordManager.numRecords(Tag.ACTIVE) < MyceliumWalletApi.MAXIMUM_ADDRESSES_PER_REQUEST);
   }

   private Button createAddButton(ViewGroup parent) {
      Button add = (Button) _layoutInflater.inflate(R.layout.records_add_button, parent, false);
      add.setOnClickListener(new AddClicked());
      return add;
   }

   private LinearLayout createRecordViewList(int titleResource, List<Record> records, Record selectedRecord,
         boolean addButton) {
      LinearLayout outer = new LinearLayout(this);
      outer.setOrientation(LinearLayout.VERTICAL);
      outer.setLayoutParams(_outerLayoutParameters);
      outer.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black));

      // Add title
      if (addButton) {
         // Add both a title and an "+" button
         LinearLayout titleLayout = new LinearLayout(this);
         titleLayout.setOrientation(LinearLayout.HORIZONTAL);
         titleLayout.setLayoutParams(_innerLayoutParameters);
         titleLayout.addView(createTitle(titleResource));
         titleLayout.addView(createAddButton(titleLayout));
         outer.addView(titleLayout);
      } else {
         outer.addView(createTitle(titleResource));
      }

      if (records.isEmpty()) {
         return outer;
      }

      LinearLayout inner = new LinearLayout(this);
      inner.setOrientation(LinearLayout.VERTICAL);
      inner.setLayoutParams(_innerLayoutParameters);
      inner.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black_slim));
      inner.requestLayout();

      // Add records
      for (Record record : records) {
         // Add separator
         inner.addView(createSeparator());

         // Add item
         boolean isSelected = record.address.equals(selectedRecord.address);
         View item = recordToView(outer, record, isSelected);
         inner.addView(item);
      }
      outer.addView(inner);
      return outer;
   }

   private TextView createTitle(int stringResourceId) {
      TextView tv = new TextView(this);
      tv.setLayoutParams(_titleLayoutParameters);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
      tv.setText(stringResourceId);
      tv.setGravity(Gravity.LEFT);

      tv.setTextAppearance(this, R.style.GenericText);
      // tv.setBackgroundColor(getResources().getColor(R.color.darkgrey));
      return tv;
   }

   private View createSeparator() {
      View v = new View(this);
      v.setLayoutParams(_separatorLayoutParameters);
      v.setBackgroundColor(_separatorColor);
      v.setPadding(10, 0, 10, 0);
      return v;
   }

   private View recordToView(LinearLayout parent, Record record, boolean isSelected) {
      View rowView = _layoutInflater.inflate(R.layout.record_row, parent, false);

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
         displayAddress = record.address.toMultiLineString();
      } else {
         // Display address in short form
         displayAddress = getShortAddress(address);
      }
      ((TextView) rowView.findViewById(R.id.tvAddress)).setText(displayAddress);

      // Set tag
      rowView.setTag(record);

      // Show selected address with a different color
      if (isSelected) {
         rowView.setBackgroundColor(getResources().getColor(R.color.black));
         rowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_blue_slim));
      } else {
         rowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black_slim));
      }

      // Set balance
      if (record.tag == Tag.ACTIVE) {
         BalanceInfo balance = new Wallet(record).getLocalBalance(_mbwManager.getBlockChainAddressTracker());
         if (balance.isKnown()) {
            rowView.findViewById(R.id.tvBalance).setVisibility(View.VISIBLE);
            String balanceString = _mbwManager.getBtcValueString(balance.unspent + balance.pendingChange);
            ((TextView) rowView.findViewById(R.id.tvBalance)).setText(balanceString);
         } else {
            // We don't show anything if we don't know the balance
            rowView.findViewById(R.id.tvBalance).setVisibility(View.INVISIBLE);
         }
      } else {
         // We don't show anything if we don't know the address is archived
         rowView.findViewById(R.id.tvBalance).setVisibility(View.INVISIBLE);
      }

      rowView.setOnClickListener(recordClickListener);
      rowView.setOnLongClickListener(recordLongClickListener);
      rowView.setLongClickable(false);
      registerForContextMenu(rowView);

      return rowView;

   }

   private OnClickListener recordClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         Record record = (Record) v.getTag();
         _recordManager.setSelectedRecord(record.address);
         update();
      }
   };

   private OnLongClickListener recordLongClickListener = new OnLongClickListener() {

      @Override
      public boolean onLongClick(View v) {
         Record record = (Record) v.getTag();
         _recordManager.setSelectedRecord(record.address);
         openContextMenu(v);
         update();
         return true;
      }
   };

   private String getShortAddress(String addressString) {
      StringBuilder sb = new StringBuilder();
      sb.append(addressString.substring(0, 6));
      sb.append("...");
      sb.append(addressString.substring(addressString.length() - 6));
      return sb.toString();
   }

   // @Override
   // public boolean dispatchTouchEvent(MotionEvent me) {
   // this._gestureFilter.onTouchEvent(me);
   // return super.dispatchTouchEvent(me);
   // }

   /** Called when menu button is pressed. */
   @Override
   public boolean onCreateOptionsMenu(Menu menu) {
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.record_options_menu, menu);
      return true;
   }

   @Override
   public boolean onPrepareOptionsMenu(Menu menu) {
      Record record = _recordManager.getSelectedRecord();

      // Set or Edit label?
      String label = _addressBook.getNameByAddress(record.address.toString());
      if (label == null || label.length() == 0) {
         menu.findItem(R.id.miSetLabel).setTitle(R.string.set_label);
      } else {
         menu.findItem(R.id.miSetLabel).setTitle(R.string.edit_label);
      }

      if (record.hasPrivateKey()) {
         menu.findItem(R.id.miDeleteAddress).setVisible(false);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(true);
         menu.findItem(R.id.miExport).setVisible(true);
      } else {
         menu.findItem(R.id.miDeleteAddress).setVisible(true);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(false);
         menu.findItem(R.id.miExport).setVisible(false);
      }

      if (record.tag == Tag.ACTIVE) {
         menu.findItem(R.id.miActivate).setVisible(false);
         menu.findItem(R.id.miArchive).setVisible(true);
      } else {
         MenuItem miActivate = menu.findItem(R.id.miActivate);
         // Only make visible if we are below the active size limit
         boolean setVisible = _recordManager.numRecords(Tag.ACTIVE) < MyceliumWalletApi.MAXIMUM_ADDRESSES_PER_REQUEST;
         miActivate.setVisible(setVisible);
         menu.findItem(R.id.miArchive).setVisible(false);
      }
      return super.onPrepareOptionsMenu(menu);
   }

   @Override
   public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
      super.onCreateContextMenu(menu, v, menuInfo);
      MenuInflater inflater = getMenuInflater();
      inflater.inflate(R.menu.records_context_menu, menu);
      Record record = _recordManager.getSelectedRecord();

      // Set or Edit label?
      String label = _addressBook.getNameByAddress(record.address.toString());
      if (label == null || label.length() == 0) {
         menu.findItem(R.id.miSetLabel).setTitle(R.string.set_label);
      } else {
         menu.findItem(R.id.miSetLabel).setTitle(R.string.edit_label);
      }

      if (record.hasPrivateKey()) {
         menu.findItem(R.id.miDeleteAddress).setVisible(false);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(true);
         menu.findItem(R.id.miExport).setVisible(true);
      } else {
         menu.findItem(R.id.miDeleteAddress).setVisible(true);
         menu.findItem(R.id.miDeletePrivateKey).setVisible(false);
         menu.findItem(R.id.miExport).setVisible(false);
      }

      if (record.tag == Tag.ACTIVE) {
         menu.findItem(R.id.miActivate).setVisible(false);
         menu.findItem(R.id.miArchive).setVisible(true);
      } else {
         MenuItem miActivate = menu.findItem(R.id.miActivate);
         miActivate.setVisible(true);
         // Only enable if we are below the active size limit
         boolean setEnabled = _recordManager.numRecords(Tag.ACTIVE) < MyceliumWalletApi.MAXIMUM_ADDRESSES_PER_REQUEST;
         miActivate.setEnabled(setEnabled);
         menu.findItem(R.id.miArchive).setVisible(false);
      }
   }

   @Override
   public boolean onContextItemSelected(final MenuItem item) {
      if (item.getItemId() == R.id.miSetLabel) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedSetLabel);
         return true;
      } else if (item.getItemId() == R.id.miDeleteAddress || item.getItemId() == R.id.miDeletePrivateKey) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedDeleteRecord);
         return true;
      } else if (item.getItemId() == R.id.miExport) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedExport);
         return true;
      } else if (item.getItemId() == R.id.miArchive) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedArchive);
         return true;
      } else if (item.getItemId() == R.id.miActivate) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedActivate);
         return true;
      } else {
         return false;
      }
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (item.getItemId() == R.id.miSetLabel) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedSetLabel);
         return true;
      } else if (item.getItemId() == R.id.miDeleteAddress || item.getItemId() == R.id.miDeletePrivateKey) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedDeleteRecord);
         return true;
      } else if (item.getItemId() == R.id.miExport) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedExport);
         return true;
      } else if (item.getItemId() == R.id.miArchive) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedArchive);
         return true;
      } else if (item.getItemId() == R.id.miActivate) {
         _mbwManager.runPinProtectedFunction(this, pinProtectedActivate);
         return true;
      }
      return super.onOptionsItemSelected(item);
   }

   final Runnable pinProtectedSetLabel = new Runnable() {

      @Override
      public void run() {
         Utils.showSetAddressLabelDialog(RecordsActivity.this, _addressBook,
               _recordManager.getSelectedRecord().address.toString(), updateAfterSetLabel);
      }
   };

   final Runnable updateAfterSetLabel = new Runnable() {

      @Override
      public void run() {
         update();
      }
   };

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
         Utils.exportPrivateKey(record, RecordsActivity.this);
      }
   };

   final Runnable pinProtectedActivate = new Runnable() {

      @Override
      public void run() {
         _recordManager.activateRecordByAddress(_recordManager.getSelectedRecord().address);
         update();
         Toast.makeText(RecordsActivity.this, R.string.activated, Toast.LENGTH_LONG).show();
      }
   };

   final Runnable pinProtectedArchive = new Runnable() {

      @Override
      public void run() {

         AlertDialog.Builder confirmDialog = new AlertDialog.Builder(RecordsActivity.this);
         String title = "Archiving Key";
         confirmDialog.setTitle(title);
         confirmDialog.setMessage(getString(R.string.question_archive));
         confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
               _recordManager.archiveRecordByAddress(_recordManager.getSelectedRecord().address);
               update();
               Toast.makeText(RecordsActivity.this, R.string.archived, Toast.LENGTH_LONG).show();
            }
         });
         confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface arg0, int arg1) {
            }
         });
         confirmDialog.show();
      }
   };

}
