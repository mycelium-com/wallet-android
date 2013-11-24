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

import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.ErrorMetaData;

/**
 * The Mycelium Bitcoin Wallet API interface. This interface describes all the
 * functions implemented by the Mycelium Wallet Service.
 */
public interface MyceliumWalletApi {

   public static final int ERROR_CODE_PARSER_ERROR = 1;
   public static final int ERROR_CODE_UNKNOWN_TYPE = 2;
   public static final int ERROR_CODE_COMMUNICATION_ERROR = 3;
   public static final int ERROR_CODE_UNEXPECTED_SERVER_RESPONSE = 4;
   public static final int ERROR_CODE_INVALID_SERVER_RESPONSE = 5;
   public static final int ERROR_CODE_INVALID_REQUEST = 6;

   /**
    * The maximal number of addresses allowed per request.
    */
   public static final int MAXIMUM_ADDRESSES_PER_REQUEST = 10;

   /**
    * Get the network used by this API instance.
    * 
    * @return The network used, test network or production network.
    */
   public NetworkParameters getNetwork();

   /**
    * Query the balance of a set of Bitcoin addresses.
    * <p>
    * No more than {@link #MAXIMUM_ADDRESSES_PER_REQUEST} addresses can be
    * queried at a time.
    * 
    * @param request
    *           a {@link QueryBalanceRequest} containing the set of addresses to
    *           query
    * @return a {@link QueryBalanceResponse}.
    * @throws ApiException
    */
   public QueryBalanceResponse queryBalance(QueryBalanceRequest request) throws ApiException;

   /**
    * Query the exchange summary for a currency code.
    * 
    * @param request
    *           a {@link QueryBalanceRequest} containing the set of addresses to
    *           query
    * @return a {@link QueryBalanceResponse}.
    * @throws ApiException
    */
   public QueryExchangeSummaryResponse queryExchangeSummary(QueryExchangeSummaryRequest request) throws ApiException;

   /**
    * Query the unspent outputs of a set of Bitcoin addresses.
    * <p>
    * No more than {@link #MAXIMUM_ADDRESSES_PER_REQUEST} addresses can be
    * queried at a time.
    * 
    * @param request
    *           a {@link QueryUnspentOutputsRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryUnspentOutputsResponse}.
    * @throws ApiException
    */
   public QueryUnspentOutputsResponse queryUnspentOutputs(QueryUnspentOutputsRequest request) throws ApiException;

   /**
    * Query the inventory of active outputs of a set of Bitcoin addresses.
    * <p>
    * No more than {@link #MAXIMUM_ADDRESSES_PER_REQUEST} addresses can be
    * queried at a time. The inventory contains for each address the index of
    * unspent outputs and outputs currently being spent by that address.
    * 
    * @param request
    *           a {@link QueryAddressSetStatusRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryAddressSetStatusResponse}.
    * @throws ApiException
    */
   public QueryAddressSetStatusResponse queryActiveOutputsInventory(QueryAddressSetStatusRequest request)
         throws ApiException;

   /**
    * Get a list of transaction outputs identified by a list of out points.
    * 
    * @param request
    *           a {@link GetTransactionDataRequest} containing the list of out
    *           points to get transaction outputs for
    * @return a {@link GetTransactionDataResponse}.
    * @throws ApiException
    */
   public GetTransactionDataResponse getTransactionData(GetTransactionDataRequest request) throws ApiException;

   /**
    * Query the transaction inventory of a set of Bitcoin addresses.
    * <p>
    * No more than {@link QueryTransactionInventoryRequest#MAXIMUM} transaction
    * IDs can be queried at a time.
    * 
    * @param request
    *           a {@link QueryTransactionInventoryRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryTransactionInventoryResponse}.
    * @throws ApiException
    */
   public QueryTransactionInventoryResponse queryTransactionInventory(QueryTransactionInventoryRequest request)
         throws ApiException;

   /**
    * Extended query of the transaction inventory of a set of Bitcoin addresses.
    * <p>
    * The result contains a list of transaction IDs for each address. The
    * non-extended version returns a combined result. This extended function
    * will be used going forward as it is more optimal for segregated views.
    * <p>
    * No more than {@link QueryTransactionInventoryRequest#MAXIMUM} transaction
    * IDs can be queried at a time for each address.
    * 
    * @param request
    *           a {@link QueryTransactionInventoryRequest} containing the set of
    *           addresses to query
    * @return a {@link QueryTransactionInventoryResponse}.
    * @throws ApiException
    */
   public QueryTransactionInventoryExResponse queryTransactionInventoryEx(QueryTransactionInventoryRequest request)
         throws ApiException;

   /**
    * Query the transaction summary for a list of transaction IDs.
    * <p>
    * No more than {@link QueryTransactionSummaryRequest#MAXIMUM_TRANSACTIONS}
    * transaction IDs can be queried at a time.
    * 
    * @param request
    *           a {@link QueryTransactionSummaryRequest} containing the set of
    *           transaction IDs to query
    * @return a {@link QueryTransactionSummaryResponse}.
    * @throws ApiException
    */
   public QueryTransactionSummaryResponse queryTransactionSummary(QueryTransactionSummaryRequest request)
         throws ApiException;

   /**
    * Broadcast a Bitcoin transaction. The server will validate that the
    * transaction conforms to transaction integrity rules, is funded by unspent
    * transaction outputs, and has been signed appropriately before broadcasting
    * it to the bitcoin network
    * 
    * @param request
    *           a {@link BroadcastTransactionRequest} containing the transaction
    *           to broadcast.
    * @return a {@link BroadcastTransactionResponse}.
    * @throws ApiException
    */
   public BroadcastTransactionResponse broadcastTransaction(BroadcastTransactionRequest request) throws ApiException;

   public ErrorCollectionResponse collectError(Throwable e, String version, ErrorMetaData metaData) throws ApiException;

   ExchangeSummary[] getRate(CurrencyCode currencyCode) throws ApiException;
}
