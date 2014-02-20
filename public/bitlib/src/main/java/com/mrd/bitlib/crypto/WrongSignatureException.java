package com.mrd.bitlib.crypto;

public class WrongSignatureException extends Exception {
   private static final long serialVersionUID = -7005402117195958469L;

   public WrongSignatureException(String message) {
      super(message);
   }
}
