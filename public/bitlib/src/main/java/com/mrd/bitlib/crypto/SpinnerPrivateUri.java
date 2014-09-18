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

import com.google.bitcoinj.Base58;
import com.mrd.bitlib.model.NetworkParameters;

import java.security.NoSuchAlgorithmException;

public class SpinnerPrivateUri {
   public final InMemoryPrivateKey key;
   public final NetworkParameters network;

   private SpinnerPrivateUri(InMemoryPrivateKey key, NetworkParameters network) {
      this.key = key;
      this.network = network;
   }

   public static SpinnerPrivateUri fromSpinnerUri(String uri) {
      if (!uri.startsWith("bsb:")) throw new IllegalArgumentException("not a bsb: uri");
      String[] elements = uri.substring(4).split("\\?");
      if (elements.length < 2) throw new IllegalArgumentException("string does not specify key and net");
      String seed = elements[0];
      final NetworkParameters params;
      if (elements[1].equals(("net=0"))) {
         params = NetworkParameters.productionNetwork;
      } else if (elements[1].equals(("net=1"))) {
         params = NetworkParameters.testNetwork;
      } else {
         throw new IllegalArgumentException("network not specified");
      }
      try {
         final HmacPRNG prng = new HmacPRNG(Base58.decode(seed));
         RandomSource randomSource = new RandomSource() {
            
            @Override
            public void nextBytes(byte[] bytes) {
               prng.nextBytes(bytes);
            }
         };
         @SuppressWarnings("unused")
         InMemoryPrivateKey discardMe = new InMemoryPrivateKey(randomSource);
         InMemoryPrivateKey key = new InMemoryPrivateKey(randomSource);
         return new SpinnerPrivateUri(key, params);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException();
      }

   }

}
