package com.satoshilabs.trezor;

public class TrezorConnectionException extends RuntimeException {
   public TrezorConnectionException(String msg) {
      super(msg);
   }
}
