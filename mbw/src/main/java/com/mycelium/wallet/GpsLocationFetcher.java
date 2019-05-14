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

package com.mycelium.wallet;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;
import android.widget.Toast;

import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.location.Geocode;
import com.mycelium.lt.location.GeocodeResponse;
import com.mycelium.lt.location.RemoteGeocodeException;
import com.mycelium.wallet.lt.BackendGeocoder;

public class GpsLocationFetcher {
   private BackendGeocoder backendGeocoder;
   public static class GpsLocationEx extends GpsLocation {
      private static final long serialVersionUID = 1L;

      public String countryCode;

      public GpsLocationEx(double latitude, double longitude, String name, String countryCode) {
         super(latitude, longitude, name);
         this.countryCode = countryCode;
      }

      public static GpsLocationEx fromGpsLocation(GpsLocation location) {
         if (location == null) {
            return null;
         }
         return new GpsLocationEx(location.latitude, location.longitude, location.name, "");
      }

      public static GpsLocationEx fromAddress(Address addr) {
         if (addr == null) {
            return null;
         }
         String addressString = addr.getCountryName();
         if (addr.getLocality() != null) {
            addressString = addr.getLocality() + ", " + addressString;
         }
         if (addr.getThoroughfare() != null) {
            addressString = addr.getThoroughfare() + ", " + addressString;
         }
         return new GpsLocationEx(addr.getLatitude(), addr.getLongitude(), addressString,
                 addr.getCountryCode());
      }

      @Override
      public String toString() {
         return name;
      }
   }

   public static abstract class Callback {
      private Context _context;
      private Handler _handler;
      private boolean _cancelled;

      protected Callback(Context context) {
         _context = context;
         _handler = new Handler();
      }

      public void cancel() {
         _cancelled = true;
      }

      /**
       * Called when the the GPS location has been obtained. The location is
       * null if no location could be found
       */
      protected abstract void onGpsLocationObtained(GpsLocationEx location);
   }

   public void getNetworkLocation(final Callback callback) {
      Thread t = new Thread(new Runnable() {

         @Override
         public void run() {
            final GpsLocationEx location;
            location = getNetworkLocation(callback._context);
            callback._handler.post(new Runnable() {
               @Override
               public void run() {
                  if (!callback._cancelled) {
                     callback.onGpsLocationObtained(location);
                  }
               }
            });

         }
      });
      t.setDaemon(true);
      t.start();
   }

   private GpsLocationEx getNetworkLocation(Context context) {
      if (!canObtainGpsPosition(context)) {
         return null;
      }
      LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      @SuppressLint("MissingPermission")
      Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      if (lastKnownLocation == null) {
         return null;
      }

      Address address = getAddress(context, lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude());
      return GpsLocationEx.fromAddress(address);
   }

   private static boolean canObtainGpsPosition(Context context) {
      final boolean hasFeature = context.getPackageManager().hasSystemFeature("android.hardware.location.network");
      if (!hasFeature) {
         return false;
      }
      String permission = "android.permission.ACCESS_COARSE_LOCATION";
      int res = context.checkCallingOrSelfPermission(permission);
      return (res == PackageManager.PERMISSION_GRANTED);
   }

   private Address getAddress(Context context, double lat, double lng) {
      if (Geocoder.isPresent()) {
         Geocoder geocoder = new Geocoder(context, Locale.getDefault());
         try {
            List<Address> addresses = geocoder.getFromLocation(lat, lng, 1);
            if (addresses != null && addresses.size() > 0) {
               return addresses.get(0);
            }
         } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
         }
      }
      if (backendGeocoder == null) {
         backendGeocoder = new BackendGeocoder(MbwManager.getInstance(context).getLocalTraderManager());
      }
      try {
         // TODO: 21.10.18 don't turn geocodes into addresses if they are only turned into geocodes later anyway. 
         GeocodeResponse location = backendGeocoder.getFromLocation(lat, lng);
         if (location.results != null && location.results.size() > 0) {
            Geocode geocode = location.results.get(0);
            Address address = new Address(Locale.getDefault());
            address.setCountryCode(geocode.getCountryCode());
            address.setLatitude(geocode.getLatitude());
            address.setLongitude(geocode.getLongitude());
            address.setCountryName(geocode.getCountryCode());
            return address;
         }
      } catch (RemoteGeocodeException e) {
         e.printStackTrace();
         Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
      }
      return null;
   }
}
