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

import com.google.common.base.Preconditions;
import com.lambdaworks.crypto.Base64;
import com.mrd.bitlib.crypto.ec.Curve;
import com.mrd.bitlib.crypto.ec.EcTools;
import com.mrd.bitlib.crypto.ec.Parameters;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.math.BigInteger;

public class SignedMessage implements Serializable {
   private static final long serialVersionUID = 1188125594280603453L;

   private final Signature signature;
   private final PublicKey publicKey;
   private final int recId;

   private SignedMessage(Signature signature, PublicKey publicKey, int recId) {
      this.signature = signature;
      this.publicKey = publicKey;
      this.recId = recId;
   }

   public static PublicKey recoverFromSignature(String message, String signatureBase64) throws WrongSignatureException {
      final byte[] signatureEncoded = Base64.decode(signatureBase64);
      final Signature sig = decodeSignature(signatureEncoded);
      return recoverFromSignature(message, signatureEncoded, sig).publicKey;
   }

   public static SignedMessage validate(Address address, String message, String signatureBase64)
         throws WrongSignatureException {
      final byte[] signatureEncoded = Base64.decode(signatureBase64);
      if (signatureEncoded == null) {
         // Invalid or truncated base64
         throw new WrongSignatureException(String.format("given signature is not valid base64 %s", signatureBase64));
      }
      final Signature sig = decodeSignature(signatureEncoded);
      final RecoveryInfo info = recoverFromSignature(message, signatureEncoded, sig);
      validateAddressMatches(address, info.publicKey);
      return new SignedMessage(sig, info.publicKey, info.recId);
   }

   public static void validateAddressMatches(Address address, PublicKey key) throws WrongSignatureException {
      Address recoveredAddress = key.toAddress(address.getNetwork());
      if (!address.equals(recoveredAddress)) {
         throw new WrongSignatureException(String.format("given Address did not match \nexpected %s\n but got %s",
               address, recoveredAddress));
      }
   }

   /*
    * to avoid extracting Signature sig twice, the parsed version is also passed
    * in here. it must be obtained from signatureEncoded
    */
   private static RecoveryInfo recoverFromSignature(String message, byte[] signatureEncoded, Signature sig)
         throws WrongSignatureException {
      int header = signatureEncoded[0] & 0xFF;
      // The header byte: 0x1B = first key with even y, 0x1C = first key with
      // odd y,
      // 0x1D = second key with even y, 0x1E = second key with odd y
      if (header < 27 || header > 34)
         throw new WrongSignatureException("Header byte out of range: " + header);

      byte[] messageBytes = Signatures.formatMessageForSigning(message);
      // Note that the C++ code doesn't actually seem to specify any character
      // encoding. Presumably it's whatever
      // JSON-SPIRIT hands back. Assume UTF-8 for now.
      Sha256Hash messageHash = HashUtils.doubleSha256(messageBytes);
      boolean compressed = false;
      if (header >= 31) {
         compressed = true;
         header -= 4;
      }
      int recId = header - 27;
      PublicKey ret = recoverFromSignature(recId, sig, messageHash, compressed);
      if (ret == null) {
         throw new WrongSignatureException("Could not recover public key from signature");
      }
      return new RecoveryInfo(ret, recId);
   }

   private static Signature decodeSignature(byte[] signatureEncoded) throws WrongSignatureException {
      // Parse the signature bytes into r/s and the selector value.
      if (signatureEncoded.length < 65)
         throw new WrongSignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);
      BigInteger r = new BigInteger(1, BitUtils.copyOfRange(signatureEncoded, 1, 33));
      BigInteger s = new BigInteger(1, BitUtils.copyOfRange(signatureEncoded, 33, 65));
      return new Signature(r, s);
   }

   public static SignedMessage from(Signature signature, PublicKey publicKey, int recId) {
      return new SignedMessage(signature, publicKey, recId);
   }

   /**
    * <p>
    * Given the components of a signature and a selector value, recover and
    * return the public key that generated the signature according to the
    * algorithm in SEC1v2 section 4.1.6.
    * </p>
    * <p/>
    * <p>
    * The recId is an index from 0 to 3 which indicates which of the 4 possible
    * keys is the correct one. Because the key recovery operation yields
    * multiple potential keys, the correct key must either be stored alongside
    * the signature, or you must be willing to try each recId in turn until you
    * find one that outputs the key you are expecting.
    * </p>
    * <p/>
    * <p>
    * If this method returns null it means recovery was not possible and recId
    * should be iterated.
    * </p>
    * <p/>
    * <p>
    * Given the above two points, a correct usage of this method is inside a for
    * loop from 0 to 3, and if the output is null OR a key that is not the one
    * you expect, you try again with the next recId.
    * </p>
    *
    * @param recId      Which possible key to recover.
    * @param sig        the R and S components of the signature, wrapped.
    * @param message    Hash of the data that was signed.
    * @param compressed Whether or not the original pubkey was compressed.
    * @return PublicKey or null if recovery was not possible
    */
   public static PublicKey recoverFromSignature(int recId, Signature sig, Sha256Hash message, boolean compressed) {
      Preconditions.checkArgument(recId >= 0, "recId must be positive");
      Preconditions.checkArgument(sig.r.compareTo(BigInteger.ZERO) >= 0, "r must be positive");
      Preconditions.checkArgument(sig.s.compareTo(BigInteger.ZERO) >= 0, "s must be positive");
      Preconditions.checkNotNull(message);
      // 1.0 For j from 0 to h (h == recId here and the loop is outside this
      // function)
      // 1.1 Let x = r + jn

      BigInteger n = Parameters.n; // Curve order.
      BigInteger i = BigInteger.valueOf((long) recId / 2);
      BigInteger x = sig.r.add(i.multiply(n));
      // 1.2. Convert the integer x to an octet string X of length mlen using
      // the conversion routine
      // specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen =
      // ⌈m/8⌉.
      // 1.3. Convert the octet string (16 set binary digits)||X to an elliptic
      // curve point R using the
      // conversion routine specified in Section 2.3.4. If this conversion
      // routine outputs "invalid", then
      // do another iteration of Step 1.
      //
      // More concisely, what these points mean is to use X as a compressed
      // public key.

      Curve curve = Parameters.curve;
      BigInteger prime = curve.getQ(); // Bouncy Castle is not consistent about
      // the letter it uses for the prime.
      if (x.compareTo(prime) >= 0) {
         // Cannot have point co-ordinates larger than this as everything takes
         // place modulo Q.
         return null;
      }
      // Compressed keys require you to know an extra bit of data about the
      // y-coord as there are two possibilities.
      // So it's encoded in the recId.
      Point R = EcTools.decompressKey(x, (recId & 1) == 1);
      // 1.4. If nR != point at infinity, then do another iteration of Step 1
      // (callers responsibility).
      if (!R.multiply(n).isInfinity())
         return null;
      // 1.5. Compute e from M using Steps 2 and 3 of ECDSA signature
      // verification.
      BigInteger e = new BigInteger(1, message.getBytes());
      // 1.6. For k from 1 to 2 do the following. (loop is outside this function
      // via iterating recId)
      // 1.6.1. Compute a candidate public key as:
      // Q = mi(r) * (sR - eG)
      //
      // Where mi(x) is the modular multiplicative inverse. We transform this
      // into the following:
      // Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
      // Where -e is the modular additive inverse of e, that is z such that z +
      // e = 0 (mod n). In the above equation
      // ** is point multiplication and + is point addition (the EC group
      // operator).
      //
      // We can find the additive inverse by subtracting e from zero then taking
      // the mod. For example the additive
      // inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and -3 mod 11 =
      // 8.
      BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
      BigInteger rInv = sig.r.modInverse(n);
      BigInteger srInv = rInv.multiply(sig.s).mod(n);
      BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
      Point q = EcTools.sumOfTwoMultiplies(Parameters.G, eInvrInv, R, srInv);
      if (compressed) {
         // We have to manually recompress the point as the compressed-ness gets
         // lost when multiply() is used.
         q = new Point(curve, q.getX(), q.getY(), true);
      }
      return new PublicKey(q.getEncoded());
   }

   /*
    * public static SignedMessage from(byte[] signature, PublicKey publicKey) {
    * ByteReader reader = new ByteReader(signature); Signature sig =
    * Signatures.decodeSignatureParameters(reader);
    * Preconditions.checkState(reader.available() == 0); return new
    * SignedMessage(sig, publicKey, recId); }
    */

   public byte[] bitcoinEncodingOfSignature() {
      if (recId == -1)
         throw new RuntimeException("Could not construct a recoverable key. This should never happen.");
      int headerByte = recId + 27 + (getPublicKey().isCompressed() ? 4 : 0);
      byte[] sigData = new byte[65]; // 1 header + 32 bytes for R + 32 bytes for
      // S
      sigData[0] = (byte) headerByte;
      System.arraycopy(EcTools.integerToBytes(signature.r, 32), 0, sigData, 1, 32);
      System.arraycopy(EcTools.integerToBytes(signature.s, 32), 0, sigData, 33, 32);
      return sigData;
   }

   public PublicKey getPublicKey() {
      return publicKey;
   }

   public String getBase64Signature() {
      return Base64.encodeToString(bitcoinEncodingOfSignature(), false);
   }

   public byte[] getDerEncodedSignature(){
      //byte[] rsValues = bitcoinEncodingOfSignature();

      byte[] rBytes = signature.r.toByteArray();
      byte[] sBytes = signature.s.toByteArray();

      ByteWriter rsValues = new ByteWriter(rBytes.length + sBytes.length + 2 + 2);
      rsValues.put((byte) 0x02);  // Type: integer
      rsValues.put((byte)rBytes.length);  // length
      rsValues.putBytes(rBytes);    // data
      rsValues.put((byte) 0x02);  // Type: integer
      rsValues.put((byte)sBytes.length);  // length
      rsValues.putBytes(sBytes); // data

      ByteWriter byteWriter = new ByteWriter(2 + rsValues.length());

      byteWriter.put((byte)0x30); // Tag
      Preconditions.checkState(rsValues.length() <= 255, "total length should be smaller than 256");
      byteWriter.put((byte) rsValues.length());
      byteWriter.putBytes(rsValues.toBytes());
      return byteWriter.toBytes();
   }

   public static class RecoveryInfo {
      PublicKey publicKey;
      int recId;

      private RecoveryInfo(PublicKey publicKey, int recId) {
         this.publicKey = publicKey;
         this.recId = recId;
      }
   }

}
