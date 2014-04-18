package com.mycelium.wallet;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Handler;

import com.mycelium.lt.ErrorCallback;
import com.mycelium.lt.api.model.GpsLocation;
import com.mycelium.lt.location.Geocode;
import com.mycelium.lt.location.GeocodeResponse;
import com.mycelium.lt.location.JsonCoder;
import com.mycelium.wallet.lt.AddressDescription;

import java.util.List;

public class GpsLocationFetcher {

   final ErrorCallback errorCallback;

   public GpsLocationFetcher(ErrorCallback errorCallback) {
      this.errorCallback = errorCallback;
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
      protected abstract void onGpsLocationObtained(GpsLocation location);

   }

   public void getNetworkLocation(final Callback callback) {
      Thread t = new Thread(new Runnable() {

         @Override
         public void run() {
            final GpsLocation location = getNetworkLocation(callback._context);
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

   private GpsLocation getNetworkLocation(Context context) {
      if (!canObtainGpsPosition(context)) {
         return null;
      }
      LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
      Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
      if (lastKnownLocation == null)
         return null;
      final List<Geocode> list;
      String language = MbwManager.getInstance(context).getLanguage();
      JsonCoder jsonCoder = new JsonCoder(language, errorCallback);
      GeocodeResponse response = jsonCoder.getFromLocation(lastKnownLocation.getLatitude(),
            lastKnownLocation.getLongitude());
      list = response.results;
      if (list.isEmpty()) {
         return null;
      }

      return new GpsLocation(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude(), new AddressDescription(
            list.get(0)).toString());
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

}
