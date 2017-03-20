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

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.util.List;
import java.util.UUID;

import com.google.bitcoinj.Base58;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.crypto.ec.Parameters;
import com.mrd.bitlib.crypto.ec.Point;
import com.mrd.bitlib.model.hdpath.HdKeyPath;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

/**
 * Implementation of BIP 32 HD wallet key derivation.
 * <p>
 * https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki
 */
public class HdKeyNode implements Serializable {

   public static final int HARDENED_MARKER = 0x80000000;

   public static class KeyGenerationException extends RuntimeException {
      private static final long serialVersionUID = 1L;

      public KeyGenerationException(String message) {
         super(message);
      }
   }

   private static final String BITCOIN_SEED = "Bitcoin seed";
   private static final int CHAIN_CODE_SIZE = 32;

   private final InMemoryPrivateKey _privateKey;
   private final PublicKey _publicKey;
   private final byte[] _chainCode;
   private final int _depth;
   private final int _parentFingerprint;
   private final int _index;

   /**
    * Convert to custom fast parsable byte format. XXX This is very much
    * experimental
    */
   public void toCustomByteFormat(ByteWriter writer) {
      if (isPrivateHdKeyNode()) {
         writer.put((byte) 1);
         Preconditions.checkArgument(_privateKey.getPrivateKeyBytes().length == 32);
         writer.putBytes(_privateKey.getPrivateKeyBytes());
      } else {
         writer.put((byte) 0);
      }
      Preconditions.checkArgument(_publicKey.getPublicKeyBytes().length == 33);
      writer.putBytes(_publicKey.getPublicKeyBytes());
      writer.putBytes(_chainCode);
      writer.putIntLE(_depth);
      writer.putIntLE(_parentFingerprint);
      writer.putIntLE(_index);
   }

   /**
    * Convert to custom fast parsable byte format. XXX This is very much
    * experimental
    */
   public byte[] toCustomByteFormat() {
      ByteWriter writer = new ByteWriter(1024);
      toCustomByteFormat(writer);
      return writer.toBytes();
   }

   /**
    * Create from custom fast parsable byte format. XXX This is very much
    * experimental
    */
   public static HdKeyNode fromCustomByteformat(byte[] bytes) throws InsufficientBytesException {
      return fromCustomByteformat(new ByteReader(bytes));
   }

   /**
    * Create from custom fast parsable byte format. XXX This is very much
    * experimental
    */
   public static HdKeyNode fromCustomByteformat(ByteReader reader) throws InsufficientBytesException {
      boolean hasPrivateKey = reader.get() == 1;
      if (hasPrivateKey) {
         // Private key node
         InMemoryPrivateKey privateKey = new InMemoryPrivateKey(reader.getBytes(32), reader.getBytes(33));
         return new HdKeyNode(privateKey, reader.getBytes(CHAIN_CODE_SIZE), reader.getIntLE(), reader.getIntLE(),
               reader.getIntLE());
      } else {
         // Public key node
         return new HdKeyNode(new PublicKey(reader.getBytes(33)), reader.getBytes(CHAIN_CODE_SIZE), reader.getIntLE(),
               reader.getIntLE(), reader.getIntLE());
      }
   }

   HdKeyNode(InMemoryPrivateKey privateKey, byte[] chainCode, int depth, int parentFingerprint, int index) {
      _privateKey = privateKey;
      _publicKey = _privateKey.getPublicKey();
      _chainCode = chainCode;
      _depth = depth;
      _parentFingerprint = parentFingerprint;
      _index = index;
   }

   public HdKeyNode(PublicKey publicKey, byte[] chainCode, int depth, int parentFingerprint, int index) {
      _privateKey = null;
      _publicKey = publicKey;
      _chainCode = chainCode;
      _depth = depth;
      _parentFingerprint = parentFingerprint;
      _index = index;
   }

   /**
    * Generate a master HD key node from a seed.
    * 
    * @param seed
    *           the seed to generate the master HD wallet key from.
    * @return a master HD key node for the seed
    * @throws KeyGenerationException
    *            if the seed is not suitable for seeding an HD wallet key
    *            generation. This is extremely unlikely
    */
   public static HdKeyNode fromSeed(byte[] seed) throws KeyGenerationException {
      Preconditions.checkArgument(seed.length * 8 >= 128, "seed must be larger than 128");
      Preconditions.checkArgument(seed.length * 8 <= 512, "seed must be smaller than 512");
      byte[] I = Hmac.hmacSha512(asciiStringToBytes(BITCOIN_SEED), seed);

      // Construct private key
      byte[] IL = BitUtils.copyOfRange(I, 0, 32);
      BigInteger k = new BigInteger(1, IL);
      if (k.compareTo(Parameters.n) >= 0) {
         throw new KeyGenerationException(
               "An unlikely thing happened: The derived key is larger than the N modulus of the curve");
      }
      if (k.equals(BigInteger.ZERO)) {
         throw new KeyGenerationException("An unlikely thing happened: The derived key is zero");
      }
      InMemoryPrivateKey privateKey = new InMemoryPrivateKey(IL, true);

      // Construct chain code
      byte[] IR = BitUtils.copyOfRange(I, 32, 32 + CHAIN_CODE_SIZE);
      return new HdKeyNode(privateKey, IR, 0, 0, 0);
   }

   /**
    * Is this a public or private key node.
    * <p>
    * A private key node can generate both public and private key hierarchies. A
    * public key node can only generate the corresponding public key
    * hierarchies.
    * 
    * @return true if this is a private key node, false otherwise.
    */
   public boolean isPrivateHdKeyNode() {
      return _privateKey != null;
   }

   /**
    * If this is a private key node, return the corresponding public key node of
    * this node, otherwise return a copy of this node.
    */
   public HdKeyNode getPublicNode() {
      return new HdKeyNode(_publicKey, _chainCode, _depth, _parentFingerprint, _index);
   }

   /**
    * Create the child private key of this node with the corresponding index.
    * 
    * @param index
    *           the index to use
    * @return the private key corresponding to the specified index
    * @throws KeyGenerationException
    *            if this is not a private key node, or if no key can be created
    *            for this index (extremely unlikely)
    */
   public InMemoryPrivateKey createChildPrivateKey(int index) throws KeyGenerationException {
      if (!isPrivateHdKeyNode()) {
         throw new KeyGenerationException("Not a private HD key node");
      }
      return createChildNode(index)._privateKey;
   }

   /**
    * Create the child public key of this node with the corresponding index.
    * 
    * @param index
    *           the index to use
    * @return the public key corresponding to the specified index
    * @throws KeyGenerationException
    *            if this is a public key node which is hardened, or if no key
    *            can be created for this index (extremely unlikely)
    */
   public PublicKey createChildPublicKey(int index) throws KeyGenerationException {
      return createChildNode(index)._publicKey;
   }


   /**
    * Create the Bip32 derived child from this KeyNode, according to the keyPath.
    *
    * @param keyPath
    *           the Bip32 Path
    * @return the child node corresponding to the current node + keyPath
    */
   public HdKeyNode createChildNode(HdKeyPath keyPath){
      List<Integer> addrN = keyPath.getAddressN();
      HdKeyNode ak = this;
      for (Integer i : addrN){
         ak = ak.createChildNode(i);
      }
      return ak;
   }

   /**
    * Create the hardened child node of this node with the corresponding index
    *
    * @param index
    *           the index to use
    * @return the child node corresponding to the specified index
    * @throws KeyGenerationException
    *            if this is a public key node which is hardened, or if no key
    *            can be created for this index (extremely unlikely)
    */
   public HdKeyNode createHardenedChildNode(int index) throws KeyGenerationException {
      return createChildNode(index | HARDENED_MARKER);
   }

   /**
    * Create the child node of this node with the corresponding index
    * 
    * @param index
    *           the index to use
    * @return the child node corresponding to the specified index
    * @throws KeyGenerationException
    *            if this is a public key node which is hardened, or if no key
    *            can be created for this index (extremely unlikely)
    */
   public HdKeyNode createChildNode(int index) throws KeyGenerationException {
      byte[] data;
      byte[] publicKeyBytes = _publicKey.getPublicKeyBytes();
      if (0 == (index & HARDENED_MARKER)) {
         // Not hardened key
         ByteWriter writer = new ByteWriter(publicKeyBytes.length + 4);
         writer.putBytes(publicKeyBytes);
         writer.putIntBE(index);
         data = writer.toBytes();
      } else {
         // Hardened key
         if (!isPrivateHdKeyNode()) {
            throw new KeyGenerationException("Cannot generate hardened HD key node from pubic HD key node");
         }
         ByteWriter writer = new ByteWriter(33 + 4);
         writer.put((byte) 0);
         writer.putBytes(_privateKey.getPrivateKeyBytes());
         writer.putIntBE(index);
         data = writer.toBytes();
      }
      byte[] l = Hmac.hmacSha512(_chainCode, data);
      byte[] lL = BitUtils.copyOfRange(l, 0, 32);
      byte[] lR = BitUtils.copyOfRange(l, 32, 64);

      BigInteger m = new BigInteger(1, lL);
      if (m.compareTo(Parameters.n) >= 0) {
         throw new KeyGenerationException(
               "An unlikely thing happened: A key derivation parameter is larger than the N modulus of the curve");
      }

      if (isPrivateHdKeyNode()) {

         BigInteger kpar = new BigInteger(1, _privateKey.getPrivateKeyBytes());
         BigInteger k = m.add(kpar).mod(Parameters.n);
         if (k.equals(BigInteger.ZERO)) {
            throw new KeyGenerationException("An unlikely thing happened: The derived key is zero");
         }

         // Make a 32 byte result where k is copied to the end
         byte[] privateKeyBytes = bigIntegerTo32Bytes(k);
         InMemoryPrivateKey key = new InMemoryPrivateKey(privateKeyBytes, true);
         return new HdKeyNode(key, lR, _depth + 1, getFingerprint(), index);
      } else {
         Point q = Parameters.G.multiply(m).add(Parameters.curve.decodePoint(_publicKey.getPublicKeyBytes()));
         if (q.isInfinity()) {
            throw new KeyGenerationException("An unlikely thing happened: Invalid key point at infinity");
         }
         PublicKey newPublicKey = new PublicKey(new Point(Parameters.curve, q.getX(), q.getY(), true).getEncoded());
         return new HdKeyNode(newPublicKey, lR, _depth + 1, getFingerprint(), index);
      }
   }

   private byte[] bigIntegerTo32Bytes(BigInteger b) {
      // Returns an array of bytes which is at most 33 bytes long, and possibly
      // with a leading zero
      byte[] bytes = b.toByteArray();
      Preconditions.checkArgument(bytes.length <= 33);
      if (bytes.length == 33) {
         // The result is 32 bytes, but with zero at the beginning, which we
         // strip
         Preconditions.checkArgument(bytes[0] == 0);
         return BitUtils.copyOfRange(bytes, 1, 33);
      }
      // The result is 32 bytes or less, make it 32 bytes with the data at the
      // end
      byte[] result = new byte[32];
      System.arraycopy(bytes, 0, result, result.length - bytes.length, bytes.length);
      return result;
   }

   /**
    * Get the fingerprint of this node
    */
   public int getFingerprint() {
      byte[] hash = _publicKey.getPublicKeyHash();
      int fingerprint = (((int) hash[0]) & 0xFF) << 24;
      fingerprint += (((int) hash[1]) & 0xFF) << 16;
      fingerprint += (((int) hash[2]) & 0xFF) << 8;
      fingerprint += (((int) hash[3]) & 0xFF);
      return fingerprint;
   }

   /**
    * Get the private key of this node
    * 
    * @throws KeyGenerationException
    *            if this is not a private key node
    */
   public InMemoryPrivateKey getPrivateKey() throws KeyGenerationException {
      if (!isPrivateHdKeyNode()) {
         throw new KeyGenerationException("Not a private HD key node");
      }
      return _privateKey;
   }

   /**
    * Get the public key of this node.
    */
   public PublicKey getPublicKey() {
      return _publicKey;
   }

   private static final byte[] PRODNET_PUBLIC = new byte[] { (byte) 0x04, (byte) 0x88, (byte) 0xB2, (byte) 0x1E };
   private static final byte[] TESTNET_PUBLIC = new byte[] { (byte) 0x04, (byte) 0x35, (byte) 0x87, (byte) 0xCF };
   private static final byte[] PRODNET_PRIVATE = new byte[] { (byte) 0x04, (byte) 0x88, (byte) 0xAD, (byte) 0xE4 };
   private static final byte[] TESTNET_PRIVATE = new byte[] { (byte) 0x04, (byte) 0x35, (byte) 0x83, (byte) 0x94 };

   /**
    * Serialize this node
    */
   public String serialize(NetworkParameters network) throws KeyGenerationException {
      ByteWriter writer = new ByteWriter(4 + 1 + 4 + 4 + 32 + 32);
      if (network.isProdnet()) {
         writer.putBytes(isPrivateHdKeyNode() ? PRODNET_PRIVATE : PRODNET_PUBLIC);
      } else {
         writer.putBytes(isPrivateHdKeyNode() ? TESTNET_PRIVATE : TESTNET_PUBLIC);
      }
      writer.put((byte) (_depth & 0xFF));
      writer.putIntBE(_parentFingerprint);
      writer.putIntBE(_index);
      writer.putBytes(_chainCode);
      if (isPrivateHdKeyNode()) {
         writer.put((byte) 0);
         writer.putBytes(_privateKey.getPrivateKeyBytes());
      } else {
         writer.putBytes(_publicKey.getPublicKeyBytes());
      }
      return Base58.encodeWithChecksum(writer.toBytes());
   }

   /**
    * Create a node from a serialized string
    * 
    * @param string
    *           the string to parse
    * @param network
    *           the network the node is to be used on
    * @return a HD wallet key node
    * @throws KeyGenerationException
    *            if there is an error parsing the string to a HD wallet key node
    *            on the specified network
    */
   public static HdKeyNode parse(String string, NetworkParameters network) throws KeyGenerationException {
      try {
         byte[] bytes = Base58.decodeChecked(string);
         if (bytes == null) {
            throw new KeyGenerationException("Invalid checksum");
         }
         if (bytes.length != 78) {
            throw new KeyGenerationException("Invalid size");
         }
         ByteReader reader = new ByteReader(bytes);
         boolean isPrivate;
         byte[] magic = reader.getBytes(4);
         if (BitUtils.areEqual(magic, PRODNET_PRIVATE)) {
            if (!network.isProdnet()) {
               throw new KeyGenerationException("Invalid network");
            }
            isPrivate = true;
         } else if (BitUtils.areEqual(magic, PRODNET_PUBLIC)) {
            if (!network.isProdnet()) {
               throw new KeyGenerationException("Invalid network");
            }
            isPrivate = false;
         } else if (BitUtils.areEqual(magic, TESTNET_PRIVATE)) {
            if (network.isProdnet()) {
               throw new KeyGenerationException("Invalid network");
            }
            isPrivate = true;
         } else if (BitUtils.areEqual(magic, TESTNET_PUBLIC)) {
            if (network.isProdnet()) {
               throw new KeyGenerationException("Invalid network");
            }
            isPrivate = false;
         } else {
            throw new KeyGenerationException("Invalid magic header for HD key node");
         }

         int depth = ((int) reader.get()) & 0xFF;
         int parentFingerprint = reader.getIntBE();
         int index = reader.getIntBE();
         byte[] chainCode = reader.getBytes(CHAIN_CODE_SIZE);
         if (isPrivate) {
            if (reader.get() != (byte) 0x00) {
               throw new KeyGenerationException("Invalid private key");
            }
            InMemoryPrivateKey privateKey = new InMemoryPrivateKey(reader.getBytes(32), true);
            return new HdKeyNode(privateKey, chainCode, depth, parentFingerprint, index);
         } else {
            PublicKey publicKey = new PublicKey(reader.getBytes(33));
            return new HdKeyNode(publicKey, chainCode, depth, parentFingerprint, index);
         }
      } catch (InsufficientBytesException e) {
         throw new KeyGenerationException("Insufficient bytes in serialization");
      }
   }

   @Override
   public String toString() {
      return "Fingerprint: " + Integer.toString(getFingerprint());
   }

   private static byte[] asciiStringToBytes(String string) {
      try {
         return string.getBytes("US-ASCII");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException();
      }
   }

   @Override
   public int hashCode() {
      return _publicKey.hashCode();
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof HdKeyNode)) {
         return false;
      }
      HdKeyNode other = (HdKeyNode) obj;
      if (!this._publicKey.equals(other._publicKey)) {
         return false;
      }
      if (this._depth != other._depth) {
         return false;
      }
      if (this._parentFingerprint != other._parentFingerprint) {
         return false;
      }
      if (this._index != other._index) {
         return false;
      }
      if (!BitUtils.areEqual(this._chainCode, other._chainCode)) {
         return false;
      }
      return this.isPrivateHdKeyNode() == other.isPrivateHdKeyNode();
   }

   // returns the own index of this key
   public int getIndex(){
      return _index;
   }

   // returns the parent fingerprint
   public int getParentFingerprint(){
      return _parentFingerprint;
   }

   // return the hierarchical depth of this node
   public int getDepth(){
      return _depth;
   }


   // generate internal uuid from public key of the HdKeyNode
   public UUID getUuid() {
      // Create a UUID from the byte indexes 8-15 and 16-23 of the account public key
      byte[] publicKeyBytes = this.getPublicKey().getPublicKeyBytes();
      return new UUID(BitUtils.uint64ToLong(publicKeyBytes, 8), BitUtils.uint64ToLong(
            publicKeyBytes, 16));
   }
}
