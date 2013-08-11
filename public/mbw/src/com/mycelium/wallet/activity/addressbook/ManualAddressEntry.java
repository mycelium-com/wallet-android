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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import com.mrd.bitlib.model.Address;
import com.mrd.mbwapi.api.AddressShortResult;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

public class ManualAddressEntry extends Activity {

   private final MbwManager _manager;

   public ManualAddressEntry() {
      _manager = MbwManager.getInstance(getApplication());

   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.manual_entry);
      final AutoCompleteTextView userEntry = (AutoCompleteTextView) findViewById(R.id.userEntry);
      userEntry.setAdapter(new AddressFilter(this));

      findViewById(R.id.button).setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View view) {
            Intent result = new Intent();
            result.putExtra("base58key", userEntry.getText().toString());
            ManualAddressEntry.this.setResult(RESULT_OK, result);
            ManualAddressEntry.this.finish();
         }
      });

   }

   private class AddressFilter extends ArrayAdapter<Address> implements Filterable {

      private LayoutInflater mInflater;

      public AddressFilter(final Context context) {
         super(context, -1);
         mInflater = LayoutInflater.from(context);

      }

      @Override
      public View getView(final int position, final View convertView, final ViewGroup parent) {
         final TextView tv;
         if (convertView != null) {
            tv = (TextView) convertView;
         } else {
            View view = mInflater.inflate(android.R.layout.simple_dropdown_item_1line, parent, false);
            tv = Preconditions.checkNotNull((TextView) view);
         }
         Address item = getItem(position);
         tv.setText(item.toString());
         return tv;
      }


      @Override
      public Filter getFilter() {
         return new Filter() {
            @Override
            protected FilterResults performFiltering(final CharSequence constraint) {
               final List<Address> addressList;
               if (constraint != null) {
                  AddressShortResult res = _manager.getAsyncApi().fromShortInput(constraint);
                  addressList = ImmutableList.copyOf(res.result.values());
               } else {
                  addressList = ImmutableList.of();
               }
               final FilterResults filterResults = new FilterResults();
               filterResults.values = addressList;
               filterResults.count = addressList.size();

               return filterResults;
            }

            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(final CharSequence contraint, final FilterResults results) {
               clear();
               List<Address> res = (List<Address>) Preconditions.checkNotNull(results.values);
               for (Address address : res) {
                  add(address);
               }
               if (results.count > 0) {
                  notifyDataSetChanged();
               } else {
                  notifyDataSetInvalidated();
               }
            }

            @Override
            public CharSequence convertResultToString(final Object resultValue) {
               return resultValue == null ? "" : resultValue.toString();
            }
         };
      }
   }
}
