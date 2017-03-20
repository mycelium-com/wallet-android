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
import java.io.UnsupportedEncodingException;

import com.mrd.bitlib.util.HashUtils;

public class ScriptOutputMsg extends ScriptOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   private byte[] _messageBytes;
   private byte[] _publicKeyBytes;

   protected ScriptOutputMsg(byte[][] chunks, byte[] scriptBytes) {
      super(scriptBytes);
      _messageBytes = chunks[0];
      _publicKeyBytes = chunks[2];
   }

   protected static boolean isScriptOutputMsg(byte[][] chunks) {
      if (chunks.length != 4) {
         return false;
      }
      if (!Script.isOP(chunks[1], OP_DROP)) {
         return false;
      }
      if (!Script.isOP(chunks[3], OP_CHECKSIG)) {
         return false;
      }
      return true;
   }

   /**
    * Get the bytes for the message contained in this output.
    * 
    * @return The message bytes of this output.
    */
   public byte[] getMessageBytes() {
      return _messageBytes;
   }

   public String getMessage() {
      try {
         return new String(getMessageBytes(), "US-ASCII");
      } catch (UnsupportedEncodingException e) {
         return "";
      }
   }

   /**
    * Get the public key bytes that this output is for.
    * 
    * @return The public key bytes that this output is for.
    */
   public byte[] getPublicKeyBytes() {
      return _publicKeyBytes;
   }

   @Override
   public Address getAddress(NetworkParameters network) {
      byte[] addressBytes = HashUtils.addressHash(getPublicKeyBytes());
      return Address.fromStandardBytes(addressBytes, network);
   }

}
