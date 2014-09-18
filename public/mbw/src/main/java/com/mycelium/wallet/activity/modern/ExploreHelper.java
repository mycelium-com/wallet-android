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

package com.mycelium.wallet.activity.modern;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import com.mycelium.wallet.R;

import java.util.Locale;

public class ExploreHelper {

   Location lastLocation;
   private boolean wasRedirected;

   public ExploreHelper() {

   }


   public void redirectToCoinmap(final Activity  activity) {
      wasRedirected = false;

      if (lastLocation != null) {
         redirToLastLocation(activity);
         return;
      }

      final ProgressDialog waitForLoad = new ProgressDialog(activity);
      waitForLoad.setIndeterminate(true);
      waitForLoad.setTitle(activity.getResources().getString(R.string.getting_location));
      waitForLoad.show();

      waitForLoad.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialog) {
            wasRedirected = true;
         }
      });
      final LocationManager locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);

      LocationListener locationListener = new LocationListener() {
         public void onLocationChanged(Location location) {
            //locationManager.removeUpdates(this);
            lastLocation = location;
            redirToLastLocation(activity);
            waitForLoad.dismiss();
         }

         public void onStatusChanged(String provider, int status, Bundle extras) {
         }

         public void onProviderEnabled(String provider) {
         }

         public void onProviderDisabled(String provider) {
         }
      };

      locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
   }

   private void redirToLastLocation(Context activity) {
      if (wasRedirected){
         return;
      }
      final String longitude = String.format(Locale.US, "%.5f", lastLocation.getLongitude());
      final String latitude = String.format(Locale.US, "%.5f", lastLocation.getLatitude());
      final String uri = "http://coinmap.org/#zoom=11&lat=" + latitude + "&lon=" + longitude + "&layer=OpenStreetMap";
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
      activity.startActivity(browserIntent);
   }
}
