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

/**
 * Parts of this code was extracted from the Java cryptography library from
 * www.bouncycastle.org.
 */
package com.mrd.bitlib.crypto;

import com.google.bitcoinj.Base58;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.ec.EcTools;
import com.mrd.bitlib.crypto.ec.Parameters;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.Arrays;

/**
 * A Bitcoin private key that is kept in memory.
 */
public class InMemoryPrivateKey extends PrivateKey implements KeyExporter, Serializable {

   private static final long serialVersionUID = 1L;

   private final BigInteger _privateKey;
   private final PublicKey _publicKey;

   /**
    * Construct a random private key using a secure random source. Using this
    * constructor yields uncompressed public keys.
    */
   public InMemoryPrivateKey(RandomSource randomSource) {
      this(randomSource, false);
   }

   /**
    * Construct a random private key using a secure random source with optional
    * compressed public keys.
    *
    * @param  randomSource
    *           The random source from which the private key will be
    *           deterministically generated.
    * @param compressed
    *           Specifies whether the corresponding public key should be
    *           compressed
    */
   public InMemoryPrivateKey(RandomSource randomSource, boolean compressed) {
      int nBitLength = Parameters.n.bitLength();
      BigInteger d;
      do {
         // Make a BigInteger from bytes to ensure that Andriod and 'classic'
         // java make the same BigIntegers from the same random source with the
         // same seed. Using BigInteger(nBitLength, random)
         // produces different results on Android compared to 'classic' java.
         byte[] bytes = new byte[nBitLength / 8];
         randomSource.nextBytes(bytes);
         bytes[0] = (byte) (bytes[0] & 0x7F); // ensure positive number
         d = new BigInteger(bytes);
      } while (d.equals(BigInteger.ZERO) || (d.compareTo(Parameters.n) >= 0));

      Point Q = EcTools.multiply(Parameters.G, d);
      _privateKey = d;
      if (compressed) {
         // Convert Q to a compressed point on the curve
         Q = new Point(Q.getCurve(), Q.getX(), Q.getY(), true);
      }
      _publicKey = new PublicKey(Q.getEncoded());
   }

   /**
    * Construct from private key bytes. Using this constructor yields
    * uncompressed public keys.
    *
    * @param bytes
    *           The private key as an array of bytes
    */
   public InMemoryPrivateKey(byte[] bytes) {
      this(bytes, false);
   }

   public InMemoryPrivateKey(Sha256Hash hash, boolean compressed) {
       this(hash.getBytes(),compressed);
   }

   /**
    * Construct from private key bytes. Using this constructor yields
    * uncompressed public keys.
    *
    * @param bytes
    *           The private key as an array of bytes
    * @param compressed
    *           Specifies whether the corresponding public key should be
    *           compressed
    */
   public InMemoryPrivateKey(byte[] bytes, boolean compressed) {
      if (bytes.length != 32) {
         throw new IllegalArgumentException("The length must be 32 bytes");
      }
      // Ensure that we treat it as a positive number
      byte[] keyBytes = new byte[33];
      System.arraycopy(bytes, 0, keyBytes, 1, 32);
      _privateKey = new BigInteger(keyBytes);
      Point Q = EcTools.multiply(Parameters.G, _privateKey);
      if (compressed) {
         // Convert Q to a compressed point on the curve
         Q = new Point(Q.getCurve(), Q.getX(), Q.getY(), true);
      }
      _publicKey = new PublicKey(Q.getEncoded());
   }

   /**
    * Construct from private and public key bytes
    *
    * @param priBytes
    *           The private key as an array of bytes
    */
   public InMemoryPrivateKey(byte[] priBytes, byte[] pubBytes) {
      if (priBytes.length != 32) {
         throw new IllegalArgumentException("The length of the array of bytes must be 32");
      }
      // Ensure that we treat it as a positive number
      byte[] keyBytes = new byte[33];
      System.arraycopy(priBytes, 0, keyBytes, 1, 32);
      _privateKey = new BigInteger(keyBytes);
      _publicKey = new PublicKey(pubBytes);
   }

   /**
    * Construct from a base58 encoded key (SIPA format)
    */
   public InMemoryPrivateKey(String base58Encoded, NetworkParameters network) {
      byte[] decoded = Base58.decodeChecked(base58Encoded);

      // Validate format
      if (decoded == null || decoded.length < 33 || decoded.length > 34) {
         throw new IllegalArgumentException("Invalid base58 encoded key");
      }
      if (network.equals(NetworkParameters.productionNetwork) && decoded[0] != (byte) 0x80) {
         throw new IllegalArgumentException("The base58 encoded key is not for the production network");
      }
      if (network.equals(NetworkParameters.testNetwork) && decoded[0] != (byte) 0xEF) {
         throw new IllegalArgumentException("The base58 encoded key is not for the test network");
      }

      // Determine whether compression should be used for the public key
      boolean compressed;
      if (decoded.length == 34) {
         if (decoded[33] != 0x01) {
            throw new IllegalArgumentException("Invalid base58 encoded key");
         }
         // Get rid of the compression indication byte at the end
         byte[] temp = new byte[33];
         System.arraycopy(decoded, 0, temp, 0, temp.length);
         decoded = temp;
         compressed = true;
      } else {
         compressed = false;
      }
      // Make positive and clear network prefix
      decoded[0] = 0;

      _privateKey = new BigInteger(decoded);
      Point Q = EcTools.multiply(Parameters.G, _privateKey);
      if (compressed) {
         // Convert Q to a compressed point on the curve
         Q = new Point(Q.getCurve(), Q.getX(), Q.getY(), true);
      }
      _publicKey = new PublicKey(Q.getEncoded());
   }

   public static Optional<InMemoryPrivateKey> fromBase58String(String base58, NetworkParameters network) {
      try {
         InMemoryPrivateKey key = new InMemoryPrivateKey(base58, network);
         return Optional.of(key);
      } catch (IllegalArgumentException e) {
         return Optional.absent();
      }
   }

   public static Optional<InMemoryPrivateKey> fromBase58MiniFormat(String base58, NetworkParameters network) {
      // Is it a mini private key on the format proposed by Casascius?
      if (base58 == null || base58.length() < 2 || !base58.startsWith("S")) {
         return Optional.absent();
      }
      // Check that the string has a valid checksum
      String withQuestionMark = base58 + "?";
      byte[] checkHash = HashUtils.sha256(withQuestionMark.getBytes()).firstFourBytes();
      if (checkHash[0] != 0x00) {
         return Optional.absent();
      }
      // Now get the Sha256 hash and use it as the private key
      Sha256Hash privateKeyBytes = HashUtils.sha256(base58.getBytes());
      try {
         InMemoryPrivateKey key = new InMemoryPrivateKey(privateKeyBytes, false);
         return Optional.of(key);
      } catch (IllegalArgumentException e) {
         return Optional.absent();
      }
   }

   @Override
   public PublicKey getPublicKey() {
      return _publicKey;
   }



   private BigInteger calculateE(BigInteger n, byte[] messageHash) {
      if (n.bitLength() > messageHash.length * 8) {
         return new BigInteger(1, messageHash);
      } else {
         int messageBitLength = messageHash.length * 8;
         BigInteger trunc = new BigInteger(1, messageHash);

         if (messageBitLength - n.bitLength() > 0) {
            trunc = trunc.shiftRight(messageBitLength - n.bitLength());
         }

         return trunc;
      }
   }

   private static abstract class DsaSignatureNonceGen {
      public abstract BigInteger getNonce();
   }

   private static class DsaSignatureNonceGenRandom extends DsaSignatureNonceGen {
      private final RandomSource randomSource;

      private DsaSignatureNonceGenRandom(RandomSource randomSource) {
         this.randomSource = randomSource;
      }

      @Override
      public BigInteger getNonce() {
         BigInteger k;
         int nBitLength = Parameters.n.bitLength();
         do {
            // make a BigInteger from bytes to ensure that Andriod and
            // 'classic' java make the same BigIntegers
            byte[] bytes = new byte[nBitLength / 8];
            randomSource.nextBytes(bytes);
            bytes[0] = (byte) (bytes[0] & 0x7F); // ensure positive number
            k = new BigInteger(bytes);
         } while (k.equals(BigInteger.ZERO));
         return k;
      }
   }

   private static class DsaSignatureNonceGenDeterministic extends DsaSignatureNonceGen {

      private final Sha256Hash messageHash;
      private final KeyExporter privateKey;

      private DsaSignatureNonceGenDeterministic(Sha256Hash messageHash, KeyExporter privateKey) {
         this.messageHash = messageHash;
         this.privateKey = privateKey;
      }

      // rfc6979 compliant generation of k-value for DSA
      @Override
      public BigInteger getNonce(){

         // Step b
         byte[] v = new byte[32];
         Arrays.fill(v, (byte)0x01);

         // Step c
         byte [] k = new byte[32];
         Arrays.fill(k, (byte)0x00);

         // Step d
         ByteWriter bwD = new ByteWriter(32 + 1 + 32 + 32);
         bwD.putBytes(v);
         bwD.put((byte) 0x00 );
         bwD.putBytes(privateKey.getPrivateKeyBytes());
         bwD.putBytes(messageHash.getBytes());
         k = Hmac.hmacSha256(k, bwD.toBytes());

         // Step e
         v = Hmac.hmacSha256(k, v);

         // Step f
         ByteWriter bwF = new ByteWriter(32 + 1 + 32 + 32);
         bwF.putBytes(v);
         bwF.put((byte) 0x01 );
         bwF.putBytes(privateKey.getPrivateKeyBytes());
         bwF.putBytes(messageHash.getBytes());
         k = Hmac.hmacSha256(k, bwF.toBytes());

         // Step g
         v = Hmac.hmacSha256(k, v);

         // Step H2b
         v = Hmac.hmacSha256(k, v);

         BigInteger t = bits2int(v);

         // Step H3, repeat until T is within the interval [1, Parameters.n - 1]
         while ((t.signum() <= 0) || (t.compareTo(Parameters.n) >= 0)) {
            ByteWriter bwH = new ByteWriter(32 + 1);
            bwH.putBytes(v);
            bwH.put((byte) 0x00);
            k = Hmac.hmacSha256(k, bwH.toBytes());
            v = Hmac.hmacSha256(k, v);

            t = new BigInteger(v);
         }
         return t;
      }

      private BigInteger bits2int(byte[] in)
      {
         BigInteger v = new BigInteger(1, in);
         // Step H1/H2a, ignored as tlen === qlen (256 bit)
         return v;
      }
   }


   @Override
   protected Signature generateSignature(Sha256Hash messageHash) {
      return generateSignatureInternal(messageHash, new DsaSignatureNonceGenDeterministic(messageHash, this));
   }

   @Override
   protected Signature generateSignature(Sha256Hash messageHash, RandomSource randomSource) {
      return generateSignatureInternal(messageHash, new DsaSignatureNonceGenRandom(randomSource));
   }

   private Signature generateSignatureInternal(Sha256Hash messageHash, DsaSignatureNonceGen kGen) {
      BigInteger n = Parameters.n;
      BigInteger e = calculateE(n, messageHash.getBytes()); //leaving strong typing here
      BigInteger r = null;
      BigInteger s = null;

      // 5.3.2
      do // generate s
      {
         BigInteger k = kGen.getNonce();

         // generate r
         Point p = EcTools.multiply(Parameters.G, k);

         // 5.3.3
         BigInteger x = p.getX().toBigInteger();

         r = x.mod(n);

         BigInteger d = _privateKey;

         s = k.modInverse(n).multiply(e.add(d.multiply(r))).mod(n);
      } while (s.equals(BigInteger.ZERO));

      // Enforce low S value
      if(s.compareTo(Parameters.MAX_SIG_S) == 1){
         // If the signature is larger than MAX_SIG_S, inverse it
         s = Parameters.n.subtract(s);
      }

      return new Signature(r, s);
   }

   @Override
   public byte[] getPrivateKeyBytes() {
      byte[] result = new byte[32];
      byte[] bytes = _privateKey.toByteArray();
      if (bytes.length <= result.length) {
         System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
      } else {
         // This happens if the most significant bit is set and we have an
         // extra leading zero to avoid a negative BigInteger
         assert bytes.length == 33 && bytes[0] == 0;
         System.arraycopy(bytes, 1, result, 0, bytes.length - 1);
      }
      return result;
   }

   @Override
   public String getBase58EncodedPrivateKey(NetworkParameters network) {
      if (getPublicKey().isCompressed()) {
         return getBase58EncodedPrivateKeyCompressed(network);
      } else {
         return getBase58EncodedPrivateKeyUncompressed(network);
      }
   }

   private String getBase58EncodedPrivateKeyUncompressed(NetworkParameters network) {
      byte[] toEncode = new byte[1 + 32 + 4];
      // Set network
      toEncode[0] = network.isProdnet() ? (byte) 0x80 : (byte) 0xEF;
      // Set key bytes
      byte[] keyBytes = getPrivateKeyBytes();
      System.arraycopy(keyBytes, 0, toEncode, 1, keyBytes.length);
      // Set checksum
      byte[] checkSum = HashUtils.doubleSha256(toEncode, 0, 1 + 32).firstFourBytes();
      System.arraycopy(checkSum, 0, toEncode, 1 + 32, 4);
      // Encode
      return Base58.encode(toEncode);
   }

   private String getBase58EncodedPrivateKeyCompressed(NetworkParameters network) {
      byte[] toEncode = new byte[1 + 32 + 1 + 4];
      // Set network
      toEncode[0] = network.isProdnet() ? (byte) 0x80 : (byte) 0xEF;
      // Set key bytes
      byte[] keyBytes = getPrivateKeyBytes();
      System.arraycopy(keyBytes, 0, toEncode, 1, keyBytes.length);
      // Set compressed indicator
      toEncode[33] = 0x01;
      // Set checksum
      byte[] checkSum = HashUtils.doubleSha256(toEncode, 0, 1 + 32 + 1).firstFourBytes();
      System.arraycopy(checkSum, 0, toEncode, 1 + 32 + 1, 4);
      // Encode
      return Base58.encode(toEncode);
   }

}
