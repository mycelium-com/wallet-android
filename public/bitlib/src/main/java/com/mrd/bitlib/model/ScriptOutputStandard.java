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

public class ScriptOutputStandard extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   private byte[] _addressBytes;

   protected ScriptOutputStandard(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      _addressBytes = chunks[2];
   }

   protected static boolean isScriptOutputStandard(byte[][] chunks) {
      if (chunks.length != 5 && chunks.length != 6) {
         return false;
      }
      if (!Script.isOP(chunks[0], OP_DUP)) {
         return false;
      }
      if (!Script.isOP(chunks[1], OP_HASH160)) {
         return false;
      }
      if (chunks[2].length != 20) {
         return false;
      }
      if (!Script.isOP(chunks[3], OP_EQUALVERIFY)) {
         return false;
      }
      if (!Script.isOP(chunks[4], OP_CHECKSIG)) {
         return false;
      }
      if (chunks.length == 6 && !Script.isOP(chunks[5], OP_NOP)) {
         // Variant that has a NOP at the end
         return false;
      }
      return true;
   }

   public ScriptOutputStandard(byte[] addressBytes) {
      //todo check length for type specfic length 20?
      super(scriptEncodeChunks(new byte[][] { { (byte) OP_DUP }, { (byte) OP_HASH160 }, addressBytes,
            { (byte) OP_EQUALVERIFY }, { (byte) OP_CHECKSIG } }));
      _addressBytes = addressBytes;
   }

   /**
    * Get the address that this output is for.
    * 
    * @return The address that this output is for.
    */
   public byte[] getAddressBytes() {
      return _addressBytes;
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      return Address.fromStandardBytes(getAddressBytes(), network);
   }

}
