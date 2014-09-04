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