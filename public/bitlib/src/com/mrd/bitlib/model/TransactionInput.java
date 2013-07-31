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

import com.mrd.bitlib.model.Script.ScriptParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;

public class TransactionInput {

   public static class TransactionInputParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      public TransactionInputParsingException(byte[] script) {
         super("Unable to parse transaction input: " + HexUtils.toHex(script));
      }

      public TransactionInputParsingException(String message) {
         super(message);
      }
   }

   private static final int NO_SEQUENCE = -1;

   public OutPoint outPoint;
   public ScriptInput script;
   public int sequence;

   public static TransactionInput fromByteReader(ByteReader reader) throws TransactionInputParsingException {
      try {
         Sha256Hash outPointHash = reader.getSha256Hash(true);
         int outPointIndex = reader.getIntLE();
         int scriptSize = (int) reader.getCompactInt();
         byte[] script = reader.getBytes(scriptSize);
         int sequence = (int) reader.getIntLE();
         OutPoint outPoint = new OutPoint(outPointHash, outPointIndex);
         ScriptInput inscript;
         if (outPointHash.equals(Sha256Hash.ZERO_HASH)) {
            // Coinbase scripts are special as they can contain anything that
            // does not parse
            inscript = new ScriptInputCoinbase(script);
         } else {
            try {
               inscript = ScriptInput.fromScriptBytes(script);
            } catch (ScriptParsingException e) {
               throw new TransactionInputParsingException(e.getMessage());
            }
         }
         return new TransactionInput(outPoint, inscript, sequence);
      } catch (InsufficientBytesException e) {
         throw new TransactionInputParsingException("Unable to parse transaction input: " + e.getMessage());
      }
   }

   public TransactionInput(OutPoint outPoint, ScriptInput script, int sequence) {
      this.outPoint = outPoint;
      this.script = script;
      this.sequence = sequence;
   }

   public TransactionInput(OutPoint outPoint, ScriptInput script) {
      this(outPoint, script, NO_SEQUENCE);
   }

   public ScriptInput getScript() {
      return script;
   }

   public void toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(outPoint.hash, true);
      writer.putIntLE(outPoint.index);
      byte[] script = getScript().getScriptBytes();
      writer.putCompactInt(script.length);
      writer.putBytes(script);
      writer.putIntLE(sequence);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("outpoint: ").append(outPoint.hash).append(':').append(outPoint.index);
      sb.append(" scriptSize: ").append(script.getScriptBytes().length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return outPoint.hash.hashCode() + outPoint.index;
   }

   @Override
   public boolean equals(Object other) {
      if (other == this) {
         return true;
      }
      if (!(other instanceof TransactionInput)) {
         return false;
      }
      TransactionInput otherInput = (TransactionInput) other;
      return outPoint.equals(otherInput.outPoint);
   }

}
