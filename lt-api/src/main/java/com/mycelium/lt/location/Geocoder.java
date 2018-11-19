package com.mycelium.lt.location;


public abstract class Geocoder {
   public abstract GeocodeResponse query(String address, int maxResults) throws RemoteGeocodeException;

   public abstract GeocodeResponse getFromLocation(double latitude, double longitude) throws RemoteGeocodeException;

   protected boolean isValidStatus(String status) {
      return "OK".equals(status) || "ZERO_RESULTS".equals(status);
   }
}
