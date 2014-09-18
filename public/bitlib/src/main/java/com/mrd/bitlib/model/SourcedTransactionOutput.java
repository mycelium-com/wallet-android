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
import java.util.HashSet;
import java.util.Set;

import com.mrd.bitlib.model.Script.ScriptParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class SourcedTransactionOutput implements Serializable {
   private static final long serialVersionUID = 1L;

   public OutPoint outPoint;
   public long value;
   public Address address;
   public Set<Address> senders;
   public byte[] script;

   public SourcedTransactionOutput(ByteReader reader) throws InsufficientBytesException, ScriptParsingException {
      outPoint = new OutPoint(reader);
      value = reader.getLongLE();
      address = new Address(reader.getBytes(Address.NUM_ADDRESS_BYTES));
      int numSenders = (int) reader.getIntLE();
      senders = new HashSet<Address>();
      for (int i = 0; i < numSenders; i++) {
         senders.add(new Address(reader.getBytes(Address.NUM_ADDRESS_BYTES)));
      }
      int scriptLength = (int) reader.getIntLE();
      script = reader.getBytes(scriptLength);
   }

   public SourcedTransactionOutput(OutPoint outPoint, long value, Address address, Set<Address> senders, byte[] script) {
      this.outPoint = outPoint;
      this.value = value;
      this.address = address;
      this.senders = senders;
      this.script = script;
   }

   public byte[] toBytes() {
      ByteWriter writer = new ByteWriter(1024);
      toByteWriter(writer);
      return writer.toBytes();
   }

   public void toByteWriter(ByteWriter writer) {
      outPoint.toByteWriter(writer);
      writer.putLongLE(value);
      writer.putBytes(address.getAllAddressBytes());
      writer.putIntLE(senders.size());
      for (Address sender : senders) {
         writer.putBytes(sender.getAllAddressBytes());
      }
      writer.putIntLE(script.length);
      writer.putBytes(script);
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("outPoint:").append(outPoint).append(" value: ").append(value).append(" receiver: ").append(address)
            .append(" senders: ").append(senders).append(" scriptLength: ").append(script.length);
      return sb.toString();
   }

   @Override
   public int hashCode() {
      return outPoint.hash.hashCode() + outPoint.index;
   }

   @Override
   public boolean equals(Object obj) {
      if (obj == this) {
         return true;
      }
      if (!(obj instanceof SourcedTransactionOutput)) {
         return false;
      }
      SourcedTransactionOutput other = (SourcedTransactionOutput) obj;
      return outPoint.equals(other.outPoint);
   }

}
