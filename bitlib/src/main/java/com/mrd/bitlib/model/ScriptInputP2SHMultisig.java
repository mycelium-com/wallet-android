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

import com.mrd.bitlib.crypto.Signatures;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScriptInputP2SHMultisig extends ScriptInput {
   private static final long serialVersionUID = 1L;

   private List<byte[]> _signatures;
   private int m;
   private int n;
   private List<byte[]> _pubKeys;
   private byte[] _scriptHash;
   private byte[] _embeddedScript;

   protected ScriptInputP2SHMultisig(byte[][] chunks, byte[] scriptBytes) throws ScriptParsingException {
      super(scriptBytes);
      //all but the first and last chunks are signatures, last chunk is the script
      _signatures = new ArrayList<byte[]>(chunks.length - 1);
      _signatures.addAll(Arrays.asList(chunks).subList(1, chunks.length - 1));
      _embeddedScript = chunks[chunks.length - 1];
      byte[][] scriptChunks = Script.chunksFromScriptBytes(_embeddedScript);
      _scriptHash = HashUtils.addressHash(chunks[chunks.length - 1]);
      //the number of signatures needed
      m = Script.opToIntValue(scriptChunks[0]);
      //the total number of possible signing keys
      n = Script.opToIntValue(scriptChunks[scriptChunks.length - 2]);
      //collecting the pubkeys
      _pubKeys = new ArrayList<byte[]>(n);
      _pubKeys.addAll(Arrays.asList(scriptChunks).subList(1, n + 1));
   }

   public List<byte[]> getPubKeys() {
      return new ArrayList<byte[]>(_pubKeys);
   }

   public List<byte[]> getSignatures() {
      return new ArrayList<byte[]>(_signatures);
   }

   public byte[] getScriptHash() {
      return BitUtils.copyByteArray(_scriptHash);
   }

   public byte[] getEmbeddedScript() {
      return BitUtils.copyByteArray(_embeddedScript);
   }

   protected static boolean isScriptInputP2SHMultisig(byte[][] chunks) throws ScriptParsingException {
      if (chunks.length < 3) {
         return false;
      }
      byte[][] scriptChunks;
      try {
         scriptChunks = Script.chunksFromScriptBytes(chunks[chunks.length - 1]);
      } catch (ScriptParsingException e) {
         return false;
      }
      if (scriptChunks.length < 4) {
         return false;
      }

      //starts with an extra op cause of a bug in OP_CHECKMULTISIG
      if (!Script.isOP(chunks[0], OP_0)) {
         return false;
      }
      //last chunk in embedded script has to be CHECKMULTISIG
      if (!Script.isOP(scriptChunks[scriptChunks.length - 1], OP_CHECKMULTISIG)) {
         return false;
      }
      //first and second last chunk must have length 1, because they should be m and n values
      if (scriptChunks[0].length != 1 || scriptChunks[scriptChunks.length - 2].length != 1) {
         return false;
      }
      //check for the m and n values
      try {
         int m = Script.opToIntValue(scriptChunks[0]);
         int n = Script.opToIntValue(scriptChunks[scriptChunks.length - 2]);

         if (n < 1 || n > 16) {
            return false;
         }
         if (m > n || m < 1 || m > 16) {
            return false;
         }
         //check that number of pubkeys matches n
         if (n != scriptChunks.length - 3) {
            return false;
         }

      }catch(IllegalStateException ex){
         //should hopefully not happen, since we check length before evaluating m and n
         //but its better to not risk BQS stopping in case something weird happens
         return false;
      }


      return true;
   }

   public int getSigNumberNeeded() {
      return m;
   }

   @Override
   public byte[] getUnmalleableBytes() {
      ByteWriter writer = new ByteWriter(1024);
      for (byte[] sig : _signatures) {
         byte[][] bytes = Signatures.decodeSignatureParameterBytes(new ByteReader(sig));
         if (bytes == null) {
            // unable to decode signature
            return null;
         }
         writer.putBytes(bytes[0]);
         writer.putBytes(bytes[1]);
      }
      return writer.toBytes();
   }

}
