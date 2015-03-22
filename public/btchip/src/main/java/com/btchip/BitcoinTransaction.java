/*
*******************************************************************************    
*   BTChip Bitcoin Hardware Wallet Java API
*   (c) 2014 BTChip - 1BTChip7VfTnrPra5jqci7ejnMguuHogTn
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Vector;

import com.btchip.utils.BufferUtils;
import com.btchip.utils.Dump;
import com.btchip.utils.VarintUtils;

public class BitcoinTransaction {
	
	public class BitcoinInput {
		
		private byte[] prevOut;
		private byte[] script;
		private byte[] sequence;
		
		public BitcoinInput(ByteArrayInputStream data) throws BTChipException {	
			try {
				prevOut = new byte[36];
				data.read(prevOut);
				long scriptSize = VarintUtils.read(data);
				script = new byte[(int)scriptSize];
				data.read(script);
				sequence = new byte[4];
				data.read(sequence);
			}
			catch(Exception e) {
				throw new BTChipException("Invalid encoding", e);
			}			
		}
		
		public BitcoinInput() {		
			prevOut = new byte[0];
			script = new byte[0];
			sequence = new byte[0];
		}
		
		public void serialize(ByteArrayOutputStream output) throws BTChipException {
			BufferUtils.writeBuffer(output, prevOut);
			VarintUtils.write(output, script.length);
			BufferUtils.writeBuffer(output, script);
			BufferUtils.writeBuffer(output, sequence);
		}
		
		public byte[] getPrevOut() {
			return prevOut;
		}
		public byte[] getScript() {
			return script;
		}
		public byte[] getSequence() {
			return sequence;
		}
		public void setPrevOut(byte[] prevOut) {
			this.prevOut = prevOut;
		}
		public void setScript(byte[] script) {
			this.script = script;
		}
		public void setSequence(byte[] sequence) {
			this.sequence = sequence;
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
		
		public BitcoinOutput(ByteArrayInputStream data) throws BTChipException {
			try {
				amount = new byte[8];
				data.read(amount);
				long scriptSize = VarintUtils.read(data);
				script = new byte[(int)scriptSize];
				data.read(script);				
			}
			catch(Exception e) {
				throw new BTChipException("Invalid encoding", e);
			}			
		}
		
		public BitcoinOutput() {
			amount = new byte[0];
			script = new byte[0];
		}
		
		public void serialize(ByteArrayOutputStream output) throws BTChipException {
			BufferUtils.writeBuffer(output, amount);
			VarintUtils.write(output, script.length);
			BufferUtils.writeBuffer(output, script);
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
	
	public static final byte DEFAULT_VERSION[] = { (byte)0x01, (byte)0x00, (byte)0x00, (byte)0x00 };
	public static final byte DEFAULT_SEQUENCE[] = { (byte)0xff, (byte)0xff, (byte)0xff, (byte)0xff };
	
	public BitcoinTransaction(ByteArrayInputStream data) throws BTChipException  {		
		inputs = new Vector<BitcoinInput>();
		outputs = new Vector<BitcoinOutput>();
		try {
			version = new byte[4];
			data.read(version);
			long numberItems = VarintUtils.read(data);
			for (long i=0; i<numberItems; i++) {
				inputs.add(new BitcoinInput(data));
			}
			numberItems = VarintUtils.read(data);
			for (long i=0; i<numberItems; i++) {
				outputs.add(new BitcoinOutput(data));
			}
			lockTime = new byte[4];
			data.read(lockTime);			
		}
		catch(Exception e) {
			throw new BTChipException("Invalid encoding", e);
		}					
	}
	
	public BitcoinTransaction() {
		version = new byte[0];
		inputs = new Vector<BitcoinInput>();
		outputs = new Vector<BitcoinOutput>();
		lockTime = new byte[0];
	}
	
	public byte[] serialize(boolean skipOutputLockTime) throws BTChipException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(output, version);
		VarintUtils.write(output, inputs.size());
		for (BitcoinInput input : inputs) {
			input.serialize(output);
		}
		if (!skipOutputLockTime) {
			VarintUtils.write(output, outputs.size());
			for (BitcoinOutput outputItem : outputs) {
				outputItem.serialize(output);
			}
			BufferUtils.writeBuffer(output, lockTime);
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
	public byte[] getLockTime() {
		return lockTime;
	}
	
	public void setVersion(byte[] version) {
		this.version = version;
	}
	public void addInput(BitcoinInput input) {
		this.inputs.add(input);
	}
	public void addOutput(BitcoinOutput output) {
		this.outputs.add(output);
	}
	public void setLockTime(byte[] lockTime) {
		this.lockTime = lockTime;
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
		return buffer.toString();
	}

}
