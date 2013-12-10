/*
 * Copyright 2013 Megion Research & Development GmbH
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

package com.mrd.bitlib.crypto;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

import Rijndael.Rijndael;

import com.google.common.io.BaseEncoding;
import com.lambdaworks.crypto.SCrypt;
import com.lambdaworks.crypto.SCryptProgress;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;

public class MrdExport {

   private static final byte[] MAGIC_COOKIE = new byte[] { (byte) 0xc4, (byte) 0x49, (byte) 0xdc };
   public static final int V1_VERSION = 1;

   public static boolean isChecksumValid(String enteredText) {
      if (enteredText.length() != V1.V1_PASSPHRASE_LENGTH + 1) {
         return false;
      }
      String password = enteredText.substring(0, V1.V1_PASSPHRASE_LENGTH);
      char chechsumChar = V1.calculatePasswordChecksum(password);
      return Character.toUpperCase(chechsumChar) == enteredText.charAt(V1.V1_PASSPHRASE_LENGTH);
   }

   public static class DecodingException extends Exception {
      // todo consider refactoring this into a composite return value instead of
      // an exception. it is not really "exceptional"
      private static final long serialVersionUID = 1L;
   }

   public static int decodeVersion(String base64EncryptedPrivateKey) throws DecodingException {
      byte[] data;
      data = base64UrlDecode(base64EncryptedPrivateKey);

      // Check for minimum length
      if (data.length < 4) {
         throw new DecodingException();
      }

      // Check magic cookie
      if (data[0] != MAGIC_COOKIE[0] || data[1] != MAGIC_COOKIE[1] || data[2] != MAGIC_COOKIE[2]) {
         throw new DecodingException();
      }

      int version = (data[3] >> 4) & 0x0F;
      return version;
   }

   public static class V1 {
      public static final String PASSWORD_CHARACTER_ENCODING = "US-ASCII";
      public static final int V1_PASSPHRASE_LENGTH = 15;

      public static final int DEFAULT_SCRYPT_N = 14;
      public static final int DEFAULT_SCRYPT_R = 8;
      public static final int DEFAULT_SCRYPT_P = 1;

      private static final int V1_SALT_LENGTH = 4;
      private static final int V1_HEADER_LENGTH = MAGIC_COOKIE.length + 3 + V1_SALT_LENGTH;
      private static final int V1_CHECKSUM_LENGTH = 4;
      private static final int V1_LENGTH = V1_HEADER_LENGTH + 32 + V1_CHECKSUM_LENGTH;
      private static final int V1_BLOCK_CIPHER_LENGTH = 16;
      public static final int V1_CIPHER_KEY_LENGTH = 32;

      public static class WrongNetworkException extends DecodingException {
         private static final long serialVersionUID = 1L;
      }

      public static class InvalidChecksumException extends DecodingException {
         // todo consider refactoring this into a composite return value instead
         // of an exception. it is not really "exceptional"
         private static final long serialVersionUID = 1L;
      }

      /**
       * Encryption and decryption parameters used with the version 1 format
       */
      public static class EncryptionParameters implements Serializable {
         private static final long serialVersionUID = 1L;

         public int n;
         public int r;
         public int p;
         public byte[] salt;

         /**
          * The AES key used for encryption and decryption
          */
         public byte[] aesKey;

         public EncryptionParameters(byte[] aesKey, KdfParameters kdfParameters) {
            n = kdfParameters.n;
            r = kdfParameters.r;
            p = kdfParameters.p;
            salt = kdfParameters.salt;
            this.aesKey = aesKey;
         }

         /**
          * Generate encryption parameters using given params including progress tracker
          *
          * @param p KeyDerivationParameters including progress tracker
          * @throws InterruptedException
          * @throws java.lang.OutOfMemoryError if the heap is not large enough. catch this if you want to provide a fallback.
          */
         public static EncryptionParameters generate(KdfParameters p) throws InterruptedException, OutOfMemoryError {
            try {
               // Derive AES Key using scrypt on passphrase and salt
               byte[] aesKey = SCrypt.scrypt(p.passphrase.getBytes(PASSWORD_CHARACTER_ENCODING), p.salt, 1 << p.n, p.r,
                     p.p, V1_CIPHER_KEY_LENGTH, p._scryptProgressTracker);
               return new EncryptionParameters(aesKey, p);
            } catch (InterruptedException e) {
               throw e;
            } catch (GeneralSecurityException e) {
               throw new RuntimeException(e);
            } catch (UnsupportedEncodingException e) {
               throw new RuntimeException(e);
            }
         }
      }

      public static class KdfParameters implements Serializable {
         private static final long serialVersionUID = 1L;

         public String passphrase;
         public byte[] salt;
         public int n;
         public int r;
         public int p;

         private SCryptProgress _scryptProgressTracker;

         public static KdfParameters createNewFromPassphrase(String passphrase, RandomSource rnd) {
            byte[] salt = new byte[V1_SALT_LENGTH];
            rnd.nextBytes(salt);
            return new KdfParameters(passphrase, salt, MrdExport.V1.DEFAULT_SCRYPT_N, MrdExport.V1.DEFAULT_SCRYPT_R,
                  MrdExport.V1.DEFAULT_SCRYPT_P);
         }

         public static KdfParameters fromPassphraseAndHeader(String passphrase, Header header) {
            return new KdfParameters(passphrase, header.salt, header.n, header.r, header.p);
         }

         protected KdfParameters(String passphrase, byte[] salt, int n, int r, int p) {
            if (n >= 32) {
               throw new RuntimeException(
                     "Parameter n can never be larger than 31. Note that n = 14 means scrypt with N = 16384");
            }
            this.passphrase = passphrase;
            this.salt = salt;
            this.n = n;
            this.r = r;
            this.p = p;
            _scryptProgressTracker = new SCryptProgress(1 << n, r, p);
         }

         public double getProgress() {
            return _scryptProgressTracker.getProgress();
         }

         public void terminate() {
            _scryptProgressTracker.terminate();
         }
      }

      /**
       * The 3 byte header of the V1 format
       */
      public static class Header {

         // The header contains:
         // 3 bytes magic cookie {0x4c, 0x49, 0xdc}
         // 3 bytes version number and flags
         // 4 bytes salt
         //
         //
         // The 24 Bits for version number and flags
         // 23 - version bit 3
         // 22 - version bit 2
         // 21 - version bit 1
         // 20 - version bit 0
         // 19 - network bit
         // 18 - reserved = 0
         // 17 - reserved = 0
         // 16 - compression bit
         // 15 - n bit 4
         // 14 - n bit 3
         // 13 - n bit 2
         // 12 - n bit 1
         // 11 - n bit 0
         // 10 - r bit 4
         // 9 - r bit 3
         // 8 - r bit 2
         // 7 - r bit 1
         // 6 - r bit 0
         // 5 - p bit 4
         // 4 - p bit 3
         // 3 - p bit 2
         // 2 - p bit 1
         // 1 - p bit 0
         // 0 - reserved = 0

         public int version;
         public NetworkParameters network;
         public boolean compressed;
         public int n;
         public int r;
         public int p;
         public byte[] salt;

         public Header(int version, NetworkParameters network, boolean compressed, int n, int r, int p, byte[] salt) {
            this.version = version;
            this.network = network;
            this.compressed = compressed;
            this.n = n;
            this.r = r;
            this.p = p;
            this.salt = salt;
            if (version != 1) {
               throw new RuntimeException("Unsupported version number");
            }
            if (n < 0 || n > 31 || r < 1 || r > 31 || p < 1 || p > 31) {
               throw new RuntimeException("SCrypt parameters out of range");
            }
         }

         public byte[] toBytes() {
            byte[] bytes = new byte[V1_HEADER_LENGTH];

            // Add magic cookie
            bytes[0] = MAGIC_COOKIE[0];
            bytes[1] = MAGIC_COOKIE[1];
            bytes[2] = MAGIC_COOKIE[2];

            // First header byte (bits 23-16)
            byte versionBits = ((byte) V1_VERSION) << 4; // bits 4, 5, 6, 7
            byte networkBits = (byte) (network.isProdnet() ? 0 : 8); // bit 3
            byte compressedBits = (byte) (compressed ? 1 : 0); // bit 0
            bytes[3] = (byte) (versionBits | networkBits | compressedBits);

            // Second header byte (bits 15-8)
            // Containing the 5 n bits and the three top bits of the the 5 r
            // bits
            byte nBits = (byte) (((byte) n) << 3); // bits 3, 4, 5, 6, 7
            byte rBitsTop = (byte) (((byte) r) >> 2); // bits 0, 1, 2
            bytes[4] = (byte) (nBits | rBitsTop);

            // Third header byte (bits 15-8)
            // Containing the remaining 2 r bits and the 5 p bits
            byte rBitsBot = (byte) (((byte) r) << 6); // bits 6, 7
            byte pBits = (byte) (((byte) p) << 1); // bits 1, 2, 3, 4, 5
            bytes[5] = (byte) (rBitsBot | pBits);

            // Add the salt
            System.arraycopy(salt, 0, bytes, 6, V1_SALT_LENGTH);

            return bytes;
         }

         public static Header fromBytes(byte[] bytes) throws DecodingException {
            if (bytes.length < V1_HEADER_LENGTH) {
               throw new DecodingException();
            }

            // Validate Magic cookie
            if (bytes[0] != MAGIC_COOKIE[0] || bytes[1] != MAGIC_COOKIE[1] || bytes[2] != MAGIC_COOKIE[2]) {
               throw new DecodingException();
            }

            // Version
            int version = (bytes[3] >> 4) & 0x0F; // bits 4, 5, 6, 7
            if (version != 1) {
               throw new DecodingException();
            }

            // Get Network (bit 3)
            NetworkParameters network = (bytes[3] & 0x08) == 0x08 ? NetworkParameters.testNetwork
                  : NetworkParameters.productionNetwork;

            // Validate reserved (bits 1, 2 of byte 0)
            if ((bytes[3] & 0x06) != 0) {
               throw new DecodingException();
            }

            // Get Compressed public key (bit 0)
            boolean compressed = (bytes[3] & 0x01) == 0x01;

            // Get n
            int n = (bytes[4] >> 3) & 0x1F; // bits 3, 4, 5, 6, 7

            // Get r
            int r = (bytes[4] << 2) & 0x1F; // bits 0, 1, 2 of byte 1
            r += ((bytes[5] >> 6) & 0x03);// and bits 7, 6 of byte 2

            // Get p
            int p = (bytes[5] >> 1) & 0x1F; // bits 1, 2, 3, 4, 5

            // Validate reserved (bit 0 of byte 2)
            if ((bytes[5] & 0x01) != 0) {
               throw new DecodingException();
            }

            // Get the salt, byte index 6, 7, 8, 9
            byte[] salt = new byte[V1_SALT_LENGTH];
            System.arraycopy(bytes, 6, salt, 0, V1_SALT_LENGTH);
            return new Header(version, network, compressed, n, r, p, salt);
         }

         @Override
         public boolean equals(Object obj) {
            if (obj == this) {
               return true;
            }
            if (!(obj instanceof Header)) {
               return false;
            }
            Header o = (Header) obj;
            return version == o.version && network.equals(o.network) && compressed == o.compressed && n == o.n
                  && r == o.r && p == o.p;
         }
      }

      public static Header extractHeader(String base64EncryptedPrivateKey) throws DecodingException {
         // Decode data
         byte[] data = base64UrlDecode(base64EncryptedPrivateKey);
         if (data.length != V1_LENGTH) {
            throw new DecodingException();
         }

         // Decode and verify header
         return Header.fromBytes(data);
      }

      /**
       * Decrypt a private key for either testnet or prodnet using the version 1
       * format.
       * 
       * @param parameters
       *           The decryption parameters to use
       * @param base64EncryptedPrivateKey
       *           the version 1 encrypted private key
       * @param network
       *           the Bitcoin network this key is meant for
       * @return The base58 encoded private key
       * @throws DecodingException
       *            if base64EncryptedPrivateKey does not follow the version 1
       *            format
       * @throws WrongNetworkException
       *            if this key was not meant for the specified network
       * @throws InvalidChecksumException
       *            if the checksum of the output key does not match the
       *            checksum. This happens when the password supplied is
       *            incorrect.
       */
      public static String decrypt(EncryptionParameters parameters, String base64EncryptedPrivateKey,
            NetworkParameters network) throws DecodingException, WrongNetworkException, InvalidChecksumException {

         // Decode data
         byte[] data = base64UrlDecode(base64EncryptedPrivateKey);
         if (data.length != V1_LENGTH) {
            throw new DecodingException();
         }
         int index = 0;

         // Decode and verify header
         Header header = Header.fromBytes(data);
         index += V1_HEADER_LENGTH;

         if (!network.equals(header.network)) {
            throw new WrongNetworkException();
         }

         // Copy encrypted data to blocks
         byte[] ciphertextBlock1 = new byte[16];
         System.arraycopy(data, index, ciphertextBlock1, 0, 16);
         index += 16;
         byte[] ciphertextBlock2 = new byte[16];
         System.arraycopy(data, index, ciphertextBlock2, 0, 16);
         index += 16;

         // Checksum
         byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
         System.arraycopy(data, index, checksum, 0, checksum.length);
         index += checksum.length;

         // Create AES initialization vector from checksum
         byte[] IV = new byte[V1_BLOCK_CIPHER_LENGTH];
         byte[] hash = HashUtils.sha256(parameters.salt, checksum);
         System.arraycopy(hash, 0, IV, 0, IV.length);

         // Initialize AES key
         Rijndael aes = new Rijndael();
         aes.makeKey(parameters.aesKey, V1_CIPHER_KEY_LENGTH * 8);

         // Decrypt block 1
         byte[] plaintextBlock1 = new byte[V1_BLOCK_CIPHER_LENGTH];
         aes.decrypt(ciphertextBlock1, plaintextBlock1);

         // Apply IV to plaintext block 1 (Cipher block chaining)
         xorBytes(IV, plaintextBlock1);

         // Decrypt block 2
         byte[] plaintextBlock2 = new byte[V1_BLOCK_CIPHER_LENGTH];
         aes.decrypt(ciphertextBlock2, plaintextBlock2);

         // Apply ciphertext block 1 on plaintext block 2 (Cipher block
         // chaining)
         xorBytes(ciphertextBlock1, plaintextBlock2);

         // Concatenate plaintext blocks
         byte[] privateKeyBytes = new byte[32];
         System.arraycopy(plaintextBlock1, 0, privateKeyBytes, 0, 16);
         System.arraycopy(plaintextBlock2, 0, privateKeyBytes, 16, 16);

         // Create key
         InMemoryPrivateKey key = new InMemoryPrivateKey(privateKeyBytes, header.compressed);

         // Verify checksum
         byte[] checksumVerify = calculateChecksum(key, network);
         if (!BitUtils.areEqual(checksum, checksumVerify)) {
            throw new InvalidChecksumException();
         }

         // Return as base58 encoded private key
         return key.getBase58EncodedPrivateKey(network);
      }

      /**
       * Encrypt a standard Bitcoin private key for either testnet or prodnet
       * using the version 1 format.
       * 
       * @param parameters
       *           The encryption parameters to use
       * @param base58EncodedPrivateKey
       *           The base58 encoded private key in plain text
       * @param network
       *           the Bitcoin network used
       * @return The base64 encoded encrypted private key on version 1 format
       */
      public static String encrypt(EncryptionParameters parameters, String base58EncodedPrivateKey,
            NetworkParameters network) {
         InMemoryPrivateKey key = new InMemoryPrivateKey(base58EncodedPrivateKey, network);

         // Encoded result
         byte[] encoded = new byte[V1_LENGTH];
         int index = 0;

         // Encode header
         Header h = new Header(V1_VERSION, network, key.getPublicKey().isCompressed(), parameters.n, parameters.r,
               parameters.p, parameters.salt);
         System.arraycopy(h.toBytes(), 0, encoded, index, V1_HEADER_LENGTH);
         index += V1_HEADER_LENGTH;

         // Calculate checksum
         byte[] checksum = calculateChecksum(key, network);

         // Create AES initialization vector from checksum
         byte[] IV = new byte[V1_BLOCK_CIPHER_LENGTH];
         byte[] hash = HashUtils.sha256(parameters.salt, checksum);
         System.arraycopy(hash, 0, IV, 0, V1_BLOCK_CIPHER_LENGTH);

         // Initialize AES key
         Rijndael aes = new Rijndael();
         aes.makeKey(parameters.aesKey, V1_CIPHER_KEY_LENGTH * 8);

         // Get private key bytes and copy to blocks
         byte[] complete = key.getPrivateKeyBytes();
         byte[] plaintextBlock1 = new byte[16];
         System.arraycopy(complete, 0, plaintextBlock1, 0, 16);
         byte[] plaintextBlock2 = new byte[16];
         System.arraycopy(complete, 16, plaintextBlock2, 0, 16);

         // Apply IV to plaintext block 1 (Cipher block chaining)
         xorBytes(IV, plaintextBlock1);

         // Encrypt block 1
         byte[] ciphertextBlock1 = new byte[16];
         aes.encrypt(plaintextBlock1, ciphertextBlock1);

         // Apply ciphertext block 1 on plaintext block 2 (Cipher block
         // chaining)
         xorBytes(ciphertextBlock1, plaintextBlock2);

         // Encrypt block 2
         byte[] ciphertextBlock2 = new byte[16];
         aes.encrypt(plaintextBlock2, ciphertextBlock2);

         // Copy encrypted form to encoding
         System.arraycopy(ciphertextBlock1, 0, encoded, index, 16);
         index += 16;
         System.arraycopy(ciphertextBlock2, 0, encoded, index, 16);
         index += 16;

         // Add checksum
         System.arraycopy(checksum, 0, encoded, index, checksum.length);

         // Base58 encode
         String result = base64UrlEncode(encoded);
         return result;
      }

      /**
       * The alfabet used when generating passwords. It only contains characters
       * [a-z]. This lowers the entropy for each character but makes it easier
       * to enter on a mobile device.
       */
      private static char[] ALPHABET = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z' };

      /**
       * Concatenate the alphabet as many whole times as possible while staying
       * below 256 characters. With 26 password characters this is 9 times and
       * produces a 234 character length array.
       * 
       * @return
       */
      private static char[] getExtendedAlphabet() {
         int repetitions = 256 / ALPHABET.length;
         char[] extendedAlphabet = new char[repetitions * ALPHABET.length];
         for (int i = 0; i < repetitions; i++) {
            System.arraycopy(ALPHABET, 0, extendedAlphabet, i * ALPHABET.length, ALPHABET.length);
         }
         return extendedAlphabet;
      }

      /**
       * Generate a random fixed-length password using characters [a-z]
       * <p>
       * The password is 15 characters long, which with the the current alphabet
       * produces 70-bit passwords
       * 
       * @param randomSource
       *           the random source to base the password on
       */
      public static String generatePassword(RandomSource randomSource) {
         char[] extendedAlphabet = getExtendedAlphabet();
         char[] passphrase = new char[V1_PASSPHRASE_LENGTH];
         byte[] one = new byte[1];
         for (int i = 0; i < V1_PASSPHRASE_LENGTH; i++) {
            int index;
            // Get a random index between 0 and 255 until it is below or equal
            // to the extended alphabet length
            do {
               // Get an integer between 0 and 255
               randomSource.nextBytes(one);
               index = ((int) one[0]) & 0xFF;
            } while (index > extendedAlphabet.length - 1);
            passphrase[i] = extendedAlphabet[index];
         }
         return new String(passphrase);
      }

      /**
       * Calculate a simple checksum from a password
       */
      public static char calculatePasswordChecksum(String password) {
         // Calculate the SHA256 has of the password
         byte[] hash;
         try {
            hash = HashUtils.sha256(password.getBytes(PASSWORD_CHARACTER_ENCODING));
         } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new RuntimeException(e);
         }
         // Regard first four bytes as a positive integer
         long asInteger = BitUtils.uint32ToLong(hash, 0);

         // Find the corresponding index in our alphabet, this is out checksum
         int index = (int) (asInteger % ALPHABET.length);
         return ALPHABET[index];
      }

      /**
       * Calculate the checksum from sha256 hash of the bitcoin address of a
       * private key
       */
      private static byte[] calculateChecksum(InMemoryPrivateKey key, NetworkParameters network) {
         try {
            byte[] hash = HashUtils.sha256(Address.fromStandardPublicKey(key.getPublicKey(), network).toString()
                  .getBytes("US-ASCII"));
            byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
            System.arraycopy(hash, 0, checksum, 0, V1_CHECKSUM_LENGTH);
            return checksum;
         } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new RuntimeException(e);
         }
      }

   }

   private static void xorBytes(byte[] toApply, byte[] target) {
      if (toApply.length != target.length) {
         throw new RuntimeException();
      }
      for (int i = 0; i < toApply.length; i++) {
         target[i] = (byte) (target[i] ^ toApply[i]);
      }
   }

   private static String base64UrlEncode(byte[] data) {
      return BaseEncoding.base64Url().omitPadding().encode(data);
   }

   private static byte[] base64UrlDecode(String base64String) throws DecodingException {
      try {
         return BaseEncoding.base64Url().decode(base64String);
      } catch (IllegalArgumentException e) {
         throw new DecodingException();
      }
   }

}
