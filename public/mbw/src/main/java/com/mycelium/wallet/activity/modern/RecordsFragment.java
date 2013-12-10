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

package com.mycelium.wallet.activity.modern;

import java.util.Date;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.view.ActionMode.Callback;
import android.text.format.DateFormat;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.google.common.collect.Lists;
import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.MyceliumWalletApi;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.BalanceInfo;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.RecordManager;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.Wallet;
import com.mycelium.wallet.WalletMode;
import com.mycelium.wallet.activity.AddRecordActivity;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil;
import com.mycelium.wallet.activity.util.EnterAddressLabelUtil.AddressLabelChangedHandler;
import com.mycelium.wallet.event.BlockchainReady;
import com.mycelium.wallet.event.SelectedRecordChanged;
import com.squareup.otto.Subscribe;

public class RecordsFragment extends Fragment {

   public static final int ADD_RECORD_RESULT_CODE = 0;

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
   private Record _focusedRecord;
   private Toaster _toaster;

   /**
    * Called when the activity is first created.
    */
   @SuppressWarnings("deprecation")
   @Override
   public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
      View ret = inflater.inflate(R.layout.records_activity, container, false);
      _layoutInflater = inflater;

      _separatorColor = getResources().getColor(R.color.darkgrey);
      _separatorLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, getDipValue(1), 1);
      _titleLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      _outerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      _outerLayoutParameters.bottomMargin = getDipValue(8);
      _innerLayoutParameters = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT, 1);
      return ret;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setHasOptionsMenu(true);
   }

   @Override
   public void onAttach(Activity activity) {
      _mbwManager = MbwManager.getInstance(activity);
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();
      _toaster = new Toaster(this);
      super.onAttach(activity);
   }

   @Override
   public void onResume() {
      _mbwManager.getEventBus().register(this);
      getView().findViewById(R.id.btUnlock).setOnClickListener(unlockClickedListener);
      update();
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

   @Override
   public void onDestroy() {
      if (_dialog != null && _dialog.isShowing()) {
         _dialog.dismiss();
      }
      super.onDestroy();
   }

   private int getDipValue(int dip) {
      return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dip, getResources().getDisplayMetrics());
   }

   @Override
   public void setUserVisibleHint(boolean isVisibleToUser) {
      super.setUserVisibleHint(isVisibleToUser);
      if (!isVisibleToUser) {
         finishCurrentActionMode();
      }
   }

   @Override
   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      // after adding a key, remove add key button eventually if limit is hit
      ActivityCompat.invalidateOptionsMenu(getActivity());
      if (requestCode == ADD_RECORD_RESULT_CODE && resultCode == Activity.RESULT_OK) {
         Record record = (Record) intent.getSerializableExtra(AddRecordActivity.RESULT_KEY);
         addRecord(record);
      } else {
         super.onActivityResult(requestCode, resultCode, intent);
      }
   }

   private void deleteRecord(final Record record) {
      if (record == null) {
         return;
      }
      final View checkBoxView = View.inflate(getActivity(), R.layout.delkey_checkbox, null);
      final CheckBox keepAddrCheckbox = (CheckBox) checkBoxView.findViewById(R.id.checkbox);
      keepAddrCheckbox.setText(getString(R.string.keep_address));
      keepAddrCheckbox.setChecked(false);

      final AlertDialog.Builder deleteDialog = new AlertDialog.Builder(getActivity());
      deleteDialog.setTitle(record.hasPrivateKey() ? R.string.delete_private_key_title : R.string.delete_address_title);
      deleteDialog.setMessage(record.hasPrivateKey() ? R.string.delete_private_key_message
            : R.string.delete_address_message);

      if (record.hasPrivateKey()) { // add checkbox only if private key is
         // present
         deleteDialog.setView(checkBoxView);
      }

      deleteDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
            if (record.hasPrivateKey()) {
               Long satoshis = getPotentialBalance(record);
               AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
               confirmDialog.setTitle(R.string.confirm_delete_pk_title);

               // Set the message. There are four combinations, with and without
               // label, with and without BTC amount.
               String label = _addressBook.getNameByAddress(record.address.toString());
               String message;
               if (record.tag == Tag.ACTIVE && satoshis != null && satoshis > 0) {
                  if (label != null && label.length() != 0) {
                     message = getString(R.string.confirm_delete_pk_with_balance_with_label, label,
                           record.address.toMultiLineString(), _mbwManager.getBtcValueString(satoshis));
                  } else {
                     message = getString(R.string.confirm_delete_pk_with_balance, record.address.toMultiLineString(),
                           _mbwManager.getBtcValueString(satoshis));
                  }
               } else {
                  if (label != null && label.length() != 0) {
                     message = getString(R.string.confirm_delete_pk_without_balance_with_label, label,
                           record.address.toMultiLineString());
                  } else {
                     message = getString(R.string.confirm_delete_pk_without_balance, record.address.toMultiLineString());
                  }
               }
               confirmDialog.setMessage(message);

               confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                     if (keepAddrCheckbox.isChecked()) {
                        _recordManager.forgetPrivateKeyForRecordByAddress(record.address);
                     } else {
                        _recordManager.deleteRecord(record.address);
                        _addressBook.deleteEntry(record.address.toString());
                     }
                     finishCurrentActionMode();
                     update();
                     _toaster.toast(R.string.private_key_deleted, false);
                  }
               });
               confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

                  public void onClick(DialogInterface arg0, int arg1) {
                  }
               });
               confirmDialog.show();
            } else {
               _recordManager.deleteRecord(record.address);
               _addressBook.deleteEntry(record.address.toString());
               finishCurrentActionMode();
               update();
               _toaster.toast(R.string.bitcoin_address_deleted, false);
            }
         }

         private Long getPotentialBalance(Record record) {
            if (record.tag == Tag.ACTIVE) {
               BalanceInfo balance = new Wallet(record).getLocalBalance(_mbwManager.getBlockChainAddressTracker());
               if (balance.isKnown()) {
                  return balance.unspent + balance.pendingChange + balance.pendingReceiving;
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }

      });
      deleteDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
         }
      });
      deleteDialog.show();

   }

   private void finishCurrentActionMode() {
      if (currentActionMode != null) {
         currentActionMode.finish();
      }
   }

   private boolean addRecord(Record record) {
      Record existing = _recordManager.getRecord(record.address);

      if (existing == null) {
         // We have a new record
         if (record.hasPrivateKey()) {
            _toaster.toast(R.string.imported_private_key, false);
         } else {
            _toaster.toast(R.string.imported_bitcoin_address, false);
         }
         _recordManager.addRecord(record);
         _recordManager.setSelectedRecord(record.address);
         update();
         setNameForNewRecord(record);
         return true;
      }

      // The record has the same address as one we have already
      if (existing.hasPrivateKey() && record.hasPrivateKey()) {
         // Nothing to do as we already have the record with the private key
         _toaster.toast(R.string.key_already_present, false);
         return false;
      } else if (!existing.hasPrivateKey() && !record.hasPrivateKey()) {
         _toaster.toast(R.string.address_already_present, false);
         // Nothing to do as none of the records have the private key
         return false;
      } else if (!existing.hasPrivateKey() && record.hasPrivateKey()) {
         // We upgrade to a record with a private key
         _toaster.toast(R.string.upgraded_address_to_key, false);
         _recordManager.addRecord(record);
         _recordManager.setSelectedRecord(record.address);
         update();
         return true;
      } else if (existing.hasPrivateKey() && !record.hasPrivateKey()) {
         _toaster.toast(R.string.address_already_present, false);
         // The new record does not have a private key, do nothing as we do not
         // do downgrades
         return false;
      }

      // We never get here
      return false;
   }

   private void setNameForNewRecord(Record record) {
      if (record == null || !isAdded()) {
         return;
      }

      // Determine a default name from the current date but in such a way that
      // it does not collide with any name we have already by adding a counter
      // to the name if necessary
      String baseName = DateFormat.getMediumDateFormat(this.getActivity()).format(new Date());
      String defaultName = baseName;
      int num = 1;
      while (_addressBook.getAddressByName(defaultName) != null) {
         defaultName = baseName + " (" + num++ + ')';
      }
      setLabelOnKey(record.address, defaultName);
   }

   private void update() {
      if (!isAdded()) {
         return;
      }
      LinearLayout llRecords = (LinearLayout) getView().findViewById(R.id.llRecords);
      llRecords.removeAllViews();

      if (!_mbwManager.getExpertMode()) {
         // Hide all the key management functionality from non-experts
         getView().findViewById(R.id.svRecords).setVisibility(View.GONE);
         getView().findViewById(R.id.tvExpertModeNeeded).setVisibility(View.VISIBLE);
         getView().findViewById(R.id.llLocked).setVisibility(View.GONE);
      } else if (_mbwManager.isKeyManagementLocked()) {
         // Key management is locked
         getView().findViewById(R.id.svRecords).setVisibility(View.GONE);
         getView().findViewById(R.id.tvExpertModeNeeded).setVisibility(View.GONE);
         getView().findViewById(R.id.llLocked).setVisibility(View.VISIBLE);
      } else {
         // Make all the key management functionality available to experts
         getView().findViewById(R.id.svRecords).setVisibility(View.VISIBLE);
         getView().findViewById(R.id.tvExpertModeNeeded).setVisibility(View.GONE);
         getView().findViewById(R.id.llLocked).setVisibility(View.GONE);

         List<Record> activeRecords = _recordManager.getRecords(Tag.ACTIVE);
         List<Record> archivedRecords = _recordManager.getRecords(Tag.ARCHIVE);
         Record selectedRecord = _recordManager.getSelectedRecord();
         LinearLayout active = createRecordViewList(activeRecords.isEmpty() ? R.string.active_name_empty : R.string.active_name,
               activeRecords, selectedRecord, true);
         llRecords.addView(active);
         llRecords.addView(createRecordViewList(archivedRecords.isEmpty() ? R.string.archive_name_empty : R.string.archive_name,
               archivedRecords, selectedRecord, false));
      }
   }

   private LinearLayout createRecordViewList(int titleResource, List<Record> records, Record selectedRecord,
         boolean addButton) {
      LinearLayout outer = new LinearLayout(getActivity());
      outer.setOrientation(LinearLayout.VERTICAL);
      outer.setLayoutParams(_outerLayoutParameters);

      // Add title
      if (addButton) {
         // Add both a title and an "+" button
         LinearLayout titleLayout = new LinearLayout(getActivity());
         titleLayout.setOrientation(LinearLayout.HORIZONTAL);
         titleLayout.setLayoutParams(_innerLayoutParameters);
         titleLayout.addView(createTitle(titleResource));
         outer.addView(titleLayout);
      } else {
         outer.addView(createTitle(titleResource));
      }

      if (records.isEmpty()) {
         return outer;
      }

      LinearLayout inner = new LinearLayout(getActivity());
      inner.setOrientation(LinearLayout.VERTICAL);
      inner.setLayoutParams(_innerLayoutParameters);
      // inner.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black_slim));
      inner.requestLayout();

      // build only once since it is relatively slow
      final Set<Address> addressSet = _mbwManager.getRecordManager().getWallet(_mbwManager.getWalletMode())
            .getAddressSet();
      // Add records
      for (Record record : records) {
         // Add separator
         inner.addView(createSeparator());

         // Add item
         boolean isSelected = record.address.equals(selectedRecord.address);
         View item = createRecord(outer, record, isSelected, addressSet);
         inner.addView(item);
      }
      
      if(records.size() > 0){
         // Add separator
         inner.addView(createSeparator());
      }
      
      outer.addView(inner);
      return outer;
   }

   private TextView createTitle(int stringResourceId) {
      TextView tv = new TextView(getActivity());
      tv.setLayoutParams(_titleLayoutParameters);
      tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 30);
      tv.setText(stringResourceId);
      tv.setGravity(Gravity.LEFT);

      tv.setTextAppearance(getActivity(), R.style.GenericText);
      // tv.setBackgroundColor(getResources().getColor(R.color.darkgrey));
      return tv;
   }

   private View createSeparator() {
      View v = new View(getActivity());
      v.setLayoutParams(_separatorLayoutParameters);
      v.setBackgroundColor(_separatorColor);
      v.setPadding(10, 0, 10, 0);
      return v;
   }

   private View createRecord(LinearLayout parent, Record record, boolean isSelected, Set<Address> addressSet) {
      boolean hasFocus = _focusedRecord != null && record.address.equals(_focusedRecord.address);
      View rowView = RecordRowBuilder.buildRecordView(getResources(), _mbwManager, _layoutInflater, _addressBook,
            parent, record, isSelected, hasFocus, addressSet);
      rowView.setOnClickListener(recordStarClickListener);
      rowView.findViewById(R.id.llAddress).setOnClickListener(recordAddressClickListener);
      return rowView;
   }

   private OnClickListener recordStarClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {

         Record record = (Record) v.getTag();
         _recordManager.setSelectedRecord(record.address);
         update();
      }
   };

   private ActionMode currentActionMode;

   private OnClickListener recordAddressClickListener = new OnClickListener() {

      @Override
      public void onClick(View v) {

         final Record record = (Record) ((View) v.getParent()).getTag();

         // Check whether a new record was selected
         if (!_recordManager.getSelectedRecord().equals(record)) {
            _recordManager.setSelectedRecord(record.address);
            toastSelectedRecordChanged(record);
         }

         final List<Integer> menus = Lists.newArrayList();
         menus.add(R.menu.record_options_menu);

         if (record.tag != Tag.ARCHIVE) {
            menus.add(R.menu.record_options_menu_active);
         }
         if (record.tag != Tag.ACTIVE) {
            menus.add(R.menu.record_options_menu_archive);
         }
         if (record.hasPrivateKey()) {
            menus.add(R.menu.record_options_menu_privkey);
         }

         ActionBarActivity parent = (ActionBarActivity) getActivity();

         Callback actionMode = new ActionMode.Callback() {
            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
               for (Integer res : menus) {
                  actionMode.getMenuInflater().inflate(res, menu);
               }
               return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
               return true;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
               int id = menuItem.getItemId();
               if (id == R.id.miActivate) {
                  activateSelected();
                  return true;
               } else if (id == R.id.miSetLabel) {
                  setLabelOnKey(_recordManager.getSelectedRecord().address, "");
                  return true;
               } else if (id == R.id.miDeleteRecord) {
                  deleteSelected();
                  return true;
               } else if (id == R.id.miArchive) {
                  archiveSelected();
                  return true;
               } else if (id == R.id.miExport) {
                  exportSelectedPrivateKey();
                  return true;
               }
               return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {
               currentActionMode = null;
               // Loose focus
               if (_focusedRecord != null) {
                  _focusedRecord = null;
                  update();
               }
            }
         };
         currentActionMode = parent.startSupportActionMode(actionMode);
         // Late set the focused record. We have to do this after
         // startSupportActionMode above, as it calls onDestroyActionMode when
         // starting for some reason, and this would clear the focus and force
         // an update.
         _focusedRecord = record;
         update();

      }

   };

   /**
    * Show a message to the user explaining what it means to select a different
    * address.
    */
   private void toastSelectedRecordChanged(Record record) {
      if (record.tag == Tag.ARCHIVE) {
         _toaster.toast("You are now working on this single archived address", true);
      } else if (_mbwManager.getWalletMode() == WalletMode.Aggregated) {
         _toaster.toast("New receiving address selected", true);
      } else {
         _toaster.toast("You are now working on this single address", true);
      }
   }

   @Override
   public boolean onOptionsItemSelected(MenuItem item) {
      if (!isAdded()) {
         return true;
      }
      if (item.getItemId() == R.id.miAddRecord) {
         if (checkActiveLimitOrWarn()) {
            AddRecordActivity.callMe(this, ADD_RECORD_RESULT_CODE);
         }
         return true;
      } else if (item.getItemId() == R.id.miLockKeys) {
         lock();
         return true;
      }

      return super.onOptionsItemSelected(item);
   }

   private boolean checkActiveLimitOrWarn() {
      boolean canAddMore = _recordManager.numRecords(Tag.ACTIVE) < MyceliumWalletApi.MAXIMUM_ADDRESSES_PER_REQUEST;
      if (!canAddMore) {
         if (!isAdded()) {
            return canAddMore;
         }
         Utils.showSimpleMessageDialog(this.getActivity(), R.string.active_key_limit_reached);
      }
      return canAddMore;
   }

   private void setLabelOnKey(final Address address, final String defaultName) {
      if (!RecordsFragment.this.isAdded()) {
         return;
      }
      if (_addressBook.hasAddress(address.toString())) {
         _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

            @Override
            public void run() {
               if (!RecordsFragment.this.isAdded()) {
                  return;
               }
               EnterAddressLabelUtil.enterAddressLabel(getActivity(), _addressBook, address.toString(), defaultName,
                     addressLabelChanged);
            }

         });
      } else {
         EnterAddressLabelUtil.enterAddressLabel(getActivity(), _addressBook, address.toString(), defaultName,
               addressLabelChanged);
      }
   }

   private void deleteSelected() {
      if (!RecordsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!RecordsFragment.this.isAdded()) {
               return;
            }
            deleteRecord(_recordManager.getSelectedRecord());
         }

      });
   }

   private AddressLabelChangedHandler addressLabelChanged = new AddressLabelChangedHandler() {

      @Override
      public void OnAddressLabelChanged(String address, String label) {
         update();
      }
   };

   private void exportSelectedPrivateKey() {
      if (!RecordsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!RecordsFragment.this.isAdded()) {
               return;
            }
            Utils.exportSelectedPrivateKey(RecordsFragment.this.getActivity());
         }

      });
   }

   private void activateSelected() {
      if (!RecordsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!RecordsFragment.this.isAdded()) {
               return;
            }
            Record record = _recordManager.getSelectedRecord();
            activate(record);
         }

      });
   }

   private void activate(Record record) {
      if (checkActiveLimitOrWarn()) {
         _recordManager.activateRecordByAddress(record.address);
         update();
         _toaster.toast(R.string.activated, false);
      }
   }

   private void archiveSelected() {
      if (!RecordsFragment.this.isAdded()) {
         return;
      }
      _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

         @Override
         public void run() {
            if (!RecordsFragment.this.isAdded()) {
               return;
            }
            Record record = _recordManager.getSelectedRecord();
            archive(record.address);
         }

      });
   }

   private void archive(final Address address) {

      AlertDialog.Builder confirmDialog = new AlertDialog.Builder(getActivity());
      confirmDialog.setTitle(R.string.archiving_key_title);
      confirmDialog.setMessage(getString(R.string.question_archive));
      confirmDialog.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
            _recordManager.archiveRecordByAddress(address);
            update();
            _toaster.toast(R.string.archived, false);
         }
      });
      confirmDialog.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {

         public void onClick(DialogInterface arg0, int arg1) {
         }
      });
      confirmDialog.show();
   }

   private void lock() {
      _mbwManager.setKeyManagementLocked(true);
      update();
      if (isAdded()) {
         getActivity().supportInvalidateOptionsMenu();
      }
   }

   OnClickListener unlockClickedListener = new OnClickListener() {

      @Override
      public void onClick(View v) {
         _mbwManager.runPinProtectedFunction(RecordsFragment.this.getActivity(), new Runnable() {

            @Override
            public void run() {
               _mbwManager.setKeyManagementLocked(false);
               update();
               if (isAdded()) {
                  getActivity().supportInvalidateOptionsMenu();
               }
            }

         });
      }
   };

   @Subscribe
   public void blockChainReady(BlockchainReady event) {
      update();
   }

   @Subscribe
   public void modeChanged(SelectedRecordChanged changed) {
      update();
   }

}
