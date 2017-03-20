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

package com.mrd.bitlib.model;

import java.io.Serializable;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

// OutPoint denotes a particular output of a given transaction.
public class OutPoint implements Serializable {
   private static final long serialVersionUID = 1L;
   
   public static final OutPoint COINBASE_OUTPOINT = new OutPoint(Sha256Hash.ZERO_HASH, 0);
   public Sha256Hash hash;
   public int index;

   public OutPoint(Sha256Hash hash, int index) {
      this.hash = hash;
      this.index = index;
   }

   public OutPoint(ByteReader reader) throws InsufficientBytesException {
      this.hash = reader.getSha256Hash();
      this.index = (int) reader.getCompactInt();
   }

   @Override
   public int hashCode() {
      return hash.hashCode() + index;
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof OutPoint)) {
         return false;
      }
      return hash.equals(((OutPoint) other).hash) && index == ((OutPoint) other).index;
   }

   @Override
   public String toString() {
      return new StringBuilder().append(hash).append(':').append(index).toString();
   }

   public ByteWriter toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(hash);
      writer.putCompactInt(index);
      return writer;
   }

   public static OutPoint fromString(String string) {
      try {
         if (string == null) {
            return null;
         }
         int colon = string.indexOf(':');
         if (colon == -1) {
            return null;
         }
         String txid = string.substring(0, colon);
         if (txid.length() != 64) {
            return null;
         }
         byte[] bytes = HexUtils.toBytes(txid);
         if (bytes == null) {
            return null;
         }
         String indexString = string.substring(colon + 1);
         int index = Integer.parseInt(indexString);
         return new OutPoint(new Sha256Hash(bytes), index);
      } catch (Exception e) {
         return null;
      }
   }
}
