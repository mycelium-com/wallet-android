package com.mycelium.wapi.api;

public interface WapiLogger {
   public void logError(String message, Exception e);

   public void logError(String message);
   
   public void logInfo(String message);
}