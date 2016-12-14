/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
