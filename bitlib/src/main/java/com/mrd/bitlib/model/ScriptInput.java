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

import com.mrd.bitlib.util.BitUtils;

public class ScriptInput extends Script {
   private static final long serialVersionUID = 1L;

   public static final ScriptInput EMPTY = new ScriptInput(new byte[] {});

   public static ScriptInput fromScriptBytes(byte[] scriptBytes) throws ScriptParsingException {
      if (isWitnessProgram(depush(scriptBytes))) {
         byte[] witnessProgram = getWitnessProgram(depush(scriptBytes));
         if (witnessProgram.length == 20) {
            return new ScriptInputP2WPKH(scriptBytes);
         }
         if (witnessProgram.length == 32) {
            return new ScriptInputP2WSH(scriptBytes);
         }
         return new ScriptInput(scriptBytes);
      }
      byte[][] chunks = Script.chunksFromScriptBytes(scriptBytes);
      if (ScriptInputStandard.isScriptInputStandard(chunks)) {
         return new ScriptInputStandard(chunks, scriptBytes);
      }
      if (ScriptInputPubKey.isScriptInputPubKey(chunks)) {
         return new ScriptInputPubKey(chunks, scriptBytes);
      }
      if (ScriptInputP2SHMultisig.isScriptInputP2SHMultisig(chunks)) {
         return new ScriptInputP2SHMultisig(chunks, scriptBytes);
      }
      return new ScriptInput(scriptBytes);
   }

   /**
    * Check if supplied bytes are witness program
    */
   private static boolean isWitnessProgram(byte[] scriptBytes) {
      if (scriptBytes.length < 4 || scriptBytes.length > 42) {
         return false;
      }
      if (scriptBytes[0] != Script.OP_0 && (scriptBytes[0] < Script.OP_1 || scriptBytes[0] > Script.OP_16)) {
         return false;
      }
      if (scriptBytes[1] < 0x02 || scriptBytes[1] > 0x28) {
         return false;
      }
      return true;
   }

   public static byte[] getWitnessProgram(byte[] scriptBytes) {
      if (!isWitnessProgram(scriptBytes)) {
         throw new IllegalArgumentException("Script is not a witness programm");
      }
      return BitUtils.copyOfRange(scriptBytes, 2, scriptBytes.length);
   }

   /**
    * Tries to remove push code from script.
    * @return script without first byte if it's push, else empty script.
    */
    public static byte[] depush(byte[] script) {
        if (script.length == 0) {
            return new byte[]{};
        }
        byte pushByte = script[0];
        if (pushByte < 1 || pushByte > 76) {
           return new byte[]{};
        }
        script = BitUtils.copyOfRange(script, 1, script.length);
        if (script.length != pushByte) {
           return new byte[]{};
        }
        return script;
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
