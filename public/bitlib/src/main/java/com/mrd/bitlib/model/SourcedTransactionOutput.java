/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
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
