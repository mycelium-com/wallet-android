package com.mycelium.lt;

public interface ErrorCallback {
   void collectError(Exception e, String url, String data);
}
