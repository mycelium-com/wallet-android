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

public class ScriptOutputOpReturn extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   private byte[] dataBytes;

   protected ScriptOutputOpReturn(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      dataBytes = chunks[1];
   }

   protected static boolean isScriptOutputOpReturn(byte[][] chunks) {
      // {{OP_RETURN},{something non-null non-empty}}
      return chunks.length == 2 &&
            Script.isOP(chunks[0], OP_RETURN) &&
            chunks[1] != null &&
            chunks[1].length > 0;
   }

   /**
    * Get the data bytes contained in this output.
    *
    * @return The data bytes of this output.
    */
   public byte[] getDataBytes() {
      return dataBytes;
   }

   @Override
   public byte[] getAddressBytes() {
      return new byte[Address.NUM_ADDRESS_BYTES];
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      // there is no address associated with this output
      return Address.getNullAddress(network);
   }
}
