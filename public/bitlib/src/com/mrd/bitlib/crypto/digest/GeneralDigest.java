/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mrd.bitlib.crypto.digest;

/**
 * base implementation of MD4 family style digest as outlined in
 * "Handbook of Applied Cryptography", pages 344 - 347.
 */
public abstract class GeneralDigest {
   private static final int BYTE_LENGTH = 64;
   private byte[] xBuf;
   private int xBufOff;

   private long byteCount;

   /**
    * Standard constructor
    */
   protected GeneralDigest() {
      xBuf = new byte[4];
      xBufOff = 0;
   }

   /**
    * Copy constructor. We are using copy constructors in place of the
    * Object.clone() interface as this interface is not supported by J2ME.
    */
   protected GeneralDigest(GeneralDigest t) {
      xBuf = new byte[t.xBuf.length];
      System.arraycopy(t.xBuf, 0, xBuf, 0, t.xBuf.length);

      xBufOff = t.xBufOff;
      byteCount = t.byteCount;
   }

   public void update(byte in) {
      xBuf[xBufOff++] = in;

      if (xBufOff == xBuf.length) {
         processWord(xBuf, 0);
         xBufOff = 0;
      }

      byteCount++;
   }

   public void update(byte[] in, int inOff, int len) {
      //
      // fill the current word
      //
      while ((xBufOff != 0) && (len > 0)) {
         update(in[inOff]);

         inOff++;
         len--;
      }

      //
      // process whole words.
      //
      while (len > xBuf.length) {
         processWord(in, inOff);

         inOff += xBuf.length;
         len -= xBuf.length;
         byteCount += xBuf.length;
      }

      //
      // load in the remainder.
      //
      while (len > 0) {
         update(in[inOff]);

         inOff++;
         len--;
      }
   }

   public void finish() {
      long bitLength = (byteCount << 3);

      //
      // add the pad bytes.
      //
      update((byte) 128);

      while (xBufOff != 0) {
         update((byte) 0);
      }

      processLength(bitLength);

      processBlock();
   }

   public void reset() {
      byteCount = 0;

      xBufOff = 0;
      for (int i = 0; i < xBuf.length; i++) {
         xBuf[i] = 0;
      }
   }

   public int getByteLength() {
      return BYTE_LENGTH;
   }

   protected abstract void processWord(byte[] in, int inOff);

   protected abstract void processLength(long bitLength);

   protected abstract void processBlock();
}
