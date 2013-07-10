/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.bitlib.model;

import com.mrd.bitlib.model.TransactionInput.TransactionInputParsingException;
import com.mrd.bitlib.model.TransactionOutput.TransactionOutputParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

public class Transaction {

   public static class TransactionParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      public TransactionParsingException(String message) {
         super(message);
      }

      public TransactionParsingException(String message, Exception e) {
         super(message, e);
      }
   }

   public static final int MIN_TRANSACTION_SIZE = 100;
   public int version;
   public TransactionInput[] inputs;
   public TransactionOutput[] outputs;
   public int lockTime;

   private Sha256Hash _hash;

   public static Transaction fromByteReader(ByteReader reader) throws TransactionParsingException {
      try {
         int version = reader.getIntLE();
         int numInputs = (int) reader.getCompactInt();
         TransactionInput[] inputs = new TransactionInput[numInputs];
         for (int i = 0; i < numInputs; i++) {
            try {
               inputs[i] = TransactionInput.fromByteReader(reader);
            } catch (TransactionInputParsingException e) {
               throw new TransactionParsingException("Unable to parse tranaction input at index " + i + ": "
                     + e.getMessage());
            }
         }
         int numOutputs = (int) reader.getCompactInt();
         TransactionOutput[] outputs = new TransactionOutput[numOutputs];
         for (int i = 0; i < numOutputs; i++) {
            try {
               outputs[i] = TransactionOutput.fromByteReader(reader);
            } catch (TransactionOutputParsingException e) {
               throw new TransactionParsingException("Unable to parse tranaction output at index " + i + ": "
                     + e.getMessage());
            }
         }
         int lockTime = reader.getIntLE();
         Transaction t = new Transaction(version, inputs, outputs, lockTime);
         return t;
      } catch (InsufficientBytesException e) {
         throw new TransactionParsingException(e.getMessage());
      }
   }

   public Transaction copy() {
      try {
         return Transaction.fromByteReader(new ByteReader(toBytes()));
      } catch (TransactionParsingException e) {
         // This should never happen
         throw new RuntimeException(e);
      }
   }

   public byte[] toBytes() {
      ByteWriter writer = new ByteWriter(1024);
      toByteWriter(writer);
      return writer.toBytes();
   }

   public void toByteWriter(ByteWriter writer) {
      writer.putIntLE(version);
      writer.putCompactInt(inputs.length);
      for (TransactionInput input : inputs) {
         input.toByteWriter(writer);
      }
      writer.putCompactInt(outputs.length);
      for (TransactionOutput output : outputs) {
         output.toByteWriter(writer);
      }
      writer.putIntLE(lockTime);
   }

   public Transaction(int version, TransactionInput[] inputs, TransactionOutput[] outputs, int lockTime) {
      this.version = version;
      this.inputs = inputs;
      this.outputs = outputs;
      this.lockTime = lockTime;
   }

   public Sha256Hash getHash() {
      if (_hash == null) {
         ByteWriter writer = new ByteWriter(2000);
         toByteWriter(writer);
         _hash = new Sha256Hash(HashUtils.doubleSha256(writer.toBytes()), true);
      }
      return _hash;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(getHash()).append(" in: ").append(inputs.length).append(" out: ").append(outputs.length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return getHash().hashCode();
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof Transaction)) {
         return false;
      }
      return getHash().equals(((Transaction) other).getHash());
   }

   public boolean isCoinbase() {
      for (TransactionInput in : inputs) {
         if (in.script instanceof ScriptInputCoinbase) {
            return true;
         }
      }
      return false;
   }
}
