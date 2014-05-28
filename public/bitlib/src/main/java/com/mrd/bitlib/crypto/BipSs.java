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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.bitcoinj.Base58;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

/**
 * Class implementing BIP-SS (place holder name until there actually is a BIP),
 * which allows sharding and combining a BIP-32 master seed or a Bitcoin private
 * key.
 * <p>
 * At its core it uses a 2^8 Galois Field for sharding and combining shares.
 * This allows you to split any secret into a number of shares and specify a
 * threshold, which defines the necessary number of shares needed to combine the
 * original secret. This allows you to for instance split a private key into 3
 * shares, where any 2 of those shares allow you to recreate the private key,
 * while preventing anyone having zero or one share have your private key. This
 * effectively protects you against loss or theft of your private key.
 * <p>
 * You can set the parameters within the following boundaries: 1 <= threshold <=
 * number of shares <= 255
 */
public class BipSs {

   /**
    * Shard a private key into a number of shares with a specified threshold
    * number of shares needed to re-create the key.
    * 
    * 
    * @param key
    *           the private key to shard
    * @param threshold
    *           the number of shares needed to create the private key. 0 <
    *           threshold <= shares <= 255
    * @param numShares
    *           the number of shares to generate 0 < threshold <= shares <= 255
    * @param encodingFormat
    *           the encoding format to use
    * @param network
    *           the network the shares are for
    * @return a list of shares for the sharded private key
    */
   public static List<Share> shard(InMemoryPrivateKey key, int threshold, int numShares, EncodingFormat encodingFormat,
         NetworkParameters network) {
      Preconditions.checkArgument(0 < threshold, "The threshold must be larger than zero");
      Preconditions.checkArgument(threshold <= numShares,
            "The threshold must be less than or equal to the number of shares");
      Preconditions.checkArgument(numShares <= 255, "The number of shares must be less than 255");
      Gf256 gf = new Gf256();
      List<Share> shares = new ArrayList<Share>(numShares);
      ContentType kind = key.getPublicKey().isCompressed() ? ContentType.COMPRESSED_KEY : ContentType.UNCOMPRESSED_KEY;
      byte[] contentHash = calculateContentHash(key.getPrivateKeyBytes());
      for (Gf256.Share s : gf.makeShares(key.getPrivateKeyBytes(), threshold, numShares)) {
         shares.add(new Share(kind, contentHash, ((int) s.index) & 0xFF, threshold, s.data, encodingFormat, network));
      }
      return shares;
   }

   /**
    * Shard a BIP-32 seed into a number of shares with a specified threshold
    * number of shares needed to re-create the key
    * 
    * @param seed
    *           the seed to shard, must be 16, 32, or 64 bytes
    * @param threshold
    *           the number of shares needed to create the seed. 0 < threshold <=
    *           shares <= 255
    * @param numShares
    *           the number of shares to generate 0 < threshold <= shares <= 255
    * @param encodingFormat
    *           the encoding format to use
    * @param network
    *           the network the shares are for
    * @return a list of shares for the sharded seed
    * 
    */
   public static List<Share> shard(byte[] seed, int threshold, int numShares, EncodingFormat encodingFormat,
         NetworkParameters network) {
      Preconditions.checkArgument(seed.length == 16 || seed.length == 32 || seed.length == 64,
            "The BIP-32 seed must be 16, 32, or 64 bytes long");
      Preconditions.checkArgument(0 < threshold, "The threshold must be larger than zero");
      Preconditions.checkArgument(threshold <= numShares,
            "The threshold must be less than or equal to the number of shares");
      Preconditions.checkArgument(numShares <= 255, "The number of shares must be less than 255");
      Gf256 gf = new Gf256();
      List<Share> shares = new ArrayList<Share>(numShares);
      byte[] contentHash = calculateContentHash(seed);
      for (Gf256.Share s : gf.makeShares(seed, threshold, numShares)) {
         shares.add(new Share(ContentType.BIP_32_SEED, contentHash, ((int) s.index) & 0xFF, threshold, s.data,
               encodingFormat, network));
      }
      return shares;
   }

   public static class NotEnoughSharesException extends Exception {
      private static final long serialVersionUID = 1L;
   }

   public static class InvalidContentTypeException extends Exception {
      private static final long serialVersionUID = 1L;
   }

   public static class IncompatibleSharesException extends Exception {
      private static final long serialVersionUID = 1L;
   }

   /**
    * Combine a number of shares into a private key.
    * 
    * @param shares
    *           the shares to combine, must be larger or equal to the share
    *           threshold set when the shares were originally created.
    * @return the combined private key
    * @throws IncompatibleSharesException
    *            if the shares are incompatible with each other.
    * @throws NotEnoughSharesException
    *            if an insufficient number of shares were presented.
    * @throws InvalidContentTypeException
    *            if the content type of the shares are not for a compressed or
    *            uncompressed private key
    */
   public static InMemoryPrivateKey combinePrivateKey(Collection<Share> shares) throws IncompatibleSharesException,
         NotEnoughSharesException, InvalidContentTypeException {
      byte[] privateKeyBytes = combine(shares, true);
      boolean compressed = shares.iterator().next().contentType == ContentType.COMPRESSED_KEY;
      return new InMemoryPrivateKey(privateKeyBytes, compressed);
   }

   /**
    * Combine a number of shares into a BIP-32 master seed.
    * 
    * @param shares
    *           the shares to combine, must be larger or equal to the share
    *           threshold set when the shares were originally created.
    * @return the combined BIp-32 master seed
    * @throws IncompatibleSharesException
    *            if the shares are incompatible with each other.
    * @throws NotEnoughSharesException
    *            if an insufficient number of shares were presented.
    * @throws InvalidContentTypeException
    *            if the content type of the shares are not for a BIP-32 master
    *            seed
    */
   public static byte[] combineSeed(Collection<Share> shares) throws IncompatibleSharesException,
         NotEnoughSharesException, InvalidContentTypeException {
      byte[] seed = combine(shares, false);
      return seed;
   }

   private static byte[] combine(Collection<Share> shares, boolean privateKey) throws IncompatibleSharesException,
         NotEnoughSharesException, InvalidContentTypeException {

      // Need at least one share
      if (shares.size() == 0) {
         throw new NotEnoughSharesException();
      }

      // Figure out whether the shares are compatible
      Iterator<Share> it = shares.iterator();
      Share firstShare = it.next();
      while (it.hasNext()) {
         Share share = it.next();
         if (!share.isCompatible(firstShare)) {
            throw new IncompatibleSharesException();
         }
      }

      // Does it have the right format?
      if (firstShare.contentType == ContentType.BIP_32_SEED && privateKey) {
         throw new InvalidContentTypeException();
      }
      if ((firstShare.contentType == ContentType.COMPRESSED_KEY || firstShare.contentType == ContentType.UNCOMPRESSED_KEY)
            && !privateKey) {
         throw new InvalidContentTypeException();
      }

      // Get the set of unique shares
      Collection<Share> unique = Share.removeDuplicateIndexes(shares);

      // Figure out whether we have enough shares (if possible)
      int threshold = unique.iterator().next().threshold;
      if (threshold == -1) {
         // If we don't know the number of needed shares we assume that it is
         // the lot
         threshold = unique.size();
      } else if (threshold > unique.size()) {
         throw new NotEnoughSharesException();
      }

      // Make a selection of the necessary shares
      List<Share> selection = new ArrayList<Share>();
      it = unique.iterator();
      while (it.hasNext()) {
         selection.add(it.next());
         if (selection.size() == threshold) {
            break;
         }
      }

      // Combine
      Gf256 gf = new Gf256();
      List<Gf256.Share> gfShares = new ArrayList<Gf256.Share>();
      for (Share s : selection) {
         gfShares.add(new Gf256.Share((byte) s.shareNumber, s.shareData));
      }
      byte[] content = gf.combineShares(gfShares);
      return content;
   }

   private static final int LONG_HEADER_LENGTH = 1 + 1 + 2 + 1 + 1;

   private static final int SHORT_OR_COMPACT_HEADER_LENGTH = 1 + 1 + 1;

   private static byte[] calculateContentHash(byte[] content) {
      return BitUtils.copyOf(HashUtils.doubleSha256(content).getBytes(), 2);
   }

   public enum EncodingFormat {
      LONG, SHORT, COMPACT
   }

   private enum ContentType {
      UNKNOWN, COMPRESSED_KEY, UNCOMPRESSED_KEY, BIP_32_SEED
   }

   private static final EncodingParameters[] PRODNET_ENCONDING_PARAMETERS;
   private static final EncodingParameters[] TESTNET_ENCONDING_PARAMETERS;
   static {
      // @formatter:off
      PRODNET_ENCONDING_PARAMETERS = new EncodingParameters[] { 
            new EncodingParameters(128>>3, EncodingFormat.LONG,    ContentType.BIP_32_SEED,      (byte) 0x0E, (byte) 0x53), 
            new EncodingParameters(128>>3, EncodingFormat.SHORT,   ContentType.BIP_32_SEED,      (byte) 0x15, (byte) 0x3C), 
            new EncodingParameters(128>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x15, (byte) 0x3B),
            
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.UNCOMPRESSED_KEY,  (byte) 0x1A, (byte) 0x26), 
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.COMPRESSED_KEY,    (byte) 0x1A, (byte) 0x27), 
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.BIP_32_SEED,       (byte) 0x1A, (byte) 0x46), 

            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.UNCOMPRESSED_KEY,  (byte) 0x26, (byte) 0xC4), 
            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.COMPRESSED_KEY,    (byte) 0x26, (byte) 0xC5), 
            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.BIP_32_SEED,       (byte) 0x26, (byte) 0xF4), 
            
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.UNCOMPRESSED_KEY, (byte) 0x26, (byte) 0xC6), 
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.COMPRESSED_KEY,   (byte) 0x26, (byte) 0xC7), 
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x26, (byte) 0xF6), 
            
            new EncodingParameters(512>>3, EncodingFormat.LONG,    ContentType.BIP_32_SEED,      (byte) 0x58, (byte) 0x72), 
            new EncodingParameters(512>>3, EncodingFormat.SHORT,   ContentType.BIP_32_SEED,      (byte) 0x83, (byte) 0x30), 
            new EncodingParameters(512>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x83, (byte) 0x32),
      };
      TESTNET_ENCONDING_PARAMETERS = new EncodingParameters[] { 
            new EncodingParameters(128>>3, EncodingFormat.LONG,    ContentType.BIP_32_SEED,      (byte) 0x0E, (byte) 0x55), 
            new EncodingParameters(128>>3, EncodingFormat.SHORT,   ContentType.BIP_32_SEED,      (byte) 0x15, (byte) 0x40), 
            new EncodingParameters(128>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x15, (byte) 0x3F),
            
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.UNCOMPRESSED_KEY,  (byte) 0x1A, (byte) 0x2C), 
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.COMPRESSED_KEY,    (byte) 0x1A, (byte) 0x2D), 
            new EncodingParameters(256>>3, EncodingFormat.LONG,   ContentType.BIP_32_SEED,       (byte) 0x1A, (byte) 0x4C), 

            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.UNCOMPRESSED_KEY,  (byte) 0x26, (byte) 0xCC), 
            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.COMPRESSED_KEY,    (byte) 0x26, (byte) 0xCD), 
            new EncodingParameters(256>>3, EncodingFormat.SHORT,  ContentType.BIP_32_SEED,       (byte) 0x26, (byte) 0xFC), 
            
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.UNCOMPRESSED_KEY, (byte) 0x26, (byte) 0xCE), 
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.COMPRESSED_KEY,   (byte) 0x26, (byte) 0xCF), 
            new EncodingParameters(256>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x26, (byte) 0xFE), 
            
            new EncodingParameters(512>>3, EncodingFormat.LONG,    ContentType.BIP_32_SEED,      (byte) 0x58, (byte) 0x8A), 
            new EncodingParameters(512>>3, EncodingFormat.SHORT,   ContentType.BIP_32_SEED,      (byte) 0x83, (byte) 0x48), 
            new EncodingParameters(512>>3, EncodingFormat.COMPACT, ContentType.BIP_32_SEED,      (byte) 0x83, (byte) 0x4A),
      };
      // @formatter:on
   }

   private static EncodingParameters findEncodingParameters(int contentSize, EncodingFormat form, ContentType kind,
         NetworkParameters network) {
      EncodingParameters[] params = network.isProdnet() ? PRODNET_ENCONDING_PARAMETERS : TESTNET_ENCONDING_PARAMETERS;
      for (EncodingParameters p : params) {
         if (p.contentSize == contentSize && p.encodingFormat == form && p.contentType == kind) {
            return p;
         }
      }
      return null;
   }

   private static EncodingParameters findEncodingParameters(int encodingSize, byte versionByte, byte prefixByte,
         NetworkParameters network) {
      EncodingParameters[] params = network.isProdnet() ? PRODNET_ENCONDING_PARAMETERS : TESTNET_ENCONDING_PARAMETERS;
      for (EncodingParameters p : params) {
         if (p.encodingSize == encodingSize && p.versionByte == versionByte && p.prefixByte == prefixByte) {
            return p;
         }
      }
      return null;
   }

   private static class EncodingParameters {
      public final int contentSize;
      public final int encodingSize;
      public final EncodingFormat encodingFormat;
      public final ContentType contentType;
      public final byte versionByte;
      public final byte prefixByte;

      private EncodingParameters(int contentSize, EncodingFormat form, ContentType contentType, byte versionByte,
            byte prefixByte) {
         this.contentSize = contentSize;
         this.encodingSize = contentSize
               + (form == EncodingFormat.LONG ? LONG_HEADER_LENGTH : SHORT_OR_COMPACT_HEADER_LENGTH);
         this.encodingFormat = form;
         this.contentType = contentType;
         this.versionByte = versionByte;
         this.prefixByte = prefixByte;
      }

   }

   public static class Share implements Serializable {
      private static final long serialVersionUID = 1L;

      /**
       * The content type of this share.
       */
      public final ContentType contentType;
      /**
       * The hash of the content, may be null if decoded from a format that does
       * not contain the content hash
       */
      public final byte[] contentHash;
      /**
       * The share number for this share
       */
      public final int shareNumber;
      /**
       * The number of shares necessary to recreate the original content, may be
       * 0 if decoded from a format that does not contain the threshold
       */
      public final int threshold;
      /**
       * The data of this share
       */
      public final byte[] shareData;
      /**
       * The encoding format
       */
      public final EncodingFormat encodingFormat;
      /**
       * The network the share is for
       */
      public final NetworkParameters network;

      private Share(ContentType contentType, byte[] contentHash, int shareNumber, int threshold, byte[] shareData,
            EncodingFormat encodingFormat, NetworkParameters network) {
         this.contentType = contentType;
         this.contentHash = contentHash;
         this.shareNumber = shareNumber;
         this.threshold = threshold;
         this.shareData = shareData;
         this.encodingFormat = encodingFormat;
         this.network = network;
      }

      @Override
      public String toString() {
         EncodingParameters params = findEncodingParameters(shareData.length, encodingFormat, contentType, network);
         Preconditions.checkState(params != null);
         ByteWriter w = new ByteWriter(1024);
         w.put(params.versionByte);
         w.put(params.prefixByte);
         switch (encodingFormat) {
         case LONG:
            w.putBytes(contentHash);
            w.put((byte) threshold);
            w.put((byte) shareNumber);
            break;
         case SHORT:
            w.put((byte) shareNumber);
            break;
         case COMPACT:
            int tmp = (threshold << 4) | shareNumber;
            w.put((byte) tmp);
            break;
         }
         w.putBytes(shareData);
         return Base58.encodeWithChecksum(w.toBytes());

      }

      /**
       * Create a share from its string representation.
       * 
       * @param encodedShare
       *           the string representing the share
       * @param network
       *           the network that this share is supposed to be used on
       * @return the decoded share or null if the string was not a valid share
       *         encoding
       */
      public static Share fromString(String encodedShare, NetworkParameters network) {
         // Base58 decode
         byte[] decoded = Base58.decodeChecked(encodedShare);
         if (decoded == null || decoded.length < 2) {
            return null;
         }
         ByteReader reader = new ByteReader(decoded);
         try {
            // Find encoding
            byte versionByte = reader.get();
            byte prefixByte = reader.get();
            EncodingParameters params = findEncodingParameters(decoded.length, versionByte, prefixByte, network);
            if (params == null) {
               return null;
            }
            byte[] contentHash;
            int threshold;
            int shareNumber;
            switch (params.encodingFormat) {
            case LONG:
               contentHash = reader.getBytes(2);
               threshold = b2i(reader.get());
               shareNumber = b2i(reader.get());
               break;
            case SHORT:
               contentHash = null;
               threshold = 0;
               shareNumber = b2i(reader.get());
               break;
            case COMPACT:
               int i = b2i(reader.get());
               contentHash = null;
               threshold = i >> 4;
               shareNumber = i & 0x0f;
               break;
            default:
               return null;
            }
            byte[] content = reader.getBytes(params.contentSize);
            return new Share(params.contentType, contentHash, shareNumber, threshold, content, params.encodingFormat,
                  network);
         } catch (InsufficientBytesException e) {
            // This should not happen as we already have checked the content
            // length
            return null;
         }
      }

      /**
       * Determine whether two shares are compatible, and can be combined.
       */
      public boolean isCompatible(Share share) {
         if (contentType != share.contentType) {
            return false;
         }
         if (encodingFormat != share.encodingFormat) {
            return false;
         }
         if (threshold != share.threshold) {
            return false;
         }
         if (!BitUtils.areEqual(contentHash, share.contentHash)) {
            return false;
         }
         if (!network.equals(share.network)) {
            return false;
         }
         return true;
      }

      /**
       * Determine the number of shares needed to combine a secret.
       * <p>
       * It is assumed that the shares are compatible.
       * 
       * @param shares
       *           The current set of shares
       * @return the number of shares missing or -1 if the share format does not
       *         allow you to determine the number of missing shares.
       */
      public static int sharesNeeded(Collection<Share> shares) {
         if (shares.size() == 0) {
            return 1;
         }
         int threshold = shares.iterator().next().threshold;
         if (threshold == 0) {
            return -1;
         }
         shares = removeDuplicateIndexes(shares);
         int missing = threshold - shares.size();
         return Math.max(0, missing);
      }

      public static Collection<Share> removeDuplicateIndexes(Collection<Share> shares) {
         Map<Integer, Share> map = new HashMap<Integer, Share>();
         for (Share share : shares) {
            map.put(share.shareNumber, share);
         }
         return map.values();
      }

      private static final int b2i(byte b) {
         return ((int) b) & 0xFF;
      }

   }
}
