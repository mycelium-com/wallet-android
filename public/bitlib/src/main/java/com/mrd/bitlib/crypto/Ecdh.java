package com.mrd.bitlib.crypto;

import java.math.BigInteger;

import com.mrd.bitlib.crypto.ec.EcTools;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.util.HashUtils;

public class Ecdh {

   public static final int SECRET_LENGTH = 32;

   /**
    * Calculate a shared secret using the Elliptic curve variant of
    * Diffie-Hellman applied with Sha256
    * 
    * @param foreignPublicKey
    *           the public key of the other party
    * @param privateKey
    *           you private key
    * @return a 32 byte shared secret
    */
   public static byte[] calculateSharedSecret(PublicKey foreignPublicKey, InMemoryPrivateKey privateKey) {
      Point P = calculateSharedSecretPoint(foreignPublicKey, privateKey);
      BigInteger Px = P.getX().toBigInteger();
      byte[] bytes = EcTools.integerToBytes(Px, SECRET_LENGTH);
      return HashUtils.sha256(bytes).getBytes();
   }

   private static Point calculateSharedSecretPoint(PublicKey foreignPublicKey, InMemoryPrivateKey privateKey) {
      BigInteger pk = new BigInteger(1, privateKey.getPrivateKeyBytes());
      Point P = foreignPublicKey.getQ().multiply(pk);
      return P;
   }

}
