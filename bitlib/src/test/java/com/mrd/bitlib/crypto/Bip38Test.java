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

import static com.mrd.bitlib.crypto.Bip38.decrypt;
import static com.mrd.bitlib.crypto.Bip38.encryptNoEcMultiply;
import static com.mrd.bitlib.crypto.Bip38.isBip38PrivateKey;
import static com.mrd.bitlib.model.NetworkParameters.productionNetwork;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class Bip38Test {
   // All Test Vectors from https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki#Test_vectors
   private static final TestVector[] ENCRYPT_DECRYPT_TVS = {
           // No compression, no EC multiply
           tv("TestingOneTwoThree", "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg", "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR"),
           tv("Satoshi", "6PRNFFkZc2NZ6dJqFfhRoFNMR9Lnyj7dYGrzdgXXVMXcxoKTePPX1dWByq", "5HtasZ6ofTHP6HCwTqTkLDuLQisYPah7aUnSKfC7h4hMUVw2gi5"),
           tv("\u03D2\u0301\u0000" + new StringBuilder().appendCodePoint(0x010400).appendCodePoint(0x01f4a9).toString(), "6PRW5o9FLp4gJDDVqJQKJFTpMvdsSGJxMYHtHaQBF3ooa8mwD69bapcDQn", "5Jajm8eQ22H3pGWLEVCXyvND8dQZhiQhoLJNKjYXk9roUFTMSZ4"),
           // Compression, no EC multiply
           tv("TestingOneTwoThree", "6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo", "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP"),
           tv("Satoshi", "6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7", "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7"),
   };
   private static final TestVector[] DECRYPT_TVS = {
           //EC multiply, no compression, no lot/sequence numbers
           tv("TestingOneTwoThree", "6PfQu77ygVyJLZjfvMLyhLMQbYnu5uguoJJ4kMCLqWwPEdfpwANVS76gTX", "5K4caxezwjGCGfnoPTZ8tMcJBLB7Jvyjv4xxeacadhq8nLisLR2"),
           tv("Satoshi", "6PfLGnQs6VZnrNpmVKfjotbnQuaJK4KZoPFrAjx1JMJUa1Ft8gnf5WxfKd", "5KJ51SgxWaAYR13zd9ReMhJpwrcX47xTJh2D3fGPG9CM8vkv5sH"),
           // EC multiply, no compression, lot/sequence numbers
           tv("MOLON LABE", "6PgNBNNzDkKdhkT6uJntUXwwzQV8Rr2tZcbkDcuC9DZRsS6AtHts4Ypo1j", "5JLdxTtcTHcfYcmJsNVy1v2PMDx432JPoYcBTVVRHpPaxUrdtf8"),
           tv("\u039C\u039F\u039B\u03A9\u039D \u039B\u0391\u0392\u0395", "6PgGWtx25kUg8QWvwuJAgorN6k9FbE25rv5dMRwu5SKMnfpfVe5mar2ngH", "5KMKKuUmAkiNbA3DazMQiLfDq47qs8MAEThm4yL8R2PhV1ov33D"),
   };

   @Test
   public void testVectorsFromTheBipEncrypt() throws InterruptedException {
      for(TestVector tv : ENCRYPT_DECRYPT_TVS) {
         String encoded = encryptNoEcMultiply(tv.passphrase, tv.unencryptedWIF, null);
         assertEquals(tv.encrypted, encoded);
         assertTrue(isBip38PrivateKey(encoded));
      }
   }

   @Test
   public void testVectorsFromTheBipDecrypt() throws InterruptedException {
      for(TestVector tv : ENCRYPT_DECRYPT_TVS) {
         testDecrypt(tv);
      }
   }

   @Test
   public void testVectorsForDecryptOnly() throws InterruptedException {
      for(TestVector tv : DECRYPT_TVS) {
         testDecrypt(tv);
      }
   }

   private void testDecrypt(TestVector tv) throws InterruptedException {
      assertEquals("Without Bom", tv.unencryptedWIF, decrypt(tv.encrypted, tv.passphrase, null, productionNetwork));
      assertEquals("With Bom", tv.unencryptedWIF, decrypt("\uFEFF" + tv.encrypted, tv.passphrase, null, productionNetwork));
   }

   private static TestVector tv(String passphrase, String encrypted, String unencryptedWIF) {
      return new TestVector(passphrase, encrypted, unencryptedWIF);
   }

   private static class TestVector {
      String passphrase;
      String encrypted;
      String unencryptedWIF;
      TestVector(String passphrase, String encrypted, String unencryptedWIF) {
         this.passphrase = passphrase;
         this.encrypted = encrypted;
         this.unencryptedWIF = unencryptedWIF;
      }
   }
}
