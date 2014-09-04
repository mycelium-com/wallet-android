package com.mycelium.wapi.wallet;

import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.RandomSource;
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher;

/**
 * Secure encrypted storage and plaintext storage for arbitrary binary values using a user defined encryption key.
 * <p/>
 * Provides functionality for storing an encrypted and plaintext component for a given ID
 * <p/>
 * Internally all encrypted values are encrypted with a key encryption key, which in turn is encrypted with a user defined
 * encryption key. This allows re-keying (replacing the user defined key) by only re-encrypting the key encryption key.
 * <p/>Note that the same user defined encryption key is used for all values inserted. The encryption key can be
 * replaced by calling {@link #replaceEncryptionKey(KeyCipher, KeyCipher)}
 * <p/>Note that for every ID an encrypted and a plaintext component can be stored.
 */
public class SecureKeyValueStore {

   private static final byte[] KEK_ID = new byte[]{(byte) 0};
   private static final byte PLAIN_PREFIX = 1;
   private static final byte CIPHER_PREFIX = 2;

   private SecureKeyValueStoreBacking _backing;

   public SecureKeyValueStore(SecureKeyValueStoreBacking backing, RandomSource randomSource) {
      _backing = backing;
      // Initialize key encryption key if necessary
      if (getEncryptedKeyEncryptionKey() == null) {
         byte[] kek = new byte[AesKeyCipher.AES_KEY_BYTE_LENGTH];
         randomSource.nextBytes(kek);
         byte[] encryptedKek = AesKeyCipher.defaultKeyCipher().encrypt(kek);
         storeEncryptedKeyEncryptionKey(encryptedKek);
      }
      Preconditions.checkNotNull(getEncryptedKeyEncryptionKey());
   }

   /**
    * Get the plaintext value of a specified id.
    *
    * @param id The ID to get the value for
    * @return The plaintext value associated with the ID or null if no plaintext value was associated
    */
   public synchronized byte[] getPlaintextValue(byte[] id) {
      if (id.length == 0) {
         throw new RuntimeException("IDs cannot have zero length");
      }
      return _backing.getValue(getRealId(id, false));
   }

   /**
    * Store the plaintext value for a given ID.
    * <p/>
    * If another plaintext value is stored under the same ID  it is overwritten
    *
    * @param id             The id to store a value under
    * @param plaintextValue The value to store
    */
   public synchronized void storePlaintextValue(byte[] id, byte[] plaintextValue) {
      if (id.length == 0) {
         throw new RuntimeException("IDs cannot have zero length");
      }
      _backing.setValue(getRealId(id, false), plaintextValue);
   }

   /**
    * Delete the plain text value associated with an ID
    *
    * @param id the ID of the plain text value to delete
    */
   public void deletePlaintextValue(byte[] id) {
      _backing.deleteValue(getRealId(id, false));
   }

   /**
    * Determine whether the specified user key cipher is the correct one to use for
    * decrypting keys.
    */
   public boolean isValidEncryptionKey(KeyCipher userCipher) {
      try {
         getKeyEncryptionKey(userCipher);
         return true;
      } catch (InvalidKeyCipher invalidKeyCipher) {
         return false;
      }
   }

   /**
    * Re-encrypt all values with another user defined encryption key.
    *
    * @param currentUserCipher the current user encryption key
    * @param newUserCipher     the new user encryption key
    * @throws com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher if the key cipher specified as the current user
    *                                                             encryption key cipher is invalid.
    */
   public synchronized void replaceEncryptionKey(KeyCipher currentUserCipher, KeyCipher newUserCipher)
         throws InvalidKeyCipher {
      // Decrypt the key encryption key using the current user key. If the current user cipher is invalid, this will 
      // throw
      AesKeyCipher kekCipher = getKeyEncryptionKey(currentUserCipher);
      // Encrypt the key encryption key using the new user key
      byte[] newEncryptedKek = newUserCipher.encrypt(kekCipher.getKeyBytes());
      storeEncryptedKeyEncryptionKey(newEncryptedKek);
   }

   /**
    * Determine whether the ciphertext value is present for a given ID without providing the encryption key
    *
    * @param id The ID of the value to probe for.
    * @return true iff a value is defined for the specified ID
    */
   public boolean hasCiphertextValue(byte[] id) {
      return _backing.getValue(getRealId(id, true)) != null;
   }

   /**
    * Get the decrypted ciphertext value associated with a given ID.
    *
    * @param id         The ID of the value to get
    * @param userCipher The user defined encryption key
    * @return The value associated with the specified ID, or null of no value was found
    * @throws InvalidKeyCipher if the specified encryption key is invalid
    */
   public synchronized byte[] getEncryptedValue(byte[] id, KeyCipher userCipher) throws InvalidKeyCipher {
      if (id.length == 0) {
         throw new RuntimeException("IDs cannot have zero length");
      }
      AesKeyCipher kekCipher = getKeyEncryptionKey(userCipher); // may throw InvalidKeyCipher
      byte[] encryptedValue = _backing.getValue(getRealId(id, true));
      if (encryptedValue == null) {
         return null;
      }
      return kekCipher.decrypt(encryptedValue);
   }

   /**
    * Encrypt a value and store it as the ciphertext value under the given ID
    *
    * @param id             the ID to store the value under
    * @param plaintextValue the plaintext value to encrypt and store
    * @param userCipher     the user defined encryption key
    * @throws InvalidKeyCipher if the user defined encryption key is invalid
    */
   public synchronized void encryptAndStoreValue(byte[] id, byte[] plaintextValue, KeyCipher userCipher) throws InvalidKeyCipher {
      if (id.length == 0) {
         throw new RuntimeException("IDs cannot have zero length");
      }
      AesKeyCipher kekCipher = getKeyEncryptionKey(userCipher); // may throw InvalidKeyCipher
      byte[] encryptedValue = kekCipher.encrypt(plaintextValue);
      _backing.setValue(getRealId(id, true), encryptedValue);
   }

   public void deleteEncryptedValue(byte[] id, KeyCipher userCipher) throws InvalidKeyCipher {
      AesKeyCipher kekCipher = getKeyEncryptionKey(userCipher); // may throw InvalidKeyCipher
      _backing.deleteValue(getRealId(id, true));
   }

   private synchronized AesKeyCipher getKeyEncryptionKey(KeyCipher userCipher) throws InvalidKeyCipher {
      byte[] rawKek = userCipher.decrypt(getEncryptedKeyEncryptionKey());
      return new AesKeyCipher(rawKek);
   }

   private byte[] getEncryptedKeyEncryptionKey() {
      return _backing.getValue(KEK_ID);
   }

   private void storeEncryptedKeyEncryptionKey(byte[] encryptedKek) {
      _backing.setValue(KEK_ID, encryptedKek);
   }


   private byte[] getRealId(byte[] id, boolean isEncrypted) {
      byte[] realId = new byte[id.length + 1];
      realId[0] = isEncrypted ? CIPHER_PREFIX : PLAIN_PREFIX;
      System.arraycopy(id, 0, realId, 1, id.length);
      return realId;
   }


}
