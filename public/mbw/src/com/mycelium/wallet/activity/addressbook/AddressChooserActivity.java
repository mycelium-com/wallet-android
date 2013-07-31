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

import java.util.List;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.mrd.bitlib.util.StringUtils;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wallet.AddressBookManager.Entry;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

public class AddressChooserActivity extends ListActivity {

   public static final String ADDRESS_RESULT_NAME = "address result";
   private ListView lvAdressList;
   private MbwManager _mbwManager;
   private AddressBookManager _addressBook;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.address_chooser_activity);
      _mbwManager = MbwManager.getInstance(this.getApplication());
      _addressBook = _mbwManager.getAddressBookManager();
      lvAdressList = (ListView) findViewById(android.R.id.list);
      lvAdressList.setOnItemClickListener(new OnItemClickListener() {

         @Override
         public void onItemClick(AdapterView<?> arg0, View view, int position, long id) {
            String value = (String) view.getTag();
            Intent result = new Intent();
            result.putExtra(ADDRESS_RESULT_NAME, value);
            setResult(RESULT_OK, result);
            finish();
         }
      });
   }

   @Override
   public void onResume() {
      super.onResume();
      List<Entry> entries = _addressBook.getEntries();
      setListAdapter(new AddressBookAdapter(this, R.layout.address_book_row, entries));
   }

   private class AddressBookAdapter extends ArrayAdapter<Entry> {

      public AddressBookAdapter(Context context, int textViewResourceId, List<Entry> objects) {
         super(context, textViewResourceId, objects);
      }

      @Override
      public View getView(int position, View convertView, ViewGroup parent) {
         View v = convertView;

         if (v == null) {
            LayoutInflater vi = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            v = vi.inflate(R.layout.address_book_row, null);
         }
         TextView tvName = (TextView) v.findViewById(R.id.address_book_name);
         TextView tvAddress = (TextView) v.findViewById(R.id.address_book_address);
         Entry e = getItem(position);
         tvName.setText(e.getName());
         tvAddress.setText(formatAddress(e.getAddress()));
         v.setTag(e.getAddress());
         return v;
      }

   }

   private static String formatAddress(String address) {
      return StringUtils.join(Utils.stringChopper(address, 12), "\r\n");
   }

}
