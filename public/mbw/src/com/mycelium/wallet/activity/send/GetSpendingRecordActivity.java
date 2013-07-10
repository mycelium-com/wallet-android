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

package com.mycelium.wallet.activity.send;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;

public class GetSpendingRecordActivity extends Activity {

   public static final int SCANNER_RESULT_CODE = 0;
   public static final int CREATE_RESULT_CODE = 1;

   private RecordManager _recordManager;
   private RecordsAdapter _recordsAdapter;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.get_spending_record_activity);
      ((ListView) findViewById(R.id.lvRecords)).setOnItemClickListener(new RecordClicked());
      _mbwManager = MbwManager.getInstance(this.getApplication());
      _recordManager = _mbwManager.getRecordManager();
      _addressBook = _mbwManager.getAddressBookManager();
   }

   class RecordClicked implements OnItemClickListener {

      @Override
      public void onItemClick(AdapterView<?> list, View v, int position, long id) {
         if (v.getTag() == null || !(v.getTag() instanceof Record)) {
            return;
         }
         Record record = (Record) v.getTag();
         SendActivityHelper.startNextActivity(GetSpendingRecordActivity.this, record);
      }
   }

   @Override
   protected void onResume() {
      update();
      super.onResume();
   }

   private void update() {
      ListView listView = (ListView) findViewById(R.id.lvRecords);
      _recordsAdapter = new RecordsAdapter(this, _recordManager.getRecordsWithPrivateKeys());
      listView.setAdapter(_recordsAdapter);
   }

   class RecordsAdapter extends ArrayAdapter<Record> {
      private Context _context;

      public RecordsAdapter(Context context, List<Record> records) {
         super(context, R.layout.record_row, records);
         _context = context;
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

         // Display address, chopping it into three
         String one = address.substring(0, 12);
         String two = address.substring(12, 24);
         String three = address.substring(24);
         address = one + "\r\n" + two + "\r\n" + three;
         ((TextView) rowView.findViewById(R.id.tvAddress)).setText(address);

         // Set tag
         rowView.setTag(record);

         rowView.setBackgroundDrawable(getResources().getDrawable(R.drawable.btn_pitch_black));
         return rowView;
      }
   }

}