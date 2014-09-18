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

import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

public class Block {

   public static class BlockParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      public BlockParsingException(String message) {
         super(message);
      }

      public BlockParsingException(String message, Exception e) {
         super(message, e);
      }
   }

   // The maximum size of a serialized block
   public static final int MAX_BLOCK_SIZE = 1000000;

   // Header
   public int version;
   public Sha256Hash prevBlockHash;
   public Sha256Hash merkleRoot;
   public int time;
   public int difficultyTarget;
   public int nonce;
   // Transactions
   public Transaction[] transactions;

   private Sha256Hash _hash;

   public static Block fromBlockStore(ByteReader reader) throws BlockParsingException {
      try {
         // Parse header
         int version = reader.getIntLE();
         Sha256Hash prevBlockHash = reader.getSha256Hash().reverse();
         Sha256Hash merkleRoot = reader.getSha256Hash().reverse();
         int time = reader.getIntLE();
         int difficultyTarget = reader.getIntLE();
         int nonce = reader.getIntLE();
         // Parse transactions
         int numTransactions = (int) reader.getCompactInt();
         Transaction[] transactions = new Transaction[numTransactions];
         for (int i = 0; i < numTransactions; i++) {
            try {
               transactions[i] = Transaction.fromByteReader(reader);
            } catch (TransactionParsingException e) {
               throw new BlockParsingException("Unable to parse transaction at index " + i + ": " + e.getMessage());
            }
         }
         return new Block(version, prevBlockHash, merkleRoot, time, difficultyTarget, nonce, transactions);
      } catch (InsufficientBytesException e) {
         throw new BlockParsingException(e.getMessage());
      }
   }

   public Block(int version, Sha256Hash prevBlockHash, Sha256Hash merkleRoot, int time, int difficultyTargetm,
         int nonce, Transaction[] transactions) {
      this.version = version;
      this.prevBlockHash = prevBlockHash;
      this.merkleRoot = merkleRoot;
      this.time = time;
      this.difficultyTarget = difficultyTargetm;
      this.nonce = nonce;
      this.transactions = transactions;
   }

   public void toByteWriter(ByteWriter writer) {
      headerToByteWriter(writer);
      transactionsToByteWriter(writer);
   }

   public void headerToByteWriter(ByteWriter writer) {
      writer.putIntLE(version);
      writer.putSha256Hash(prevBlockHash, true);
      writer.putSha256Hash(merkleRoot, true);
      writer.putIntLE(time);
      writer.putIntLE(difficultyTarget);
      writer.putIntLE(nonce);
   }

   public void transactionsToByteWriter(ByteWriter writer) {
      writer.putCompactInt(transactions.length);
      for (Transaction t : transactions) {
         t.toByteWriter(writer);
      }
   }

   public Sha256Hash getHash() {
      if (_hash == null) {
         ByteWriter writer = new ByteWriter(2000);
         headerToByteWriter(writer);
         _hash = HashUtils.doubleSha256(writer.toBytes()).reverse();
      }
      return _hash;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("Hash: ").append(getHash().toString());
      sb.append(" PrevHash: ").append(prevBlockHash.toString());
      sb.append(" #Tx: ").append(transactions.length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return getHash().hashCode();
   }

   @Override
   public boolean equals(Object other) {
      if (!(other instanceof Block)) {
         return false;
      }
      return getHash().equals(((Block) other).getHash());
   }

}
