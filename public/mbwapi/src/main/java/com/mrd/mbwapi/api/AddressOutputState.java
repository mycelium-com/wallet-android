package com.mrd.mbwapi.api;

import java.util.HashSet;
import java.util.Set;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class AddressOutputState {
   public Address address;
   public Set<OutPoint> confirmed;
   public Set<OutPoint> receiving;
   public Set<OutPoint> sending;

   public AddressOutputState(Address address, Set<OutPoint> confirmed, Set<OutPoint> receiving, Set<OutPoint> sending) {
      this.address = address;
      this.confirmed = confirmed;
      this.receiving = receiving;
      this.sending = sending;
   }

   public AddressOutputState(ByteReader reader) throws InsufficientBytesException {
      byte[] addressBytes = reader.getBytes(21);
      address = new Address(addressBytes);
      confirmed = outPointSetFromReader(reader);
      receiving = outPointSetFromReader(reader);
      sending = outPointSetFromReader(reader);
   }

   private static Set<OutPoint> outPointSetFromReader(ByteReader reader) throws InsufficientBytesException {
      int size = reader.getIntLE();
      Set<OutPoint> list = new HashSet<OutPoint>(size);
      for (int i = 0; i < size; i++) {
         list.add(new OutPoint(reader));
      }
      return list;
   }

   private static void outPointSetToWriter(Set<OutPoint> outPoints, ByteWriter writer) {
      writer.putIntLE(outPoints.size());
      for (OutPoint outPoint : outPoints) {
         outPoint.toByteWriter(writer);
      }
   }

   public ByteWriter toByteWriter(ByteWriter writer) {
      writer.putBytes(address.getAllAddressBytes());
      outPointSetToWriter(confirmed, writer);
      outPointSetToWriter(receiving, writer);
      outPointSetToWriter(sending, writer);
      return writer;
   }

}
