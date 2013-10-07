/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
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
         Sha256Hash prevBlockHash = reader.getSha256Hash(true);
         Sha256Hash merkleRoot = reader.getSha256Hash(true);
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
         _hash = new Sha256Hash(HashUtils.doubleSha256(writer.toBytes()), true);
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
