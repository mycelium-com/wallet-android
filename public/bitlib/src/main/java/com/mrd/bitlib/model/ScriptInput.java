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

public class ScriptInput extends Script {
   private static final long serialVersionUID = 1L;

   public static final ScriptInput EMPTY = new ScriptInput(new byte[] {});

   public static ScriptInput fromScriptBytes(byte[] scriptBytes) throws ScriptParsingException {
      byte[][] chunks = Script.chunksFromScriptBytes(scriptBytes);
      if (ScriptInputStandard.isScriptInputStandard(chunks)) {
         return new ScriptInputStandard(chunks, scriptBytes);
      } else if (ScriptInputPubKey.isScriptInputPubKey(chunks)) {
         return new ScriptInputPubKey(chunks, scriptBytes);
      } else if (ScriptInputP2SHMultisig.isScriptInputP2SHMultisig(chunks)) {
         return new ScriptInputP2SHMultisig(chunks, scriptBytes);
      } else {
         return new ScriptInput(scriptBytes);
      }

   }

   /**
    * Construct an input script from an output script.
    * <p>
    * This is used when verifying or generating signatures, where the input is
    * set to the output of the funding transaction.
    */
   public static ScriptInput fromOutputScript(ScriptOutput output) {
      return new ScriptInput(output._scriptBytes);
   }

   public static ScriptInput fromP2SHOutputBytes(byte[] script) {
      return new ScriptInput(script);
   }

   protected ScriptInput(byte[] scriptBytes) {
      super(scriptBytes, false);
   }

   /**
    * Special constructor for coinbase scripts
    * 
    * @param script
    */
   protected ScriptInput(byte[] script, boolean isCoinBase) {
      super(script, isCoinBase);
   }

   public byte[] getUnmalleableBytes() {
      // We cannot do this for scripts we do not understand
      return null;
   }
}
