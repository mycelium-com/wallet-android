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
import com.mrd.bitlib.util.Sha256Hash;

import kotlin.NotImplementedError;


public class TransactionInput implements Serializable {
   private static final long serialVersionUID = 1L;
   private static final long SEQUENCE_NO_RBF = 0xFFFFFFFEL; // MAX_INT-1 as unsigned int, anything below is RBF able
   private static final int NO_SEQUENCE = -1;

   public OutPoint outPoint;
   public ScriptInput script;
   private InputWitness witness = InputWitness.EMPTY;
   public int sequence;
   private final long value;

   public static TransactionInput fromByteReader(ByteReader reader) throws TransactionInputParsingException {
      try {
         Sha256Hash outPointHash = reader.getSha256Hash().reverse();
         int outPointIndex = reader.getIntLE();
         int scriptSize = (int) reader.getCompactInt();
         byte[] script = reader.getBytes(scriptSize);
         int sequence = reader.getIntLE();
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
         return new TransactionInput(outPoint, inscript, sequence, 0);
      } catch (InsufficientBytesException e) {
         throw new TransactionInputParsingException("Unable to parse transaction input: " + e.getMessage());
      }
   }

   public TransactionInput(OutPoint outPoint, ScriptInput script, int sequence, long value) {
      this.outPoint = outPoint;
      this.script = script;
      this.sequence = sequence;
      this.value = value;
   }

   public TransactionInput(OutPoint outPoint, ScriptInput script) {
      this(outPoint, script, NO_SEQUENCE, 0);
   }

   public byte[] getScriptCode()  {
      ByteWriter byteWriter = new ByteWriter(1024);
      if (script instanceof ScriptInputP2WSH) {
         throw new NotImplementedError();
      } else if (script instanceof ScriptInputP2WPKH) {
         byteWriter.put((byte) Script.OP_DUP);
         byteWriter.put((byte) Script.OP_HASH160);
         byte[] witnessProgram;
         witnessProgram = ScriptInput.getWitnessProgram(ScriptInput.depush(script.getScriptBytes()));
         byteWriter.put((byte) (0xFF & witnessProgram.length));
         byteWriter.putBytes(witnessProgram);
         byteWriter.put((byte) Script.OP_EQUALVERIFY);
         byteWriter.put((byte) Script.OP_CHECKSIG);
      } else {
         throw new IllegalArgumentException("No scriptcode for " + script.getClass().getCanonicalName());
      }
      return byteWriter.toBytes();
   }

   public boolean hasWitness() {
      return witness != null && witness.getPushCount() != 0;
   }

   public ScriptInput getScript() {
      return script;
   }

   public boolean isMarkedForRbf(){
      return (this.sequence & 0xFFFFFFFFL) < SEQUENCE_NO_RBF;
   }

   public void toByteWriter(ByteWriter writer) {
      writer.putSha256Hash(outPoint.txid, true);
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
      writer.putSha256Hash(outPoint.txid, true);
      writer.putIntLE(outPoint.index);
      writer.putBytes(scriptBytes);
      writer.putIntLE(sequence);
      return writer.toBytes();
   }

   @Override
   public String toString() {
      return "outpoint: " + outPoint.txid + ':' + outPoint.index +
              " scriptSize: " + script.getScriptBytes().length;
   }

   @Override
   public int hashCode() {
      return outPoint.txid.hashCode() + outPoint.index;
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

   public InputWitness getWitness() {
      return witness;
   }

   public void setWitness(InputWitness witness) {
      this.witness = witness;
   }

    public long getValue() {
        return value;
    }

    static class TransactionInputParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      TransactionInputParsingException(String message) {
         this(message, null);
      }

      TransactionInputParsingException(String message, Exception e) {
         super(message, e );
      }
   }
}
