package com.mrd.bitlib.crypto;

/**
 * Really simple random generator that forms a ring using a small prime. It
 * generates the same random sequence every time.Don't use where security
 * matters.
 */

public class TestNonRandomSource implements RandomSource {
   private long _state;

   public TestNonRandomSource() {
      _state = 104723;
   }

   @Override
   public void nextBytes(byte[] bytes) {
      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = (byte) (_state & 0xFFL);
         _state *= _state;
         _state %= 104729;
      }
   }

}
