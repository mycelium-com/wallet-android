/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

package com.mrd.mbwapi.api;

import java.util.LinkedList;
import java.util.List;

import com.mrd.bitlib.model.IndependentTransactionOutput;
import com.mrd.bitlib.model.Script.ScriptParsingException;
import com.mrd.bitlib.model.SourcedTransactionOutput;
import com.mrd.bitlib.model.Transaction.TransactionParsingException;
import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public class GetTransactionDataResponse extends ApiObject {

   public List<IndependentTransactionOutput> outputs;
   public List<SourcedTransactionOutput> sourcedOutputs;
   public List<byte[]> rawTransactions;

   public GetTransactionDataResponse(List<IndependentTransactionOutput> outputs,
         List<SourcedTransactionOutput> sourcedOutputs, List<byte[]> rawTransactions) {
      this.outputs = outputs;
      this.sourcedOutputs = sourcedOutputs;
      this.rawTransactions = rawTransactions;
   }

   protected GetTransactionDataResponse(ByteReader reader) throws InsufficientBytesException, ApiException {
      try {
         outputs = outputListFromReader(reader);
         sourcedOutputs = sourcedOutputListFromReader(reader);
         rawTransactions = rawTxListFromReader(reader);
      } catch (ScriptParsingException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_INVALID_SERVER_RESPONSE,
               "Invalid script returned by server");
      } catch (TransactionParsingException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_INVALID_SERVER_RESPONSE,
               "Invalid transaction returned by server");
      }
      // Payload may contain more, but we ignore it for forwards
      // compatibility
   }

   private List<SourcedTransactionOutput> sourcedOutputListFromReader(ByteReader reader)
         throws InsufficientBytesException, ApiException, ScriptParsingException {
      int size = reader.getIntLE();
      List<SourcedTransactionOutput> list = new LinkedList<SourcedTransactionOutput>();
      for (int i = 0; i < size; i++) {
         list.add(new SourcedTransactionOutput(reader));
      }
      return list;
   }

   private List<IndependentTransactionOutput> outputListFromReader(ByteReader reader)
         throws InsufficientBytesException, ApiException, ScriptParsingException {
      int size = reader.getIntLE();
      List<IndependentTransactionOutput> list = new LinkedList<IndependentTransactionOutput>();
      for (int i = 0; i < size; i++) {
         list.add(new IndependentTransactionOutput(reader));
      }
      return list;
   }

   private List<byte[]> rawTxListFromReader(ByteReader reader) throws InsufficientBytesException, ApiException,
         ScriptParsingException, TransactionParsingException {
      int length = reader.getIntLE();
      List<byte[]> list = new LinkedList<byte[]>();
      for (int i = 0; i < length; i++) {
         int size = reader.getIntLE();
         byte[] rawTx = reader.getBytes(size);
         list.add(rawTx);
      }
      return list;
   }

   private void sourcedOutputListToWriter(List<SourcedTransactionOutput> list, ByteWriter writer) {
      writer.putIntLE(list.size());
      for (SourcedTransactionOutput item : list) {
         item.toByteWriter(writer);
      }
   }

   private void outputListToWriter(List<IndependentTransactionOutput> list, ByteWriter writer) {
      writer.putIntLE(list.size());
      for (IndependentTransactionOutput item : list) {
         item.toByteWriter(writer);
      }
   }

   private void transactionListToWriter(List<byte[]> list, ByteWriter writer) {
      writer.putIntLE(list.size());
      for (byte[] item : list) {
         writer.putIntLE(item.length);
         writer.putBytes(item);
      }
   }

   @Override
   protected ByteWriter toByteWriter(ByteWriter writer) {
      outputListToWriter(outputs, writer);
      sourcedOutputListToWriter(sourcedOutputs, writer);
      transactionListToWriter(rawTransactions, writer);
      return writer;
   }

   @Override
   protected byte getType() {
      return ApiObject.GET_TRANSACTION_DATA_RESPONSE_TYPE;
   }

}
