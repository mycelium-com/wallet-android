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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.wallet.GpsLocationFetcher;
import com.mycelium.wallet.GpsLocationFetcher.GpsLocationEx;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.lt.LocalTraderManager;

public class ChangeLocationActivity extends Activity {
   protected static final int ENTER_LOCATION_REQUEST_CODE = 0;

   public static void callMe(Activity currentActivity) {
      Intent intent = new Intent(currentActivity, ChangeLocationActivity.class)
              .putExtra("persist", true);
      currentActivity.startActivity(intent);
   }

   public static void callMeForResult(Activity currentActivity, GpsLocation location, boolean persist, int requestCode) {
      Intent intent = new Intent(currentActivity, ChangeLocationActivity.class)
              .putExtra("persist", persist)
              .putExtra("location", location);
      currentActivity.startActivityForResult(intent, requestCode);
   }

   private MbwManager _mbwManager;
   private GpsLocationEx _chosenAddress;
   private Button _btUse;
   private TextView _tvLocation;
   private GpsLocationFetcher.Callback _gpsLocationCallback;
   private boolean _persist;

   /**
    * Called when the activity is first created.
    */
   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.lt_change_location_activity);
      _mbwManager = MbwManager.getInstance(this);
      LocalTraderManager ltManager = _mbwManager.getLocalTraderManager();

      _btUse = findViewById(R.id.btUse);
      _tvLocation = findViewById(R.id.tvLocation);

      // Load intent parameters
      _persist = getIntent().getBooleanExtra("persist", false);
      _chosenAddress = GpsLocationEx.fromGpsLocation((GpsLocation) getIntent().getSerializableExtra("location"));

      // Load saved state
      if (savedInstanceState != null) {
         _chosenAddress = (GpsLocationEx) savedInstanceState.getSerializable("location");
      }

      if (_chosenAddress == null) {
         _chosenAddress = ltManager.getUserLocation();
      }

      _btUse.setOnClickListener(useClickListener);
      findViewById(R.id.btEnter).setOnClickListener(enterClickListener);

      findViewById(R.id.btCrosshair).setOnClickListener(crossHairClickListener);

      _gpsLocationCallback = new GpsLocationFetcher.Callback(this) {
         @Override
         protected void onGpsLocationObtained(GpsLocationEx location) {
            TextView tvError = findViewById(R.id.tvError);
            tvError.setVisibility(View.VISIBLE);

            if (location != null) {
               _chosenAddress = location;
               updateUi();
            } else {
               Toast.makeText(ChangeLocationActivity.this, R.string.lt_localization_not_available, Toast.LENGTH_LONG)
                     .show();
            }
         }
      };
   }

   protected void onSaveInstanceState(Bundle outState) {
      outState.putSerializable("location", _chosenAddress);
   }

   OnClickListener crossHairClickListener = new OnClickListener() {
      @Override
      public void onClick(View arg0) {
         if(!Utils.hasOrRequestLocationAccess(ChangeLocationActivity.this)) {
            return;
         }
         _chosenAddress = null;
         updateUi();
         new GpsLocationFetcher().getNetworkLocation(_gpsLocationCallback);
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

   OnClickListener enterClickListener = new OnClickListener() {

      @Override
      public void onClick(View arg0) {
         EnterLocationActivity.callMeForResult(ChangeLocationActivity.this, ENTER_LOCATION_REQUEST_CODE);
      }
   };

   @Override
   protected void onResume() {
      updateUi();
      super.onResume();
   }

   private void updateUi() {
      if (_chosenAddress != null) {
         _tvLocation.setText(_chosenAddress.name);
      } else {
         _tvLocation.setText("");
      }
      _btUse.setEnabled(_chosenAddress != null);

   }

   public void onActivityResult(final int requestCode, final int resultCode, final Intent intent) {
      if (requestCode == ENTER_LOCATION_REQUEST_CODE && resultCode == RESULT_OK) {
         _chosenAddress = (GpsLocationEx) intent.getSerializableExtra("location");
      }
      // else  We didn't like what we got, bail...
   }
}
