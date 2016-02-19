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

import com.mrd.bitlib.model.Script.ScriptParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;
import com.mrd.bitlib.util.Sha256Hash;

public class TransactionInput implements Serializable {
   private static final long serialVersionUID = 1L;
   private static final int SEQUENCE_NO_RBF = -1; // -1 => MAX_INT as unsigned int, anything else is RBF-able

   public static class TransactionInputParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      public TransactionInputParsingException(byte[] script) {
         this(script, null);
      }

      public TransactionInputParsingException(byte[] script, Exception e) {
         super("Unable to parse transaction input: " + HexUtils.toHex(script), e);
      }

      public TransactionInputParsingException(String message) {
         this(message, null);
      }

      public TransactionInputParsingException(String message, Exception e) {
         super(message, e );
      }
   }

   private static final int NO_SEQUENCE = -1;

   public OutPoint outPoint;
   public ScriptInput script;
   public int sequence;

   public static TransactionInput fromByteReader(ByteReader reader) throws TransactionInputParsingException {
      try {
         Sha256Hash outPointHash = reader.getSha256Hash().reverse();
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
               throw new TransactionInputParsingException(e.getMessage(), e);
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

   public boolean isMarkedForRbf(){
      return this.sequence != SEQUENCE_NO_RBF;
   }

   public void toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(outPoint.hash, true);
      writer.putIntLE(outPoint.index);
      byte[] script = getScript().getScriptBytes();
      writer.putCompactInt(script.length);
      writer.putBytes(script);
      writer.putIntLE(sequence);
   }

   public byte[] getUnmalleableBytes() {
      byte[] scriptBytes = script.getUnmalleableBytes();
      if (scriptBytes == null) {
         return null;
      }
      ByteWriter writer = new ByteWriter(32 + 4 + scriptBytes.length + 4);
      writer.putSha256Hash(outPoint.hash, true);
      writer.putIntLE(outPoint.index);
      writer.putBytes(scriptBytes);
      writer.putIntLE(sequence);
      return writer.toBytes();
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
