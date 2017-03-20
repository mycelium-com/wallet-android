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

import com.google.bitcoinj.Base58;
import com.google.common.base.Preconditions;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Class implementing BIP-SS (place holder name until there actually is a BIP),
 * which allows combining shares to retrieve the secret
 * <p>
 * At its core it uses a 2^8 Galois Field  combining shares.
 * This allows you to split any secret into a number of shares and specify a
 * threshold, which defines the necessary number of shares needed to combine the
 * original secret. This allows you to for instance split a private key into 3
 * shares, where any 2 of those shares allow you to recreate the private key,
 * while preventing anyone having zero or one share have your private key. This
 * effectively protects you against loss or theft of your private key.
 * <p>
 */
public class BipSss {

   private static final int TYPE_BASE_58_STRING = 19;

   public static class NotEnoughSharesException extends Exception {
      public int needed;
      public NotEnoughSharesException(int needed) {
         this.needed = needed;
      }
      private static final long serialVersionUID = 1L;
   }

   public static class InvalidContentTypeException extends Exception {
      private static final long serialVersionUID = 1L;
   }

   public static class IncompatibleSharesException extends Exception {
      private static final long serialVersionUID = 1L;
   }

   /**
    *
    * @param shares the list of shares to combine
    * @return a base58 encoded string with the secret 
    * @throws IncompatibleSharesException if there are shares not belonging to the same secret
    * @throws NotEnoughSharesException if more shares are needed to get the secret
    * @throws InvalidContentTypeException if the content type is not 19 (for base58 encoded secret)
    */
   public static String combine(Collection<Share> shares) throws IncompatibleSharesException,
         NotEnoughSharesException, InvalidContentTypeException {

      // Need at least one share
      if (shares.size() == 0) {
         //todo figure out something better - disallow empty list
         throw new NotEnoughSharesException(1);
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
      if (firstShare.contentType != TYPE_BASE_58_STRING) {
         throw new InvalidContentTypeException();
      }

      // Get the set of unique shares
      Collection<Share> unique = Share.removeDuplicateIndexes(shares);

      // Figure out whether we have enough shares (if possible)
      int threshold = unique.iterator().next().threshold;
      if (threshold > unique.size()) {
         throw new NotEnoughSharesException(threshold - unique.size());
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
      return Base58.encodeWithChecksum(content);
   }

   public static class Share implements Serializable {
      private static final long serialVersionUID = 1L;
      public static final String SSS_PREFIX = "SSS-";

      public final byte[] id;

      /**
       * The content type of this share.
       */
      public final int contentType;

      /**
       * The share number for this share
       */
      public final int shareNumber;
      
      /**
       * The number of shares necessary to recreate the original content
       */
      public final int threshold;
      
      /**
       * The data of this share
       */
      public final byte[] shareData;

      private Share(int contentType, byte[] id, int shareNumber, int threshold, byte[] shareData) {
         this.id = id;
         this.contentType = contentType;
         this.shareNumber = shareNumber;
         this.threshold = threshold;
         this.shareData = shareData;
      }

      @Override
      public String toString() {
         ByteWriter w = new ByteWriter(1024);
         w.put((byte) contentType);
         w.putBytes(id);
         w.put(getByteForNumberAndThreshold(shareNumber, threshold));
         w.putBytes(shareData);
         String base58 = Base58.encodeWithChecksum(w.toBytes());
         return SSS_PREFIX + base58;
      }

      private byte getByteForNumberAndThreshold(int shareNumber, int threshold) {
         int number = (shareNumber - 1) * 16 + (threshold - 1);
         return  (byte) number;
      }

      /**
       * Create a share from its string representation.
       * 
       * @param encodedShare
       *           the string representing the share
       * @return the decoded share or null if the string was not a valid share
       *         encoding
       */
      public static Share fromString(String encodedShare) {
         //check for SSS- prefix
         if (encodedShare.startsWith(SSS_PREFIX)) {
            encodedShare = encodedShare.substring(SSS_PREFIX.length());
         }
         // Base58 decode
         byte[] decoded = Base58.decodeChecked(encodedShare);
         if (decoded == null || decoded.length < 4) {
            return null;
         }
         ByteReader reader = new ByteReader(decoded);
         try {
            // content type
            byte contentByte = reader.get();
            //id of the secret
            byte[] id = reader.getBytes(2);
            //contains number of this share and total shares needed
            byte numberAndThreshold = reader.get();
            //the  share bytes
            byte[] content = reader.getBytes(reader.available());

            return new Share(b2i(contentByte), id, getShareNumber(numberAndThreshold), getThreshold(numberAndThreshold), content);
         } catch (InsufficientBytesException e) {
            // This should not happen as we already have checked the content length
            return null;
         }
      }

      private static int getThreshold(byte numberAndThreshold) {
         return (b2i(numberAndThreshold) / 16) + 1;
      }

      private static int getShareNumber(byte numberAndThreshold) {
         return (b2i(numberAndThreshold) % 16) + 1;
      }

      /**
       * Determine whether two shares are compatible, and can be combined.
       */
      public boolean isCompatible(Share share) {
         if (contentType != share.contentType) {
            return false;
         }
         if (!BitUtils.areEqual(id, share.id)) {
            return false;
         }
         if (threshold != share.threshold) {
            return false;
         }
         return true;
      }

      public static Collection<Share> removeDuplicateIndexes(Collection<Share> shares) {
         Map<Integer, Share> map = new HashMap<Integer, Share>();
         for (Share share : shares) {
            map.put(share.shareNumber, share);
         }
         return map.values();
      }

      private static int b2i(byte b) {
         return ((int) b) & 0xFF;
      }
   }
}
