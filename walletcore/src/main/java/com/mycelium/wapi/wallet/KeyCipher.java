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

package com.mycelium.wapi.wallet;

/**
 * A key cipher allows encrypting and decrypting of arbitrary data with
 * integrity checks.
 */
public interface KeyCipher {

   public class InvalidKeyCipher extends Exception {
      private static final long serialVersionUID = 1L;
   }

   /**
    * Get the thumbprint of this key cipher
    * 
    * @return the thumbprint of this key cipher
    */
   public long getThumbprint();

   /**
    * Decrypt an array of bytes
    * 
    * @param data
    *           the data to decrypt
    * @return the decrypted data
    * @throws InvalidKeyCipher
    *            If the integrity check failed while decrypting
    */
   public byte[] decrypt(byte[] data) throws InvalidKeyCipher;

   /**
    * Encrypt an array of bytes
    * 
    * @param data
    *           the data to encrypt
    * @return the encrypted data
    */
   public byte[] encrypt(byte[] data);
}