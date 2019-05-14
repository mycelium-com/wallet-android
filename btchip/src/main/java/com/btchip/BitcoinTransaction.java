/*
*******************************************************************************    
*   Ledger Bitcoin Hardware Wallet Java API
*   (c) 2014-2015 Ledger - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
*   
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*      http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*   limitations under the License.
********************************************************************************
*/

package com.btchip;

import com.btchip.utils.BufferUtils;
import com.btchip.utils.Dump;
import com.btchip.utils.VarintUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Vector;

public class BitcoinTransaction {

   public class BitcoinInput {

      private byte[] prevOut;
      private byte[] script;
      private byte[] sequence;
      private boolean isSegwit;

      public BitcoinInput(ByteArrayInputStream data) throws BTChipException {
         try {
            prevOut = new byte[36];
            data.read(prevOut);
            long scriptSize = VarintUtils.read(data);
            script = new byte[(int) scriptSize];
            data.read(script);
            sequence = new byte[4];
            data.read(sequence);
         } catch (Exception e) {
            throw new BTChipException("Invalid encoding", e);
         }
      }

      byte[] getPrevOut() {
         return prevOut;
      }

      public byte[] getScript() {
         return script;
      }

      byte[] getSequence() {
         return sequence;
      }

      public void setScript(byte[] script) {
         this.script = script;
      }

      public boolean isSegwit() {
         return isSegwit;
      }

      public void setSegwit(boolean segwit) {
         isSegwit = segwit;
      }

      public String toString() {
         StringBuffer buffer = new StringBuffer();
         buffer.append("Prevout ").append(Dump.dump(prevOut)).append('\r').append('\n');
         buffer.append("Script ").append(Dump.dump(script)).append('\r').append('\n');
         buffer.append("Sequence ").append(Dump.dump(sequence)).append('\r').append('\n');
         return buffer.toString();
      }
   }

   public class BitcoinOutput {

      private byte[] amount;
      private byte[] script;

      BitcoinOutput(ByteArrayInputStream data) throws BTChipException {
         try {
            amount = new byte[8];
            data.read(amount);
            long scriptSize = VarintUtils.read(data);
            script = new byte[(int) scriptSize];
            data.read(script);
         } catch (Exception e) {
            throw new BTChipException("Invalid encoding", e);
         }
      }

      public byte[] getAmount() {
         return amount;
      }

      public byte[] getScript() {
         return script;
      }

      public void setAmount(byte[] amount) {
         this.amount = amount;
      }

      public void setScript(byte[] script) {
         this.script = script;
      }

      public void serialize(ByteArrayOutputStream output) throws BTChipException {
         BufferUtils.writeBuffer(output, amount);
         VarintUtils.write(output, script.length);
         BufferUtils.writeBuffer(output, script);
      }

      public String toString() {
         StringBuffer buffer = new StringBuffer();
         buffer.append("Amount ").append(Dump.dump(amount)).append('\r').append('\n');
         buffer.append("Script ").append(Dump.dump(script)).append('\r').append('\n');
         return buffer.toString();
      }
   }

   private byte[] version;
   private Vector<BitcoinInput> inputs;
   private Vector<BitcoinOutput> outputs;
   private byte[] lockTime;
   private boolean witness = false;
   private byte[] witnessScript;

   static final byte DEFAULT_VERSION[] = {(byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x00};
   static final byte DEFAULT_SEQUENCE[] = {(byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};

   public BitcoinTransaction(ByteArrayInputStream data) throws BTChipException {
      inputs = new Vector<>();
      outputs = new Vector<>();
      try {
         version = new byte[4];
         data.read(version);
         data.mark(0);
         if (data.read() == 0 && data.read() != 0 ) {
            witness = true;
         } else {
            data.reset();
         }
         long numberItems = VarintUtils.read(data);
         for (long i = 0; i < numberItems; i++) {
            inputs.add(new BitcoinInput(data));
         }
         numberItems = VarintUtils.read(data);
         for (long i = 0; i < numberItems; i++) {
            outputs.add(new BitcoinOutput(data));
         }
         if (witness) {
            readWitnessScript(data);
         }
         lockTime = new byte[4];
         data.read(lockTime);
      } catch (Exception e) {
         throw new BTChipException("Invalid encoding", e);
      }
   }

   private void readWitnessScript(ByteArrayInputStream data) throws IOException {
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      for (long i = 0; i < inputs.size(); i++) {
         byte stackSize = (byte) data.read();
         outputStream.write(stackSize);
         if (stackSize != 0) {
            inputs.elementAt((int) i).setSegwit(true);
         }
         for (int y = 0; y < stackSize; y++) {
            byte pushSize = (byte) data.read();
            outputStream.write(pushSize);
            byte[] push = new byte[pushSize];
            data.read(push);
            outputStream.write(push);
         }
      }
      witnessScript = outputStream.toByteArray();
   }


   public byte[] serializeOutputs() throws BTChipException {
      ByteArrayOutputStream output = new ByteArrayOutputStream();
      VarintUtils.write(output, outputs.size());
      for (BitcoinOutput outputItem : outputs) {
         outputItem.serialize(output);
      }
      return output.toByteArray();
   }

   public byte[] getVersion() {
      return version;
   }

   public Vector<BitcoinInput> getInputs() {
      return inputs;
   }

   public Vector<BitcoinOutput> getOutputs() {
      return outputs;
   }

   byte[] getLockTime() {
      return lockTime;
   }

   public void setVersion(byte[] version) {
      this.version = version;
   }

   public String toString() {
      StringBuffer buffer = new StringBuffer();
      buffer.append("Version ").append(Dump.dump(version)).append('\r').append('\n');
      int index = 1;
      for (BitcoinInput input : inputs) {
         buffer.append("Input #").append(index).append('\r').append('\n');
         buffer.append(input.toString());
         index++;
      }
      index = 1;
      for (BitcoinOutput output : outputs) {
         buffer.append("Output #").append(index).append('\r').append('\n');
         buffer.append(output.toString());
         index++;
      }
      buffer.append("LockTime ").append(Dump.dump(lockTime)).append('\r').append('\n');
      if (witness) {
         buffer.append("Witness script ").append(Dump.dump(witnessScript)).append('\r').append('\n');
      }
      return buffer.toString();
   }

}
