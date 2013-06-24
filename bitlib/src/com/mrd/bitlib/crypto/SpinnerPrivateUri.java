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
         HmacPRNG prng = new HmacPRNG(Base58.decode(seed));
         InMemoryPrivateKey discardMe = new InMemoryPrivateKey(prng);
         InMemoryPrivateKey key = new InMemoryPrivateKey(prng);
         return new SpinnerPrivateUri(key, params);
      } catch (NoSuchAlgorithmException e) {
         throw new IllegalStateException();
      }

   }

}
