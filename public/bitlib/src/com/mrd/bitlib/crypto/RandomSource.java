package com.mrd.bitlib.crypto;

public abstract class RandomSource {
   /**
    * Generates a user specified number of random bytes
    * 
    * @param bytes
    *           The array to fill with random bytes
    */
   public abstract void nextBytes(byte[] bytes);
}
