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

package com.mrd.bitlib.crypto;

import com.mrd.bitlib.crypto.ec.Parameters;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SignatureSValueTest {

   public class FakeRandom implements RandomSource {
      @Override
      public void nextBytes(byte[] bytes) {
         for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) 0xE9;
         }
      }
   }

   /**
    * The purpose of this test is to verify that the signatures we generate
    * follow the version 3 transaction rules when signing. The rules are
    * introduced to fix transaction malleability issues.
    */
   @Test
   public void checkSValue() {
      FakeRandom rnd = new FakeRandom();
      InMemoryPrivateKey pk = new InMemoryPrivateKey(rnd, true);

      Sha256Hash toSign;
      Signature sig;

      // Generate hash that would create S-value below Parameters.MAX_SIG_S with
      // or without the fix. (positive test)
      toSign = HashUtils.sha256(new byte[]{0x02});
      sig = pk.generateSignature(toSign, rnd);
      // Verify that the S parameter is below MAX_SIG_S
      assertTrue(sig.s.compareTo(Parameters.MAX_SIG_S) == -1);
      // Verify that the signature is valid
      assertTrue(Signatures.verifySignature(toSign.getBytes(), sig, pk.getPublicKey().getQ()));

      // Generate hash that would create S-value above Parameters.MAX_SIG_S
      // without the fix. (negative test)
      toSign = HashUtils.sha256(new byte[]{0x00});
      sig = pk.generateSignature(toSign, rnd);
      // Verify that the S parameter is below MAX_SIG_S
      assertTrue(sig.s.compareTo(Parameters.MAX_SIG_S) == -1);
      // Verify that the signature is valid
      assertTrue(Signatures.verifySignature(toSign.getBytes(), sig, pk.getPublicKey().getQ()));

   }

   @Test
   public void checkSValueDeterministic() {
      FakeRandom rnd = new FakeRandom();
      InMemoryPrivateKey pk = new InMemoryPrivateKey(rnd, true);

      Sha256Hash toSign;
      Signature sig;

      // Generate hash that would create S-value below Parameters.MAX_SIG_S with
      // or without the fix. (positive test)
      toSign = HashUtils.sha256(new byte[]{0x01});
      sig = pk.generateSignature(toSign);
      // Verify that the S parameter is below MAX_SIG_S
      assertTrue(sig.s.compareTo(Parameters.MAX_SIG_S) == -1);
      // Verify that the signature is valid
      assertTrue(Signatures.verifySignature(toSign.getBytes(), sig, pk.getPublicKey().getQ()));

      // Generate hash that would create S-value above Parameters.MAX_SIG_S
      // without the fix. (negative test)
      toSign = HashUtils.sha256(new byte[]{0x02});
      sig = pk.generateSignature(toSign);
      // Verify that the S parameter is below MAX_SIG_S
      assertTrue(sig.s.compareTo(Parameters.MAX_SIG_S) == -1);
      // Verify that the signature is valid
      assertTrue(Signatures.verifySignature(toSign.getBytes(), sig, pk.getPublicKey().getQ()));

   }

    @Test
    public void testSValue(){
        BigInteger half = Parameters.n.divide(new BigInteger("2"));
        assertEquals(Parameters.MAX_SIG_S, half);
    }
}
