/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib.crypto;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import com.mrd.bitlib.crypto.Hmac;
import com.mrd.bitlib.util.BitUtils;

/**
 * A Pseudo Random Number Generator based on HMAC SHA-256 which is wrapping
 * {@link SecureRandom}. This way we are certain that we use the same random
 * generator on all platforms, and can generate the same sequence of random
 * bytes from the same seed. This is the implementation that was used in
 * BitcoinSpinner.
 */
public class HmacPRNG extends SecureRandom {

   private static final long serialVersionUID = 5678497558585271430L;

   private int _nonce;
   private byte[] _key;
   private byte[] _randomBuffer;
   private int _index;

   /**
    * Constructor based on an input seed.
    * 
    * @param seed
    *           The seed to use.
    * @throws NoSuchAlgorithmException
    */
   public HmacPRNG(byte[] seed) throws NoSuchAlgorithmException {
      _key = seed;
      _nonce = 1;
      _randomBuffer = new byte[16];
      hmacIteration();
   }

   private void hmacIteration() {
      byte[] message = new byte[4];
      BitUtils.uint32ToByteArrayLE(_nonce++, message, 0);
      byte[] temp = Hmac.hmacSha256(_key, message);
      // Only use half of the output as random bytes
      System.arraycopy(temp, 0, _randomBuffer, 0, _randomBuffer.length);
      _index = 0;
   }

   @Override
   public String getAlgorithm() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public synchronized void setSeed(byte[] seed) {
      throw new RuntimeException("Not supported");
   }

   @Override
   public void setSeed(long seed) {
      // ignore
   }

   @Override
   public synchronized void nextBytes(byte[] bytes) {
      for (int i = 0; i < bytes.length; i++) {
         bytes[i] = nextByte();
      }
   }

   private byte nextByte() {
      if (_index == _randomBuffer.length) {
         hmacIteration();
      }
      return _randomBuffer[_index++];
   }

   @Override
   public byte[] generateSeed(int numBytes) {
      throw new RuntimeException("Not supported");
   }

   @Override
   public int nextInt() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public int nextInt(int n) {
      throw new RuntimeException("Not supported");
   }

   @Override
   public long nextLong() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public boolean nextBoolean() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public float nextFloat() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public double nextDouble() {
      throw new RuntimeException("Not supported");
   }

   @Override
   public synchronized double nextGaussian() {
      throw new RuntimeException("Not supported");
   }

}
