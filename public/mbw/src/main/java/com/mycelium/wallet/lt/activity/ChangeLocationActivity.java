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

package com.mycelium.wallet.lt.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.Toast;

import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.location.Geocode;
import com.mycelium.lt.location.JsonCoder;
import com.mycelium.wallet.GpsLocationFetcher;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.lt.AddressDescription;
import com.mycelium.wallet.lt.LocalTraderManager;

import java.util.List;

public class ChangeLocationActivity extends Activity {

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, ChangeLocationActivity.class);
      intent.putExtra("persist", true);
      currentActivity.startActivity(intent);
   }

   public static void callMeForResult(Activity currentActivity, GpsLocation location, boolean persist, int requestCode) {
      Intent intent = new Intent(currentActivity, ChangeLocationActivity.class);
      intent.putExtra("persist", persist);
      intent.putExtra("location", location);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private LocalTraderManager _ltManager;
   private GpsLocation _chosenAddress;
   private Button _btUse;
   private AutoCompleteTextView _atvLocation;
   private GpsLocationFetcher.Callback _gpsLocationCallback;
   private boolean _persist;

   /** Called when the activity is first created. */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_change_location_activity);
      _mbwManager = MbwManager.getInstance(this);
      _ltManager = _mbwManager.getLocalTraderManager();

      // Load intent parameters
      _persist = getIntent().getBooleanExtra("persist", false);
      _chosenAddress = (GpsLocation) getIntent().getSerializableExtra("location");

      // Load saved state
      if (savedInstanceState != null) {
         _chosenAddress = (GpsLocation) savedInstanceState.getSerializable("location");
      }

      if (_chosenAddress == null) {
         _chosenAddress = _ltManager.getUserLocation();
      }

      _atvLocation = (AutoCompleteTextView) findViewById(R.id.atvLocation);
      _atvLocation.setText(_chosenAddress.name);
      _atvLocation.setAdapter(new PlacesAutoCompleteAdapter(this, R.layout.lt_location_item));
      _atvLocation.setOnItemClickListener(atvLocationItemClickListener);
      _atvLocation.setOnClickListener(atvLocationClickListener);

      _btUse = (Button) findViewById(R.id.btUse);

      _btUse.setOnClickListener(useClickListener);

      findViewById(R.id.btCrosshair).setOnClickListener(crossHairClickListener);

      _gpsLocationCallback = new GpsLocationFetcher.Callback(this) {

         @Override
         protected void onGpsLocationObtained(GpsLocation location) {
            if (location != null) {
               _atvLocation.setText(location.name);
               _chosenAddress = location;
               updateUseButton();
            } else {
               Toast.makeText(ChangeLocationActivity.this, R.string.lt_localization_not_available, Toast.LENGTH_LONG)
                     .show();
            }
         }
      };

   }

   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("location", _chosenAddress);
   };

   OnClickListener crossHairClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _atvLocation.setText("");
         _chosenAddress = null;
         GpsLocationFetcher.getNetworkLocation(_gpsLocationCallback);
         updateUseButton();
      }
   };

   OnClickListener useClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         if (_persist) {
            _mbwManager.getLocalTraderManager().setLocation(_chosenAddress);
         }
         Intent result = new Intent();
         result.putExtra("location", _chosenAddress);
         setResult(RESULT_OK, result);
         finish();
      }
   };

   @Override
   protected void onResume() {
      updateUseButton();
      super.onResume();
   }

   @Override
   protected void onPause() {
      super.onPause();
   }

   private void updateUseButton() {
      _btUse.setEnabled(_chosenAddress != null);
   }

   OnItemClickListener atvLocationItemClickListener = new AdapterView.OnItemClickListener() {
      @Override
      public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
         AddressDescription itemAtPosition = (AddressDescription) parent.getItemAtPosition(position);
         _chosenAddress = address2Location(itemAtPosition);
         _atvLocation.clearFocus();
         updateUseButton();
      }

   };

   OnClickListener atvLocationClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         _chosenAddress = null;
         _atvLocation.selectAll();
         updateUseButton();
      }
   };

   private GpsLocation address2Location(AddressDescription addr) {
      return new GpsLocation(addr.location.getLatitude(), addr.location.getLongitude(), addr.toString());
   }

   private List<Geocode> autocompleteInternal(String input) {
      JsonCoder geocoder = new JsonCoder();
      return geocoder.query(input, 5).results;
   }

   public class PlacesAutoCompleteAdapter extends ArrayAdapter<AddressDescription> implements Filterable {
      private List<Geocode> resultList;

      public PlacesAutoCompleteAdapter(Context context, int textViewResourceId) {
         super(context, textViewResourceId);
      }

      @Override
      public int getCount() {
         return resultList.size();
      }

      @Override
      public AddressDescription getItem(int index) {
         return new AddressDescription(resultList.get(index));
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