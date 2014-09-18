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

package com.mrd.bitlib.util;


import com.lambdaworks.crypto.Base64;

import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionUtils {
   /**
    * Decrypt bytes which were encrypted with password-based AES-256-CBC as it's done by OpenSSL
    * using the commandline:  "openssl enc -e -aes-256-cbc -a -in filenname.txt"
    *
    * @param encryptedMessageBase64 encrypted ciphertext
    * @param password password used for encryption
    * @return decrypted message bytes
    * @throws java.security.GeneralSecurityException
    */
   public static byte[] decryptOpenSslAes256CbcBytes(String encryptedMessageBase64, String password) throws GeneralSecurityException, UnsupportedEncodingException {
      // Offset from beginning of ciphertext where actual salt begins is always 8 bytes (when
      // using OpenSSL), as OpenSSL always places the magic string "Salted__" at the beginning
      // of the ciphertext to indicate that salt was used.
      final int SALT_OFFSET = 8;
      final int SALT_SIZE = 8; // next 8 bytes after header are the actual salt
      final int CIPHERTEXT_OFFSET = SALT_OFFSET + SALT_SIZE; // after that starts the ciphertext

      // As Bitcoin Wallet uses only salted encryption, we check if the ciphertext starts with
      // the string "Salted__". Base64-encoded the first bytes of the string "Salted__"
      // will look like "U2FsdGVkX1":
      if (!encryptedMessageBase64.startsWith("U2FsdGVkX1")) {
         throw new GeneralSecurityException("Ciphertext missing 'Salted__' header");
      }
      // Base64 decode the whole thing
      byte[] headerSaltAndCipherText = Base64.decode(encryptedMessageBase64);

      byte[] salt = new byte[SALT_SIZE];
      // starts with magic header "Salted__" so omit first 8 bytes
      System.arraycopy(headerSaltAndCipherText, SALT_OFFSET, salt, 0, SALT_SIZE);
      // the rest is the actual ciphertext
      byte[] ciphertext = new byte[headerSaltAndCipherText.length - CIPHERTEXT_OFFSET];
      System.arraycopy(headerSaltAndCipherText, CIPHERTEXT_OFFSET, ciphertext, 0, headerSaltAndCipherText.length - CIPHERTEXT_OFFSET);

      Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
      MessageDigest md5 = MessageDigest.getInstance("MD5");

      // using getBytes(String charsetName) here as getBytes(Charset charset) is only available in API level >8
      byte[][] keyAndIV = openSslEVP_BytesToKey(256 / Byte.SIZE, aesCBC.getBlockSize(), md5, salt, password.getBytes("UTF-8"), 1);
      aesCBC.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIV[0], "AES"), new IvParameterSpec(keyAndIV[1]));

      return aesCBC.doFinal(ciphertext);
   }

   /**
    * Decrypt text which was encrypted with password-based AES-256-CBC as it's done by OpenSSL
    * using the commandline:  "openssl enc -e -aes-256-cbc -a -in filenname.txt"
    *
    * @param encryptedMessageBase64
    * @param password
    * @return decrypted message string
    * @throws GeneralSecurityException
    * @throws UnsupportedEncodingException
    */
   public static String decryptOpenSslAes256Cbc(String encryptedMessageBase64, String password) throws GeneralSecurityException, UnsupportedEncodingException {
      return new String(decryptOpenSslAes256CbcBytes(encryptedMessageBase64, password), "UTF-8");
   }


      /**
       * Java implementation of OpenSSLs "EVP_BytesToKey()".<br/>
       * <br/>
       * This method is used to derive the IV and key for AES-256-CBC decryption from a given password
       * the same way as it's done by OpenSSL when using the commandline:
       *     "openssl enc -e -aes-256-cbc -a -in filenname.txt"
       * <br/><br/>
       * Thanks to Ola Bini for releasing sourcecode for this method on his blog.
       * This implementation is based on the sourcecode obtained from
       * http://olabini.com/blog/tag/evp_bytestokey/ (last accessed at May 08, 2014)
       * where it was released into public domain ("note, I release this into the public domain").
       */
   private static byte[][] openSslEVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data, int iterations) {
      byte[][] keyAndIv = new byte[2][];
      byte[] key = new byte[key_len];
      int key_ix = 0;
      byte[] iv = new byte[iv_len];
      int iv_ix = 0;
      keyAndIv[0] = key;
      keyAndIv[1] = iv;
      byte[] md_buf = null;
      int nkey = key_len;
      int niv = iv_len;
      int i = 0;
      if (data == null) {
         return keyAndIv;
      }
      int addmd = 0;
      for (;;) {
         md.reset();
         if (addmd++ > 0) {
            md.update(md_buf);
         }
         md.update(data);
         if (null != salt) {
            md.update(salt, 0, 8);
         }
         md_buf = md.digest();
         for (i = 1; i < iterations; i++) {
            md.reset();
            md.update(md_buf);
            md_buf = md.digest();
         }
         i = 0;
         if (nkey > 0) {
            for (;;) {
               if (nkey == 0)
                  break;
               if (i == md_buf.length)
                  break;
               key[key_ix++] = md_buf[i];
               nkey--;
               i++;
            }
         }
         if (niv > 0 && i != md_buf.length) {
            for (;;) {
               if (niv == 0)
                  break;
               if (i == md_buf.length)
                  break;
               iv[iv_ix++] = md_buf[i];
               niv--;
               i++;
            }
         }
         if (nkey == 0 && niv == 0) {
            break;
         }
      }
      for (i = 0; i < md_buf.length; i++) {
         md_buf[i] = 0;
      }
      return keyAndIv;
   }
}
