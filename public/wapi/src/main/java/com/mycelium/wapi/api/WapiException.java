package com.mycelium.wapi.api;

public class WapiException extends Exception {
   private static final long serialVersionUID = 1L;

   public int errorCode;

   public WapiException(int errorCode) {
      super("Wapi error code: "+errorCode);
      this.errorCode = errorCode;
   }

}
