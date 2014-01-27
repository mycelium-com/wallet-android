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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.*;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.activity.modern.RecordRowBuilder;

import java.util.List;
import java.util.Set;

public class GetSpendingRecordActivity extends Activity {

   private Long _amountToSend;
   private Address _receivingAddress;
   private RecordManager _recordManager;
   private RecordsAdapter _recordsAdapter;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;

   public static void callMeWithResult(Activity currentActivity, Long amountToSend, Address receivingAddress, int request) {
      Intent intent = new Intent(currentActivity, GetSpendingRecordActivity.class);
      intent.putExtra("amountToSend", amountToSend);
      intent.putExtra("receivingAddress", receivingAddress);
      currentActivity.startActivityForResult(intent, request);
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      this.requestWindowFeature(Window.FEATURE_NO_TITLE);
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_spending_record_activity);
      ((ListView) findViewById(R.id.lvRecords)).setOnItemClickListener(new RecordClicked());
      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();

      // Get intent parameters
      // May be null
      _amountToSend = (Long) getIntent().getSerializableExtra("amountToSend");
      // May be null
      _receivingAddress = (Address) getIntent().getSerializableExtra("receivingAddress");

   }

   class RecordClicked implements OnItemClickListener {

      @Override
      public void onItemClick(AdapterView<?> list, View v, int position, long id) {
         if (v.getTag() == null || !(v.getTag() instanceof Record)) {
            return;
         }
         Record record = (Record) v.getTag();
         Wallet wallet = new Wallet(record);
         SendInitializationActivity.callMe(GetSpendingRecordActivity.this, wallet, _amountToSend, _receivingAddress,
               false);
         GetSpendingRecordActivity.this.finish();
      }
   }

   @Override
   protected void onResume() {
      update();
      super.onResume();
   }

   private void update() {
      ListView listView = (ListView) findViewById(R.id.lvRecords);
      Set<Address> addressSet = _mbwManager.getRecordManager().getWallet(_mbwManager.getWalletMode()).getAddressSet();
      _recordsAdapter = new RecordsAdapter(this, _recordManager.getRecordsWithPrivateKeys(Tag.ACTIVE), addressSet);
      listView.setAdapter(_recordsAdapter);
   }

   class RecordsAdapter extends ArrayAdapter<Record> {
      private Context _context;
      private final Set<Address> addressSet;

      public RecordsAdapter(Context context, List<Record> records, Set<Address> addressSet) {
         super(context, R.layout.record_row, records);
         _context = context;
         this.addressSet = addressSet;
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         LayoutInflater inflater = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         Record record = getItem(position);
         return RecordRowBuilder.buildRecordView(getResources(), _mbwManager, inflater, _addressBook, parent, record,
               false, false, addressSet);
      }
   }

}