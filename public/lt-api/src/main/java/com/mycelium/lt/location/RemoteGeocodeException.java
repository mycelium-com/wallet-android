package com.mycelium.lt.location;

public class RemoteGeocodeException extends Exception {
   private static final long serialVersionUID = 4646210150078841846L;
   public final String status;
   @SuppressWarnings("unused")
   private final String url;

   public RemoteGeocodeException(String message, String status, String errorMessage) {
      super(message + " status: " + status + " errorMessage: " + errorMessage);
      this.status = status;
      url = null;
   }

   public RemoteGeocodeException(String url, String status) {
      super(url + " status: " + status);
      this.status = status;
      this.url = url;
   }
}
