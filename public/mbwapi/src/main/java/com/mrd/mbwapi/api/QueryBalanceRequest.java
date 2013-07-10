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

import java.util.ArrayList;
import java.util.List;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.StringUtils;

public class QueryBalanceRequest extends ApiObject {

   /**
    * The list of addresses to query the balance for
    */
   public List<Address> addresses;

   public QueryBalanceRequest(List<Address> addresses) {
      this.addresses = addresses;
   }

   public QueryBalanceRequest(Address address) {
      this.addresses = new ArrayList<Address>(1);
      this.addresses.add(address);
   }

   protected QueryBalanceRequest(ByteReader reader) throws InsufficientBytesException {
      int num = reader.getIntLE();
      addresses = new ArrayList<Address>(num);
      for (int i = 0; i < num; i++) {
         byte[] addressBytes = reader.getBytes(21);
         addresses.add(new Address(addressBytes));
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      writer.putIntLE(addresses.size());
      for (Address address : addresses) {
         writer.putBytes(address.getAllAddressBytes());
      }
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.BALANCE_REQUEST_TYPE;
   }

   @Override
   public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append('[');
      sb.append(StringUtils.join(addresses.toArray(), ", "));
      sb.append(']');
      return sb.toString();
   }

}
