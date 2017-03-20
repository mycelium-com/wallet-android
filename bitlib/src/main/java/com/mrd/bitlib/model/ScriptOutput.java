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

public abstract class ScriptOutput extends Script {
   private static final long serialVersionUID = 1L;

   public static ScriptOutput fromScriptBytes(byte[] scriptBytes) {
      byte[][] chunks;
      try {
         chunks = Script.chunksFromScriptBytes(scriptBytes);
      } catch (ScriptParsingException e) {
         return new ScriptOutputError(scriptBytes);
      }
      if (chunks == null) {
         return null;
      }
      if (ScriptOutputStandard.isScriptOutputStandard(chunks)) {
         return new ScriptOutputStandard(chunks, scriptBytes);
      } else if (ScriptOutputPubkey.isScriptOutputPubkey(chunks)) {
         return new ScriptOutputPubkey(chunks, scriptBytes);
      } else if (ScriptOutputP2SH.isScriptOutputP2SH(chunks)) {
         return new ScriptOutputP2SH(chunks, scriptBytes);
      } else if (ScriptOutputMsg.isScriptOutputMsg(chunks)) {
         return new ScriptOutputMsg(chunks, scriptBytes);
      } else {
         return new ScriptOutputStrange(chunks, scriptBytes);
      }

   }

   protected ScriptOutput(byte[] scriptBytes) {
      super(scriptBytes, false);
   }

   public abstract Address getAddress(NetworkParameters network);

}
