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

package com.mrd.bitlib.crypto;

import Rijndael.Rijndael;
import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.io.BaseEncoding;
import com.lambdaworks.crypto.SCrypt;
import com.lambdaworks.crypto.SCryptProgress;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

public class MrdExport {

   private static final byte[] MAGIC_COOKIE = new byte[]{(byte) 0xc4, (byte) 0x49, (byte) 0xdc};
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

      private static final int V1_SALT_LENGTH = 4;
      private static final int V1_HEADER_LENGTH = MAGIC_COOKIE.length + 3 + V1_SALT_LENGTH;
      private static final int V1_CHECKSUM_LENGTH = 4;
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

      public static class ScryptParameters implements Serializable {
         public static final ScryptParameters DEFAULT_PARAMS = new ScryptParameters(14, 8, 1);
         public static final ScryptParameters LOW_MEM_PARAMS = new ScryptParameters(14, 4, 1);


         public final int n;
         public final int r;
         public final int p;

         public ScryptParameters(int n, int r, int p) {
            this.n = n;
            this.r = r;
            this.p = p;
         }

         public ScryptParameters(ScryptParameters scryptParameters) {
            this.n = scryptParameters.n;
            this.r = scryptParameters.r;
            this.p = scryptParameters.p;
         }

         public ScryptParameters(Header header) {
            this.n = header.n;
            this.r = header.r;
            this.p = header.p;
         }
      }

      public static class KdfParameters extends ScryptParameters {
         private static final long serialVersionUID = 1L;

         public String passphrase;
         public byte[] salt;

         private SCryptProgress _scryptProgressTracker;

         public static KdfParameters createNewFromPassphrase(String passphrase, RandomSource rnd, ScryptParameters useScryptParameters) {
            byte[] salt = new byte[V1_SALT_LENGTH];
            rnd.nextBytes(salt);
            return new KdfParameters(passphrase, salt, useScryptParameters);
         }

         public static KdfParameters fromPassphraseAndHeader(String passphrase, Header header) {
            return new KdfParameters(passphrase, header.salt, new ScryptParameters(header));
         }

         protected KdfParameters(String passphrase, byte[] salt, ScryptParameters scryptParameters) {
            super(scryptParameters);
            if (n >= 32) {
               throw new RuntimeException(
                     "Parameter n can never be larger than 31. Note that n = 14 means scrypt with N = 16384");
            }
            this.passphrase = passphrase;
            this.salt = salt;
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
         // 18 - type bit 2
         // 17 - type bit 1
         // 16 - type bit 0
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

         public enum Type {UNCOMPRESSED, COMPRESSED, MASTER_SEED}

         ;

         public int version;
         public NetworkParameters network;
         public Type type;
         public int n;
         public int r;
         public int p;
         public byte[] salt;

         public Header(int version, NetworkParameters network, Type type, int n, int r, int p, byte[] salt) {
            this.version = version;
            this.network = network;
            this.type = type;
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
            byte typeBits; // bits 0,1,2
            switch (type) {
               case UNCOMPRESSED:
                  typeBits = 0;
                  break;
               case COMPRESSED:
                  typeBits = 1;
                  break;
               case MASTER_SEED:
                  typeBits = 2;
                  break;
               default:
                  throw new RuntimeException("Invalid type: " + type.toString());
            }
            bytes[3] = (byte) (versionBits | networkBits | typeBits);

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

            // Type
            int typeBits = bytes[3] & 0x07;
            Type type;
            switch (typeBits) {
               case 0:
                  type = Type.UNCOMPRESSED;
                  break;
               case 1:
                  type = Type.COMPRESSED;
                  break;
               case 2:
                  type = Type.MASTER_SEED;
                  break;
               default:
                  throw new DecodingException();
            }

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
            return new Header(version, network, type, n, r, p, salt);
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
            return version == o.version && network.equals(o.network) && type == o.type && n == o.n
                  && r == o.r && p == o.p;
         }
      }

      public static Header extractHeader(String base64EncryptedPrivateKey) throws DecodingException {
         // Decode data
         byte[] data = base64UrlDecode(base64EncryptedPrivateKey);

         // Decode and verify header
         return Header.fromBytes(data);
      }

      /**
       * Decrypt a private key for either testnet or prodnet using the version 1
       * format.
       *
       * @param parameters                The decryption parameters to use
       * @param base64EncryptedPrivateKey the version 1 encrypted private key
       * @param network                   the Bitcoin network this key is meant for
       * @return The base58 encoded private key
       * @throws DecodingException        if base64EncryptedPrivateKey does not follow the version 1
       *                                  format, or if the type is not a compressed or uncompressed private key
       * @throws WrongNetworkException    if this key was not meant for the specified network
       * @throws InvalidChecksumException if the checksum of the output key does not match the
       *                                  checksum. This happens when the password supplied is
       *                                  incorrect.
       */
      public static String decryptPrivateKey(EncryptionParameters parameters, String base64EncryptedPrivateKey,
                                             NetworkParameters network) throws DecodingException, WrongNetworkException, InvalidChecksumException {

         // Decode data
         byte[] data = base64UrlDecode(base64EncryptedPrivateKey);
         int index = 0;

         // Decode and verify header
         Header header = Header.fromBytes(data);
         index += V1_HEADER_LENGTH;

         if (!network.equals(header.network)) {
            throw new WrongNetworkException();
         }

         // Check that we are working with a private key, and not master seeds
         if (header.type != Header.Type.UNCOMPRESSED && header.type != Header.Type.COMPRESSED) {
            throw new DecodingException();
         }

         // Ciphertext
         byte[] ciphertext = new byte[32];
         System.arraycopy(data, index, ciphertext, 0, 32);
         index += 32;

         // Checksum
         byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
         System.arraycopy(data, index, checksum, 0, V1_CHECKSUM_LENGTH);
         index += V1_CHECKSUM_LENGTH;

         // Decrypt
         byte[] decrypted = decryptBytes(parameters, ciphertext, checksum);

         // Create key
         InMemoryPrivateKey key = new InMemoryPrivateKey(decrypted, header.type == Header.Type.COMPRESSED);

         // Verify checksum
         byte[] checksumVerify = calculatePrivateKeyChecksum(key, network);
         if (!BitUtils.areEqual(checksum, checksumVerify)) {
            throw new InvalidChecksumException();
         }

         // Return as base58 encoded private key
         return key.getBase58EncodedPrivateKey(network);
      }

      /**
       * Decrypt a master seed for either testnet or prodnet using the version 1
       * format.
       *
       * @param parameters                The decryption parameters to use
       * @param base64EncryptedMasterSeed the version 1 encrypted master seed
       * @param network                   the Bitcoin network this key is meant for
       * @return The base58 encoded private key
       * @throws DecodingException        if base64EncryptedMasterSeed does not follow the version 1
       *                                  format, or if the type is not a master seed
       * @throws WrongNetworkException    if this master seed was not meant for the specified network
       * @throws InvalidChecksumException if the checksum of the output master seed does not match the
       *                                  checksum. This happens when the password supplied is
       *                                  incorrect.
       */
      public static Bip39.MasterSeed decryptMasterSeed(EncryptionParameters parameters, String base64EncryptedMasterSeed,
                                                       NetworkParameters network) throws DecodingException, WrongNetworkException, InvalidChecksumException {

         // Decode data
         byte[] data = base64UrlDecode(base64EncryptedMasterSeed);
         int index = 0;

         // Decode and verify header
         Header header = Header.fromBytes(data);
         index += V1_HEADER_LENGTH;

         if (!network.equals(header.network)) {
            throw new WrongNetworkException();
         }

         // Check that we are working with master seeds and not private keys

         if (header.type != Header.Type.MASTER_SEED) {
            throw new DecodingException();
         }

         //
         // Figure out the length of the encrypted seed, must be a multiple of 16 bytes and longer than zero
         int encryptedSeedLength = data.length - V1_HEADER_LENGTH - V1_CHECKSUM_LENGTH;
         if (encryptedSeedLength <= 0 || encryptedSeedLength % Rijndael.BLOCK_SIZE != 0) {
            throw new DecodingException();
         }

         // Ciphertext
         byte[] ciphertext = new byte[encryptedSeedLength];
         System.arraycopy(data, index, ciphertext, 0, encryptedSeedLength);
         index += encryptedSeedLength;

         // Checksum
         byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
         System.arraycopy(data, index, checksum, 0, V1_CHECKSUM_LENGTH);
         index += V1_CHECKSUM_LENGTH;

         // Decrypt
         byte[] decrypted = decryptBytes(parameters, ciphertext, checksum);

         // Create master seed
         Optional<Bip39.MasterSeed> masterSeed = Bip39.MasterSeed.fromBytes(decrypted, true);
         if (!masterSeed.isPresent()) {
            throw new DecodingException();
         }

         // Verify checksum
         byte[] checksumVerify = calculateMasterSeedChecksum(masterSeed.get());
         if (!BitUtils.areEqual(checksum, checksumVerify)) {
            throw new InvalidChecksumException();
         }

         // Return master seed
         return masterSeed.get();
      }

      /**
       * Decrypt a the contained 32 bytes of the version 1 format.
       *
       * @param parameters The decryption parameters to use
       * @param ciphertext the complete version 1 format decoded to bytes
       * @param checksum   the checksum used for initializing the IV
       * @return The decrypted bytes
       */
      private static byte[] decryptBytes(EncryptionParameters parameters, byte[] ciphertext, byte[] checksum) throws InvalidChecksumException {
         // Ciphertext must be a multiple of 16 bytes
         Preconditions.checkArgument(ciphertext.length % V1_BLOCK_CIPHER_LENGTH == 0);

         byte[] decrypted = new byte[ciphertext.length];

         // Create AES initialization vector from checksum
         byte[] IV = new byte[V1_BLOCK_CIPHER_LENGTH];
         byte[] hash = HashUtils.sha256(parameters.salt, checksum).getBytes();
         System.arraycopy(hash, 0, IV, 0, IV.length);

         // Initialize AES key
         Rijndael aes = new Rijndael();
         aes.makeKey(parameters.aesKey, V1_CIPHER_KEY_LENGTH * 8);

         // Use IV as the first cbc block
         byte[] cbcBlock = IV;

         int blocks = ciphertext.length / V1_BLOCK_CIPHER_LENGTH;
         for (int i = 0; i < blocks; i++) {

            // Get first ciphertext block
            byte[] ciphertextBlock = new byte[V1_BLOCK_CIPHER_LENGTH];
            System.arraycopy(ciphertext, i * V1_BLOCK_CIPHER_LENGTH, ciphertextBlock, 0, V1_BLOCK_CIPHER_LENGTH);

            // Decrypt block
            byte[] plaintextBlock = new byte[V1_BLOCK_CIPHER_LENGTH];
            aes.decrypt(ciphertextBlock, plaintextBlock);

            // Xor cbc block
            xorBytes(cbcBlock, plaintextBlock);

            // Copy to result
            System.arraycopy(plaintextBlock, 0, decrypted, i * V1_BLOCK_CIPHER_LENGTH, V1_BLOCK_CIPHER_LENGTH);

            // Use ciphertext block as next cbc block
            cbcBlock = ciphertextBlock;
         }

         // Return result
         return decrypted;
      }

      /**
       * Encrypt a standard Bitcoin private key for either testnet or prodnet
       * using the version 1 format.
       *
       * @param parameters              The encryption parameters to use
       * @param base58EncodedPrivateKey The base58 encoded private key in plain text
       * @param network                 the Bitcoin network used
       * @return The base64 encoded encrypted private key on version 1 format
       */
      public static String encryptPrivateKey(EncryptionParameters parameters, String base58EncodedPrivateKey,
                                             NetworkParameters network) {
         InMemoryPrivateKey key = new InMemoryPrivateKey(base58EncodedPrivateKey, network);

         // Encoded result
         byte[] encoded = new byte[V1_HEADER_LENGTH + 32 + V1_CHECKSUM_LENGTH];
         int index = 0;

         // Type
         Header.Type type = key.getPublicKey().isCompressed() ? Header.Type.COMPRESSED : Header.Type.UNCOMPRESSED;

         // Encode header
         Header h = new Header(V1_VERSION, network, type, parameters.n, parameters.r,
               parameters.p, parameters.salt);
         System.arraycopy(h.toBytes(), 0, encoded, index, V1_HEADER_LENGTH);
         index += V1_HEADER_LENGTH;

         // Calculate checksum
         byte[] checksum = calculatePrivateKeyChecksum(key, network);

         // Encrypt
         byte[] ciphertext = encryptBytes(parameters, key.getPrivateKeyBytes(), checksum);

         // Copy encrypted form to encoding
         System.arraycopy(ciphertext, 0, encoded, index, 16);
         index += 16;
         System.arraycopy(ciphertext, 16, encoded, index, 16);
         index += 16;

         // Add checksum
         System.arraycopy(checksum, 0, encoded, index, checksum.length);

         // Base58 encode
         String result = base64UrlEncode(encoded);
         return result;
      }


      /**
       * Encrypt a BIP32 master seed for either testnet or prodnet
       * using the version 1 format.
       *
       * @param parameters The encryption parameters to use
       * @param masterSeed The BIP39 master seed
       * @param network    the Bitcoin network used
       * @return The base64 encoded encrypted private key on version 1 format
       */
      public static String encryptMasterSeed(EncryptionParameters parameters, Bip39.MasterSeed masterSeed,
                                             NetworkParameters network) {
         // Encoded result
         int index = 0;

         // Turn master seed into compressed binary form
         byte[] compressedMasterSeed = masterSeed.toBytes(true);
         byte[] paddedPlaintext = addZeroPadding(compressedMasterSeed);

         // Type
         Header.Type type = Header.Type.MASTER_SEED;

         byte[] encoded = new byte[V1_HEADER_LENGTH + paddedPlaintext.length + V1_CHECKSUM_LENGTH];

         // Encode header
         Header h = new Header(V1_VERSION, network, type, parameters.n, parameters.r,
               parameters.p, parameters.salt);
         System.arraycopy(h.toBytes(), 0, encoded, index, V1_HEADER_LENGTH);
         index += V1_HEADER_LENGTH;

         // Calculate checksum
         byte[] checksum = calculateMasterSeedChecksum(masterSeed);

         // Encrypt
         byte[] ciphertext = encryptBytes(parameters, paddedPlaintext, checksum);

         // Copy encrypted form to encoding
         System.arraycopy(ciphertext, 0, encoded, index, paddedPlaintext.length);
         index += paddedPlaintext.length;

         // Add checksum
         System.arraycopy(checksum, 0, encoded, index, checksum.length);

         // Base58 encode
         String result = base64UrlEncode(encoded);
         return result;
      }

      private static byte[] addZeroPadding(byte[] data) {
         ByteWriter writer = new ByteWriter(data.length + Rijndael.BLOCK_SIZE);
         writer.putBytes(data);
         int excess = writer.length() % Rijndael.BLOCK_SIZE;
         if (excess == 0) {
            return writer.toBytes();
         }
         return BitUtils.copyOf(writer.toBytes(), writer.length() + Rijndael.BLOCK_SIZE - excess);
      }


      /**
       * Encrypt a multiple of 16 bytes using CBC mode and basing the IV on the checksum from our version 1 format.
       *
       * @param parameters The encryption parameters to use
       * @param plaintext  The plaintext data to encrypt
       * @param checksum   the checksum used for initializing the IV
       * @return the ciphertext
       */
      private static byte[] encryptBytes(EncryptionParameters parameters, byte[] plaintext, byte[] checksum) {
         // Plaintext must be a multiple of 16 bytes
         Preconditions.checkArgument(plaintext.length % 16 == 0);

         // Buffer for encrypted result
         byte[] encrypted = new byte[plaintext.length];

         // Create AES initialization vector from checksum
         byte[] IV = new byte[V1_BLOCK_CIPHER_LENGTH];
         byte[] hash = HashUtils.sha256(parameters.salt, checksum).getBytes();
         System.arraycopy(hash, 0, IV, 0, V1_BLOCK_CIPHER_LENGTH);

         // Initialize AES key
         Rijndael aes = new Rijndael();
         aes.makeKey(parameters.aesKey, V1_CIPHER_KEY_LENGTH * 8);

         // Use IV as the first cbc block
         byte[] cbcBlock = IV;

         int blocks = plaintext.length / 16;
         for (int i = 0; i < blocks; i++) {

            // Get plaintext block
            byte[] plaintextBlock = new byte[16];
            System.arraycopy(plaintext, i * 16, plaintextBlock, 0, 16);

            // Xor cbc block
            xorBytes(cbcBlock, plaintextBlock);

            // Encrypt
            byte[] ciphertextBlock = new byte[16];
            aes.encrypt(plaintextBlock, ciphertextBlock);

            // Copy to result
            System.arraycopy(ciphertextBlock, 0, encrypted, i * 16, 16);

            // Use ciphertext block as next cbc block
            cbcBlock = ciphertextBlock;
         }
         return encrypted;
      }

      /**
       * The alfabet used when generating passwords. It only contains characters
       * [a-z]. This lowers the entropy for each character but makes it easier
       * to enter on a mobile device.
       */
      private static char[] ALPHABET = new char[]{'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
            'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

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
       * <p/>
       * The password is 15 characters long, which with the the current alphabet
       * produces 70-bit passwords
       *
       * @param randomSource the random source to base the password on
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
         Sha256Hash hash;
         try {
            hash = HashUtils.sha256(password.getBytes(PASSWORD_CHARACTER_ENCODING));
         } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new RuntimeException(e);
         }
         // Regard first four bytes as a positive integer
         long asInteger = BitUtils.uint32ToLong(hash.firstFourBytes(), 0);

         // Find the corresponding index in our alphabet, this is out checksum
         int index = (int) (asInteger % ALPHABET.length);
         return ALPHABET[index];
      }

      /**
       * Calculate the checksum from sha256 hash of the bitcoin address of a
       * private key
       */
      private static byte[] calculatePrivateKeyChecksum(InMemoryPrivateKey key, NetworkParameters network) {
         try {
            String address = key.getPublicKey().toAddress(network).toString();
            byte[] hash = HashUtils.sha256(address.getBytes("US-ASCII")).getBytes();
            byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
            System.arraycopy(hash, 0, checksum, 0, V1_CHECKSUM_LENGTH);
            return checksum;
         } catch (UnsupportedEncodingException e) {
            // Never happens
            throw new RuntimeException(e);
         }
      }

      /**
       * Calculate the checksum from double sha256 hash of a master seed
       */
      private static byte[] calculateMasterSeedChecksum(Bip39.MasterSeed masterSeed) {
         byte[] hash = HashUtils.doubleSha256(masterSeed.getBip32Seed()).getBytes();
         byte[] checksum = new byte[V1_CHECKSUM_LENGTH];
         System.arraycopy(hash, 0, checksum, 0, V1_CHECKSUM_LENGTH);
         return checksum;
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
