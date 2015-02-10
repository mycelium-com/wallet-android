/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mycelium.wallet.lt.activity;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.google.common.collect.ImmutableList;
import com.mycelium.lt.location.Geocode;
import com.mycelium.lt.location.Geocoder;
import com.mycelium.lt.location.GoogleMapsGeocoder;
import com.mycelium.lt.location.RemoteGeocodeException;
import com.mycelium.wallet.GpsLocationFetcher.GpsLocationEx;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.DelayAutoCompleteTextView;
import com.mycelium.wallet.lt.AddressDescription;
import com.mycelium.wallet.lt.BackendGeocoder;
import com.mycelium.wallet.lt.LocalTraderManager;

public class EnterLocationActivity extends Activity {

   private String language;
   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private Geocoder _geocoder;

   public static void callMeForResult(Activity currentActivity, int requestCode) {
      Intent intent = new Intent(currentActivity, EnterLocationActivity.class);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private DelayAutoCompleteTextView _atvLocation;

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      _geocoder =  _ltManager.getGeocoder();

      setContentView(R.layout.lt_enter_location_activity);
      _atvLocation = (DelayAutoCompleteTextView) findViewById(R.id.atvLocation);

      _atvLocation.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.lt_location_item));
      _atvLocation.setThreshold(2);
      _atvLocation.setDelay(400);
      _atvLocation.setOnItemClickListener(atvLocationItemClickListener);

      // Load saved state
      if (savedInstanceState != null) {
         _atvLocation.setText(savedInstanceState.getString("location"));
      }

   }

   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("location", _atvLocation.getEditableText().toString());
   }

   OnItemClickListener atvLocationItemClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         AddressDescription itemAtPosition = (AddressDescription) parent.getItemAtPosition(position);
         Intent result = new Intent();
         result.putExtra("location", address2Location(itemAtPosition));
         setResult(RESULT_OK, result);
         finish();
      }

   };

   private GpsLocationEx address2Location(AddressDescription addr) {
      return new GpsLocationEx(addr.location.getLatitude(), addr.location.getLongitude(), addr.toString(),
            addr.location.getCountryCode());
   }

   private List<Geocode> autocompleteInternal(String input) {

      try {
         List<Geocode> ret = _geocoder.query(input, 5).results;
         hideGeocoderError();
         return ret;
      } catch (final RemoteGeocodeException ex) {
         // We tried hard, but we failed...
         showGeocoderError(ex);
         MbwManager.getInstance(this).reportIgnoredException("used geocoder:" + _geocoder.toString(), ex);
         return ImmutableList.of();
      }
   }

   private void hideGeocoderError() {
      runOnUiThread(new Runnable() {
         @Override
         public void run() {
            TextView tvError = (TextView) findViewById(R.id.tvError);
            tvError.setVisibility(View.GONE);
         }
      });
   }

   private void showGeocoderError(final RemoteGeocodeException ex) {
      runOnUiThread(new Runnable() {
         @Override
         public void run() {
            TextView tvError = (TextView) findViewById(R.id.tvError);
            tvError.setVisibility(View.VISIBLE);
            tvError.setText(String.format(getString(R.string.geocode_error), ex.status));
         }
      });
   }

   public class PlacesAutoCompleteAdapter extends ArrayAdapter<AddressDescription> implements Filterable {
      private List<Geocode> resultList;

      public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
         super(context, textViewResourceId);
      }


      @Override
      public int getCount() {
         return resultList == null ? 0 : resultList.size();
      }

      @Override
      public AddressDescription getItem(int index) {
         if (index < resultList.size() ) {
            return new AddressDescription(resultList.get(index));
         }else{
            return null;
         }
      }

      @Override
      public Filter getFilter() {
         return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
               FilterResults filterResults = new FilterResults();
               if (constraint != null) {
                  // Retrieve the auto-complete results.
                  resultList = autocompleteInternal(constraint.toString());

                  // Assign the data to the FilterResults
                  filterResults.values = resultList;
                  filterResults.count = resultList.size();
               } else{
                  resultList = new ArrayList<Geocode>();
               }
               return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
               if (results != null && results.count > 0) {
                  notifyDataSetChanged();
               } else {
                  notifyDataSetInvalidated();
               }
            }
         };
      }
   }

}

