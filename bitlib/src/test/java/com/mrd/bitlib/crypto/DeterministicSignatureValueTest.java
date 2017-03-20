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

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import org.junit.Test;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DeterministicSignatureValueTest {

   private class SignatureTestVector{
      private final PrivateKey pk;
      byte[] message;
      BigInteger r;
      BigInteger s;

      public SignatureTestVector(PrivateKey pk, byte[] message, BigInteger r, BigInteger s) {
         this.pk = pk;
         this.message = message;
         this.r = r;
         this.s = s;
      }

      public SignatureTestVector(String pk, String message, String r, String s) throws UnsupportedEncodingException {
         this(
            new InMemoryPrivateKey(HexUtils.toBytes(pk), true),
            message.getBytes("UTF-8"),
            new BigInteger(r),
            new BigInteger(s)
         );
      }

      public SignatureTestVector(String pk, String message, String signatureDer) throws UnsupportedEncodingException
      {
         final Signature signature = Signatures.decodeSignatureParameters(new ByteReader(HexUtils.toBytes(signatureDer)));
         this.pk = new InMemoryPrivateKey(HexUtils.toBytes(pk), true);
         this.message = message.getBytes("UTF-8");
         this.r = signature.r;
         this.s = signature.s;
      }

      public void check(){
         Sha256Hash toSign;
         Signature sig;

         toSign = HashUtils.sha256(message);
         sig = pk.generateSignature(toSign);

         assertEquals("R", r, sig.r);
         assertEquals("S", s, sig.s);

         // Verify that the signature is valid
         assertTrue(Signatures.verifySignature(toSign.getBytes(), sig, pk.getPublicKey().getQ()));

      }
   }

   /**
    * Check if the signatures are RFC6979 compliant, by using various test vectors
    */
   @Test
   public void checkDeterministicSig() throws UnsupportedEncodingException {

      // todo: more/other testvectors?
      // https://bitcointalk.org/index.php?topic=285142.40

      new SignatureTestVector(
             "0000000000000000000000000000000000000000000000000000000000000001"
            ,"Everything should be made as simple as possible, but not simpler."
            ,"3044022033a69cd2065432a30f3d1ce4eb0d59b8ab58c74f27c41a7fdb5696ad4e6108c902206f807982866f785d3f6418d24163ddae117b7db4d5fdf0071de069fa54342262"
            ).check();

      new SignatureTestVector(
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140"
            ,"Equations are more important to me, because politics is for the present, but an equation is something for eternity."
            ,"3044022054c4a33c6423d689378f160a7ff8b61330444abb58fb470f96ea16d99d4a2fed022007082304410efa6b2943111b6a4e0aaa7b7db55a07e9861d1fb3cb1f421044a5"
      ).check();

      new SignatureTestVector(
            "fffffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140"
            ,"Not only is the Universe stranger than we think, it is stranger than we can think."
            ,"3045022100ff466a9f1b7b273e2f4c3ffe032eb2e814121ed18ef84665d0f515360dab3dd002206fc95f5132e5ecfdc8e5e6e616cc77151455d46ed48f5589b7db7771a332b283"
      ).check();

      new SignatureTestVector(
            "0000000000000000000000000000000000000000000000000000000000000001"
            ,"How wonderful that we have met with a paradox. Now we have some hope of making progress."
            ,"3045022100c0dafec8251f1d5010289d210232220b03202cba34ec11fec58b3e93a85b91d3022075afdc06b7d6322a590955bf264e7aaa155847f614d80078a90292fe205064d3"
      ).check();

      new SignatureTestVector(
            "69ec59eaa1f4f2e36b639716b7c30ca86d9a5375c7b38d8918bd9c0ebc80ba64"
            ,"Computer science is no more about computers than astronomy is about telescopes."
            ,"304402207186363571d65e084e7f02b0b77c3ec44fb1b257dee26274c38c928986fea45d02200de0b38e06807e46bda1f1e293f4f6323e854c86d58abdd00c46c16441085df6"
      ).check();

      new SignatureTestVector(
            "00000000000000000000000000007246174ab1e92e9149c6e446fe194d072637"
            ,"...if you aren't, at any given time, scandalized by code you wrote five or even three years ago, you're not learning anywhere near enough"
            ,"3045022100fbfe5076a15860ba8ed00e75e9bd22e05d230f02a936b653eb55b61c99dda48702200e68880ebb0050fe4312b1b1eb0899e1b82da89baa5b895f612619edf34cbd37"
      ).check();

      new SignatureTestVector(
            "000000000000000000000000000000000000000000056916d0f9b31dc9b637f3"
            ,"The question of whether computers can think is like the question of whether submarines can swim."
            ,"3045022100cde1302d83f8dd835d89aef803c74a119f561fbaef3eb9129e45f30de86abbf9022006ce643f5049ee1f27890467b77a6a8e11ec4661cc38cd8badf90115fbd03cef"
      ).check();



      // own
      new SignatureTestVector(
            "00fffffffffffffffffffffffffffffebaaedce6af48a03bbfd25e8cd0364140",
            "Equations are more important to me, because politics is for the present, but an equation is something for eternity.",
            "26820159967830081822798970710067426129483172706261789974922243430675895943943",
            "14912181929787960001668259713503722835850920746900933010493547455088626203371"
            ).check();





   }

}
