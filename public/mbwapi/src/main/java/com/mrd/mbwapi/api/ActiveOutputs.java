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

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.IndependentTransactionOutput;
import com.mrd.bitlib.model.Script.ScriptParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class ActiveOutputs implements Serializable {
   private static final long serialVersionUID = 1L;

   public Address address;
   public Set<IndependentTransactionOutput> unspent;
   public Set<IndependentTransactionOutput> sending;

   public ActiveOutputs(Address address, Set<IndependentTransactionOutput> unspent,
         Set<IndependentTransactionOutput> sending) {
      this.address = address;
      this.unspent = unspent;
      this.sending = sending;
   }

   public ActiveOutputs(ByteReader reader) throws InsufficientBytesException, ApiException {
      byte[] addressBytes = reader.getBytes(21);
      address = new Address(addressBytes);
      try {
         unspent = setFromReader(reader);
         sending = setFromReader(reader);
      } catch (ScriptParsingException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_INVALID_SERVER_RESPONSE,
               "Invalid script returned by server");
      }
   }

   private Set<IndependentTransactionOutput> setFromReader(ByteReader reader) throws InsufficientBytesException,
         ScriptParsingException {
      int size = reader.getIntLE();
      Set<IndependentTransactionOutput> set = new HashSet<IndependentTransactionOutput>();
      for (int i = 0; i < size; i++) {
         set.add(new IndependentTransactionOutput(reader));
      }
      return set;
   }

   private void setToWriter(Set<IndependentTransactionOutput> set, ByteWriter writer) {
      writer.putIntLE(set.size());
      for (IndependentTransactionOutput output : set) {
         output.toByteWriter(writer);
      }
   }

   public ByteWriter toByteWriter(ByteWriter writer) {
      writer.putBytes(address.getAllAddressBytes());
      setToWriter(unspent, writer);
      setToWriter(sending, writer);
      return writer;
   }

}
