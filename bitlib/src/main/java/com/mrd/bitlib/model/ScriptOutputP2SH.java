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

public class ScriptOutputP2SH extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   protected ScriptOutputP2SH(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      _p2shAddressBytes = chunks[1];
   }

   private byte[] _p2shAddressBytes;

   protected static boolean isScriptOutputP2SH(byte[][] chunks) {
      if (chunks.length != 3) {
         return false;
      }
      if (!Script.isOP(chunks[0], OP_HASH160)) {
         return false;
      }
      if (chunks[1].length != 20) {
         return false;
      }
      if (!Script.isOP(chunks[2], OP_EQUAL)) {
         return false;
      }
      return true;
   }

   public ScriptOutputP2SH(byte[] addressBytes) {
      super(scriptEncodeChunks(new byte[][] { { (byte) OP_HASH160 }, addressBytes, { (byte) OP_EQUAL } }));
      _p2shAddressBytes = addressBytes;
   }

   /**
    * Get the raw p2sh address that this output is for.
    * 
    * @return The raw p2sh address that this output is for.
    */
   public byte[] getP2SHAddressBytes() {
      return _p2shAddressBytes;
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      byte[] addressBytes = getP2SHAddressBytes();
      return Address.fromP2SHBytes(addressBytes, network);
   }

}
