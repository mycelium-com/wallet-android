package com.mycelium.lt.api;

public class LtApiException extends Exception {
   private static final long serialVersionUID = 1L;

   public int errorCode;

   public LtApiException(int errorCode) {
      this.errorCode = errorCode;
   }

}
