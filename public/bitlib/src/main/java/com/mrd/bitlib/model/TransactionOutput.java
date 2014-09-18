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
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HexUtils;

public class TransactionOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   public static class TransactionOutputParsingException extends Exception {
      private static final long serialVersionUID = 1L;

      public TransactionOutputParsingException(byte[] script) {
         super("Unable to parse transaction output: " + HexUtils.toHex(script));
      }

      public TransactionOutputParsingException(String message) {
         super(message);
      }
   }

   public long value;
   public ScriptOutput script;

   public static TransactionOutput fromByteReader(ByteReader reader) throws TransactionOutputParsingException {
      try {
         long value = reader.getLongLE();
         int scriptSize = (int) reader.getCompactInt();
         byte[] scriptBytes = reader.getBytes(scriptSize);
         ScriptOutput script = ScriptOutput.fromScriptBytes(scriptBytes);
         return new TransactionOutput(value, script);
      } catch (InsufficientBytesException e) {
         throw new TransactionOutputParsingException("Unable to parse transaction output: " + e.getMessage());
      }
   }

   public TransactionOutput(long value, ScriptOutput script) {
      this.value = value;
      this.script = script;
   }

   public byte[] toBytes() {
      ByteWriter writer = new ByteWriter(1024);
      toByteWriter(writer);
      return writer.toBytes();
   }

   public void toByteWriter(ByteWriter writer) {
      writer.putLongLE(value);
      byte[] scriptBytes = script.getScriptBytes();
      writer.putCompactInt(scriptBytes.length);
      writer.putBytes(scriptBytes);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("value: ").append(value).append(" script: ").append(script.dump());
      return sb.toString();
   }

}
