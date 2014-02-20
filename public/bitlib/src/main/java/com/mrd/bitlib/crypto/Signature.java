package com.mrd.bitlib.crypto;

import java.math.BigInteger;

import com.mrd.bitlib.util.ByteWriter;

/**
 * represents the values of a signature, independent of its encoding.
 * depending on the context, encode wrapping is needed. subclass this to represent additional metadata
 * SIGHASH type, deterministic r, etc.
 */
public class Signature {

   public final BigInteger r;
   public final BigInteger s;

   public Signature(BigInteger r, BigInteger s) {
      this.r = r;
      this.s = s;
   }

   public byte[] derEncode() { //todo emit Subclass instead, with cached encoding
      // Write DER encoding of signature
      ByteWriter writer = new ByteWriter(1024);
      // Write tag
      writer.put((byte) 0x30);
      // Write total length
      byte[] s1 = r.toByteArray();
      byte[] s2 = s.toByteArray();
      int totalLength = 2 + s1.length + 2 + s2.length;
      if (totalLength > 127) {
         // We assume that the total length never goes beyond a 1-byte
         // representation
         throw new RuntimeException("Unsupported signature length: " + totalLength);
      }
      writer.put((byte) (totalLength & 0xFF));
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s1.length & 0xFF));
      // Write bytes
      writer.putBytes(s1);
      // Write type
      writer.put((byte) 0x02);
      // We assume that the length never goes beyond a 1-byte representation
      writer.put((byte) (s2.length & 0xFF));
      // Write bytes
      writer.putBytes(s2);
      return writer.toBytes();
   }
}
