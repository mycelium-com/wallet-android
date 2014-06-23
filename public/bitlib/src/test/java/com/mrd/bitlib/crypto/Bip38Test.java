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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.UnsupportedEncodingException;

import org.junit.Test;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.HexUtils;

public class Bip38Test {

   @Test
   public void encryptNoCompression() throws InterruptedException {
      String encoded = Bip38.encryptNoEcMultiply("TestingOneTwoThree",
            "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR", null, NetworkParameters.productionNetwork);
      assertEquals(encoded, "6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg");
      assertTrue(Bip38.isBip38PrivateKey(encoded));
   }

   @Test
   public void decryptNoCompression() throws InterruptedException {
      String decoded = Bip38.decrypt("6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR");
   }

   @Test
   public void decryptNoCompressionWithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PRVWUbkzzsbcVac2qwfssoUJAN1Xhrg6bNk8J7Nzm5H7kxEbn2Nh2ZoGg",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KN7MzqK5wt2TP1fQCYyHBtDrXdJuXbUzm4A9rKAteGu3Qi5CVR");
   }

   @Test
   public void encryptCompression1() throws InterruptedException {
      String encoded = Bip38.encryptNoEcMultiply("TestingOneTwoThree",
            "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP", null, NetworkParameters.productionNetwork);
      assertEquals(encoded, "6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo");
      assertTrue(Bip38.isBip38PrivateKey(encoded));
   }

   @Test
   public void decryptCompression1() throws InterruptedException {
      String decoded = Bip38.decrypt("6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP");
   }

   @Test
   public void decryptCompression1WithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PYNKZ1EAgYgmQfmNVamxyXVWHzK5s6DGhwP4J5o44cvXdoY7sRzhtpUeo",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "L44B5gGEpqEDRS9vVPz7QT35jcBG2r3CZwSwQ4fCewXAhAhqGVpP");
   }

   @Test
   public void encryptCompression2() throws InterruptedException {
      String encoded = Bip38.encryptNoEcMultiply("Satoshi", "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7",
            null, NetworkParameters.productionNetwork);
      assertEquals(encoded, "6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7");
      assertTrue(Bip38.isBip38PrivateKey(encoded));
   }

   @Test
   public void decryptCompression2() throws InterruptedException {
      String decoded = Bip38.decrypt("6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7", "Satoshi", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7");
   }

   @Test
   public void decryptCompression2WithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PYLtMnXvfG3oJde97zRyLYFZCYizPU5T3LwgdYJz1fRhh16bU7u6PPmY7", "Satoshi", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "KwYgW8gcxj1JWJXhPSu4Fqwzfhp5Yfi42mdYmMa4XqK7NJxXUSK7");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyNoLot1() throws InterruptedException {
      String decoded = Bip38.decrypt("6PfQu77ygVyJLZjfvMLyhLMQbYnu5uguoJJ4kMCLqWwPEdfpwANVS76gTX",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "5K4caxezwjGCGfnoPTZ8tMcJBLB7Jvyjv4xxeacadhq8nLisLR2");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyNoLot1WithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PfQu77ygVyJLZjfvMLyhLMQbYnu5uguoJJ4kMCLqWwPEdfpwANVS76gTX",
            "TestingOneTwoThree", null, NetworkParameters.productionNetwork);
      assertEquals(decoded, "5K4caxezwjGCGfnoPTZ8tMcJBLB7Jvyjv4xxeacadhq8nLisLR2");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyNoLot2() throws InterruptedException {
      String decoded = Bip38.decrypt("6PfLGnQs6VZnrNpmVKfjotbnQuaJK4KZoPFrAjx1JMJUa1Ft8gnf5WxfKd", "Satoshi", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KJ51SgxWaAYR13zd9ReMhJpwrcX47xTJh2D3fGPG9CM8vkv5sH");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyNoLot2WithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PfLGnQs6VZnrNpmVKfjotbnQuaJK4KZoPFrAjx1JMJUa1Ft8gnf5WxfKd", "Satoshi", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KJ51SgxWaAYR13zd9ReMhJpwrcX47xTJh2D3fGPG9CM8vkv5sH");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyWithLot1() throws InterruptedException {
      String decoded = Bip38.decrypt("6PgNBNNzDkKdhkT6uJntUXwwzQV8Rr2tZcbkDcuC9DZRsS6AtHts4Ypo1j", "MOLON LABE", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5JLdxTtcTHcfYcmJsNVy1v2PMDx432JPoYcBTVVRHpPaxUrdtf8");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyWithLot1WithBom() throws InterruptedException {
      String decoded = Bip38.decrypt("\uFEFF6PgNBNNzDkKdhkT6uJntUXwwzQV8Rr2tZcbkDcuC9DZRsS6AtHts4Ypo1j", "MOLON LABE", null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5JLdxTtcTHcfYcmJsNVy1v2PMDx432JPoYcBTVVRHpPaxUrdtf8");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyWithLot2() throws InterruptedException, UnsupportedEncodingException {
      // "MOLON LABE" using greek characters  = "ΜΟΛΩΝ ΛΑΒΕ" 
      String passphrase = "\u039C\u039F\u039B\u03A9\u039D \u039B\u0391\u0392\u0395";
      assertEquals("ce9cce9fce9bcea9ce9d20ce9bce91ce92ce95", HexUtils.toHex(passphrase.getBytes("UTF-8")));
      String decoded = Bip38.decrypt("6PgGWtx25kUg8QWvwuJAgorN6k9FbE25rv5dMRwu5SKMnfpfVe5mar2ngH", passphrase, null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KMKKuUmAkiNbA3DazMQiLfDq47qs8MAEThm4yL8R2PhV1ov33D");
   }

   @Test
   public void decryptNoCompressionWithEcMultiplyWithLot2WithBom() throws InterruptedException, UnsupportedEncodingException {
      // "MOLON LABE" using greek characters  = "ΜΟΛΩΝ ΛΑΒΕ"
      String passphrase = "\u039C\u039F\u039B\u03A9\u039D \u039B\u0391\u0392\u0395";
      assertEquals("ce9cce9fce9bcea9ce9d20ce9bce91ce92ce95", HexUtils.toHex(passphrase.getBytes("UTF-8")));
      String decoded = Bip38.decrypt("\uFEFF6PgGWtx25kUg8QWvwuJAgorN6k9FbE25rv5dMRwu5SKMnfpfVe5mar2ngH", passphrase, null,
            NetworkParameters.productionNetwork);
      assertEquals(decoded, "5KMKKuUmAkiNbA3DazMQiLfDq47qs8MAEThm4yL8R2PhV1ov33D");
   }
}
