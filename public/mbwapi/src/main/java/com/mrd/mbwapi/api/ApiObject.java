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

package com.mrd.mbwapi.api;

import com.mrd.bitlib.util.ByteReader;
import com.mrd.bitlib.util.ByteReader.InsufficientBytesException;
import com.mrd.bitlib.util.ByteWriter;

public abstract class ApiObject {

   protected static final byte ERROR_TYPE = (byte) 0x01;
   protected static final byte BALANCE_TYPE = (byte) 0x02;
   protected static final byte TRANSACTION_SUMMARY_TYPE = (byte) 0x03;
   protected static final byte BALANCE_REQUEST_TYPE = (byte) 0x04;
   protected static final byte BALANCE_RESPONSE_TYPE = (byte) 0x5;
   protected static final byte UNSPENT_OUTPUTS_REQUEST_TYPE = (byte) 0x06;
   protected static final byte UNSPENT_OUTPUTS_RESPONSE_TYPE = (byte) 0x07;
   protected static final byte TRANSACTION_INVENTORY_REQUEST_TYPE = (byte) 0x08;
   protected static final byte TRANSACTION_INVENTORY_RESPONSE_TYPE = (byte) 0x09;
   protected static final byte ERROR_REQUEST_TYPE = (byte) 0xA;
   protected static final byte ERROR_RESPONSE_TYPE = (byte) 0xB;
   protected static final byte TRANSACTION_SUMMARY_REQUEST_TYPE = (byte) 0x10;
   protected static final byte TRANSACTION_SUMMARY_RESPONSE_TYPE = (byte) 0x11;
   protected static final byte BROADCAST_TRANSACTION_REQUEST_TYPE = (byte) 0x12;
   protected static final byte BROADCAST_TRANSACTION_RESPONSE_TYPE = (byte) 0x13;
   protected static final byte EXCHANGE_TRADE_SUMMARY_TYPE = (byte) 0x14;
   protected static final byte EXCHANGE_SUMMARY_REQUEST_TYPE = (byte) 0x15;
   protected static final byte EXCHANGE_SUMMARY_RESPONSE_TYPE = (byte) 0x16;
   protected static final byte ADDRESS_SET_STATUS_REQUEST_TYPE = (byte) 0x17;
   protected static final byte ADDRESS_SET_STATUS_RESPONSE_TYPE = (byte) 0x18;
   protected static final byte GET_TRANSACTION_DATA_REQUEST_TYPE = (byte) 0x19;
   protected static final byte GET_TRANSACTION_DATA_RESPONSE_TYPE = (byte) 0x20;
   protected static final byte TRANSACTION_INVENTORY_EX_RESPONSE_TYPE = (byte) 0x21;
   protected static final byte WALLET_VERSION_REQUEST_TYPE = (byte) 0x22;
   protected static final byte WALLET_VERSION_RESPONSE_TYPE = (byte) 0x23;
   protected static final byte EXCHANGE_RATES_REQUEST_TYPE = (byte) 0x24;
   protected static final byte EXCHANGE_RATES_RESPONSE_TYPE = (byte) 0x25;
   protected static final byte EXCHANGE_RATE_TYPE = (byte) 0x26;

   public final ByteWriter serialize(ByteWriter writer) {
      byte[] payload = toByteWriter(new ByteWriter(1024)).toBytes();
      writer.put(getType());
      writer.putIntLE(payload.length);
      writer.putBytes(payload);
      return writer;
   }

   private static ApiObject deserialize(ByteReader reader) throws ApiException {
      try {
         byte type = reader.get();
         int length = reader.getIntLE();
         byte[] payload = reader.getBytes(length);
         ByteReader payloadReader = new ByteReader(payload);
         if (type == ERROR_TYPE) {
            return new ApiError(payloadReader);
         } else if (type == BALANCE_TYPE) {
            return new Balance(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_TYPE) {
            return new TransactionSummary(payloadReader);
         } else if (type == BALANCE_REQUEST_TYPE) {
            return new QueryBalanceRequest(payloadReader);
         } else if (type == BALANCE_RESPONSE_TYPE) {
            return new QueryBalanceResponse(payloadReader);
         } else if (type == UNSPENT_OUTPUTS_REQUEST_TYPE) {
            return new QueryUnspentOutputsRequest(payloadReader);
         } else if (type == UNSPENT_OUTPUTS_RESPONSE_TYPE) {
            return new QueryUnspentOutputsResponse(payloadReader);
         } else if (type == TRANSACTION_INVENTORY_REQUEST_TYPE) {
            return new QueryTransactionInventoryRequest(payloadReader);
         } else if (type == TRANSACTION_INVENTORY_RESPONSE_TYPE) {
            return new QueryTransactionInventoryResponse(payloadReader);
         } else if (type == TRANSACTION_INVENTORY_EX_RESPONSE_TYPE) {
            return new QueryTransactionInventoryExResponse(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_REQUEST_TYPE) {
            return new QueryTransactionSummaryRequest(payloadReader);
         } else if (type == TRANSACTION_SUMMARY_RESPONSE_TYPE) {
            return new QueryTransactionSummaryResponse(payloadReader);
         } else if (type == BROADCAST_TRANSACTION_REQUEST_TYPE) {
            return new BroadcastTransactionRequest(payloadReader);
         } else if (type == BROADCAST_TRANSACTION_RESPONSE_TYPE) {
            return new BroadcastTransactionResponse(payloadReader);
         } else if (type == EXCHANGE_TRADE_SUMMARY_TYPE) {
            return new ExchangeSummary(payloadReader);
         } else if (type == EXCHANGE_SUMMARY_REQUEST_TYPE) {
            return new QueryExchangeSummaryRequest(payloadReader);
         } else if (type == EXCHANGE_SUMMARY_RESPONSE_TYPE) {
            return new QueryExchangeSummaryResponse(payloadReader);
         } else if (type == ERROR_REQUEST_TYPE) {
            return new ErrorCollectionRequest(payloadReader);
         } else if (type == ERROR_RESPONSE_TYPE) {
            return new ErrorCollectionResponse(payloadReader);
         } else if (type == ADDRESS_SET_STATUS_REQUEST_TYPE) {
            return new QueryAddressSetStatusRequest(payloadReader);
         } else if (type == ADDRESS_SET_STATUS_RESPONSE_TYPE) {
            return new QueryAddressSetStatusResponse(payloadReader);
         } else if (type == GET_TRANSACTION_DATA_REQUEST_TYPE) {
            return new GetTransactionDataRequest(payloadReader);
         } else if (type == GET_TRANSACTION_DATA_RESPONSE_TYPE) {
            return new GetTransactionDataResponse(payloadReader);
         } else if (type == WALLET_VERSION_REQUEST_TYPE) {
            return new WalletVersionRequest(payloadReader);
         } else if (type == WALLET_VERSION_RESPONSE_TYPE) {
            return new WalletVersionResponse(payloadReader);
         } else if (type == EXCHANGE_RATES_REQUEST_TYPE) {
            return new QueryExchangeRatesRequest(payloadReader);
         } else if (type == EXCHANGE_RATES_RESPONSE_TYPE) {
            return new QueryExchangeRatesResponse(payloadReader);
         } else if (type == EXCHANGE_RATE_TYPE) {
            return new ExchangeRate(payloadReader);
         } else {
            throw new ApiException(MyceliumWalletApi.ERROR_CODE_UNKNOWN_TYPE, "Error deserializing server response");
         }
      } catch (InsufficientBytesException e) {
         throw new ApiException(MyceliumWalletApi.ERROR_CODE_PARSER_ERROR, "Unable to parse API object",e);
      }
   }

   @SuppressWarnings("unchecked")
   public static <T> T deserialize(Class<T> klass, ByteReader reader) throws ApiException {
      ApiObject obj = deserialize(reader);
      if (obj.getClass().equals(klass)) {
         return (T) obj;
      } else if (obj.getClass().equals(ApiError.class)) {
         throw new ApiException((ApiError) obj);
      }
      throw new ApiException(MyceliumWalletApi.ERROR_CODE_UNKNOWN_TYPE, "Error deserializing server response");
   }

   protected abstract ByteWriter toByteWriter(ByteWriter writer);

   protected abstract byte getType();

}
