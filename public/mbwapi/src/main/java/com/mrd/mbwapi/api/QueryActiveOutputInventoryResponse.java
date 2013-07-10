/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbwapi.api;


import java.util.LinkedList;
import java.util.List;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.OutPoint;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class QueryActiveOutputInventoryResponse extends ApiObject {

   public static class Item {
      public OutPoint outPoint;
      public int height;

      public Item(OutPoint outPoint, int height) {
         this.outPoint = outPoint;
         this.height = height;
      }

      public Item(ByteReader reader) throws InsufficientBytesException {
         outPoint = new OutPoint(reader);
         height = reader.getIntLE();
      }

      public ByteWriter toByteWriter(ByteWriter writer) {
         outPoint.toByteWriter(writer);
         writer.putIntLE(height);
         return writer;
      }

   }

   public static class Inventory {
      public Address address;
      public List<Item> items;

      public Inventory(Address address, List<Item> items) {
         this.address = address;
         this.items = items;
      }

      public Inventory(ByteReader reader) throws InsufficientBytesException, ApiException {
         byte[] addressBytes = reader.getBytes(21);
         address = new Address(addressBytes);
         int size = reader.getIntLE();
         items = new LinkedList<Item>();
         for (int i = 0; i < size; i++) {
            items.add(new Item(reader));
         }
      }

      public ByteWriter toByteWriter(ByteWriter writer) {
         writer.putBytes(address.getAllAddressBytes());
         writer.putIntLE(items.size());
         for (Item item : items) {
            item.toByteWriter(writer);
         }
         return writer;
      }
   }

   public List<Inventory> activeOutputs;

   /**
    * Current height of the block chain.
    */
   public int chainHeight;

   public QueryActiveOutputInventoryResponse(List<Inventory> activeOutputs, int chainHeight) {
      this.activeOutputs = activeOutputs;
      this.chainHeight = chainHeight;
   }

   protected QueryActiveOutputInventoryResponse(ByteReader reader) throws InsufficientBytesException, ApiException {
      activeOutputs = listFromReader(reader);
      chainHeight = reader.getIntLE();
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   private List<Inventory> listFromReader(ByteReader reader) throws InsufficientBytesException, ApiException {
      int size = reader.getIntLE();
      List<Inventory> list = new LinkedList<Inventory>();
      for (int i = 0; i < size; i++) {
         list.add(new Inventory(reader));
      }
      return list;
   }

   private void listToWriter(List<Inventory> list, ByteWriter writer) {
      writer.putIntLE(list.size());
      for (Inventory inv : list) {
         inv.toByteWriter(writer);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      listToWriter(activeOutputs, writer);
      writer.putIntLE(chainHeight);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.ACTIVE_OUTPUT_INVENTORY_RESPONSE_TYPE;
   }

}
