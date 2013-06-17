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

package com.mrd.mbw.api;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.Sha256Hash;
import com.mrd.mbw.Constants;
import com.mrd.mbw.api.ApiCache.TransactionInventory;
import com.mrd.mbw.api.ApiCache.TransactionInventory.Item;
import com.mrd.mbwapi.api.ApiError;
import com.mrd.mbwapi.api.ApiException;
import com.mrd.mbwapi.api.Balance;
import com.mrd.mbwapi.api.BitcoinClientApi;
import com.mrd.mbwapi.api.BroadcastTransactionRequest;
import com.mrd.mbwapi.api.BroadcastTransactionResponse;
import com.mrd.mbwapi.api.ExchangeSummary;
import com.mrd.mbwapi.api.QueryBalanceRequest;
import com.mrd.mbwapi.api.QueryExchangeSummaryRequest;
import com.mrd.mbwapi.api.QueryTransactionInventoryRequest;
import com.mrd.mbwapi.api.QueryTransactionInventoryResponse;
import com.mrd.mbwapi.api.QueryTransactionSummaryRequest;
import com.mrd.mbwapi.api.QueryTransactionSummaryResponse;
import com.mrd.mbwapi.api.QueryUnspentOutputsRequest;
import com.mrd.mbwapi.api.QueryUnspentOutputsResponse;
import com.mrd.mbwapi.api.TransactionSummary;

/**
 * This class is an asynchronous wrapper for the MPB Client API. All the public
 * methods are non-blocking. Methods that return an AsyncTask are executing one
 * or more MPB Client API functions in the background. For each of those
 * functions there is a corresponding interface with a call-back function that
 * the caller must implement. This function is called once the AsyncTask has
 * completed or failed.
 */
public abstract class AsynchronousApi {

   public class TransactionSummaryList {

      public List<TransactionSummary> transactions;
      public int chainHeight;

      public TransactionSummaryList(List<TransactionSummary> transactions, int chainHeight) {
         this.transactions = transactions;
         this.chainHeight = chainHeight;
      }
   }

   abstract protected CallbackRunnerInvoker createCallbackRunnerInvoker();

   abstract private class SynchronousFunctionCaller implements Runnable, AsyncTask {

      protected ApiError _error;
      private volatile boolean _canceled;

      @Override
      public void cancel() {
         _canceled = true;
      }

      @Override
      public void run() {
         try {
            callFunction();
         } catch (ApiException e) {
            _error = new ApiError(e.errorCode, e.getMessage());
         } finally {
            if (_canceled) {
               return;
            }
            callback();
         }
      }

      abstract protected void callFunction() throws ApiException;

      abstract protected void callback();

   }

   private abstract class AbstractCaller<T> extends SynchronousFunctionCaller {

      private AbstractCallbackHandler<T> _callbackHandler;
      private CallbackRunnerInvoker _callbackInvoker;
      protected T _response;

      private AbstractCaller(AbstractCallbackHandler<T> callbackHandler) {
         _callbackHandler = callbackHandler;
         _callbackInvoker = createCallbackRunnerInvoker();
      }

      @Override
      protected abstract void callFunction() throws ApiException;

      protected void callback() {
         _callbackInvoker.invoke(new AbstractCallbackRunner<T>(_callbackHandler, _response, _error));
      }
   }

   private static class AbstractCallbackRunner<T> implements Runnable {
      private AbstractCallbackHandler<T> _callbackHandler;
      private T _response;
      private ApiError _error;

      private AbstractCallbackRunner(AbstractCallbackHandler<T> callbackHandler, T response, ApiError error) {
         _callbackHandler = callbackHandler;
         _response = response;
         _error = error;
      }

      @Override
      public void run() {
         _callbackHandler.handleCallback(_response, _error);
      }
   }

   private BitcoinClientApi _api;
   private ApiCache _cache;

   /**
    * Create a new asynchronous API instance.
    * 
    * @param keyRing
    *           The key ring containing all Bitcoin public keys we operate on
    * @param api
    *           The BCCAPI instance used for communicating with the BCCAPI
    *           server.
    * @param accountCache
    *           The account cache instance used.
    */
   public AsynchronousApi(BitcoinClientApi api, ApiCache cache) {
      _api = api;
      _cache = cache;
   }

   private synchronized void executeRequest(SynchronousFunctionCaller caller) {
      Thread thread = new Thread(caller);
      thread.start();
   }

   /**
    * Get balance of a list of bitcoin addresses.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask getBalance(final List<Address> addresses, AbstractCallbackHandler<Balance> callbackHandler) {
      AbstractCaller<Balance> caller = new AbstractCaller<Balance>(callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            _response = _api.queryBalance(new QueryBalanceRequest(addresses)).balance;
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Get balance of a single bitcoin addresses.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask getBalance(final Address address, AbstractCallbackHandler<Balance> callbackHandler) {
      AbstractCaller<Balance> caller = new AbstractCaller<Balance>(callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            _response = _api.queryBalance(new QueryBalanceRequest(address)).balance;
            _cache.setBalance(address, _response);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Get balance of a single bitcoin addresses.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask getExchangeSummary(final String currency, AbstractCallbackHandler<ExchangeSummary[]> callbackHandler) {
      AbstractCaller<ExchangeSummary[]> caller = new AbstractCaller<ExchangeSummary[]>(callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            _response = _api.queryExchangeSummary(new QueryExchangeSummaryRequest(currency)).exchangeSummaries;
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Get the transaction history inventory of a list of bitcoin addresses.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask getTransactionSummary(final Address address,
         AbstractCallbackHandler<QueryTransactionSummaryResponse> callbackHandler) {
      AbstractCaller<QueryTransactionSummaryResponse> caller = new AbstractCaller<QueryTransactionSummaryResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            final List<Address> addresses = new LinkedList<Address>();
            addresses.add(address);
            QueryTransactionInventoryRequest request = new QueryTransactionInventoryRequest(addresses,
                  Constants.TRANSACTION_HISTORY_LENGTH);

            // Get the inventory
            QueryTransactionInventoryResponse inv = _api.queryTransactionInventory(request);
            int chainHeight = inv.chainHeight;

            // Fetch what we can from the cache, the rest we get from the server
            List<TransactionSummary> txList = new LinkedList<TransactionSummary>();
            List<Sha256Hash> hashesToFetch = new LinkedList<Sha256Hash>();
            for (QueryTransactionInventoryResponse.Item item : inv.transactions) {
               // Fetch from cache
               TransactionSummary txSummary = _cache.getTransactionSummary(item.hash.toString());

               if (txSummary == null || txSummary.height == -1 || chainHeight - txSummary.height <= 6) {
                  // Fetch transaction if we don't have it in our cache, if the
                  // cached version is unconfirmed, or if
                  // the height of the cached version is less than 6
                  // confirmations
                  hashesToFetch.add(item.hash);
               } else {
                  txList.add(txSummary);
               }
            }

            // Fetch the rest from the server, if any
            if (hashesToFetch.size() > 0) {
               QueryTransactionSummaryResponse result = _api
                     .queryTransactionSummary(new QueryTransactionSummaryRequest(hashesToFetch));
               txList.addAll(result.transactions);

               // Note that chain height might have changed since the call above
               chainHeight = result.chainHeight;

               // Insert new transactions in cache
               for (TransactionSummary t : result.transactions) {
                  _cache.addTransactionSummary(t);
               }

            }

            // Insert inventory in cache
            _cache.setTransactionInventory(address, toInventory(inv));

            // Sort
            Collections.sort(txList);

            _response = new QueryTransactionSummaryResponse(txList, chainHeight);
         }

      };
      executeRequest(caller);
      return caller;
   }

   private TransactionInventory toInventory(QueryTransactionInventoryResponse response) {
      List<Item> items = new LinkedList<TransactionInventory.Item>();
      for (QueryTransactionInventoryResponse.Item item : response.transactions) {
         items.add(new TransactionInventory.Item(item.hash, item.height));
      }
      return new TransactionInventory(items, response.chainHeight);
   }

   // /**
   // * Get the transaction history inventory of a list of bitcoin addresses.
   // *
   // * @param callbackHandler
   // * The callback handler to call
   // * @return an {@link AsyncTask} instance that allows the caller to cancel
   // the
   // * call back.
   // */
   // public AsyncTask getTransactionInventory(final List<Address> addresses,
   // AbstractCallbackHandler<QueryTransactionInventoryResponse>
   // callbackHandler) {
   // AbstractCaller<QueryTransactionInventoryResponse> caller = new
   // AbstractCaller<QueryTransactionInventoryResponse>(
   // callbackHandler) {
   //
   // @Override
   // protected void callFunction() throws ApiException {
   // QueryTransactionInventoryRequest request = new
   // QueryTransactionInventoryRequest(addresses, 15);
   // _response = _api.queryTransactionInventory(request);
   // }
   //
   // };
   // executeRequest(caller);
   // return caller;
   // }
   //
   // /**
   // * Get the transaction history inventory of a single bitcoin addresses.
   // *
   // * @param callbackHandler
   // * The callback handler to call
   // * @return an {@link AsyncTask} instance that allows the caller to cancel
   // the
   // * call back.
   // */
   // public AsyncTask getTransactionInventory(Address address,
   // AbstractCallbackHandler<QueryTransactionInventoryResponse>
   // callbackHandler) {
   // final List<Address> addresses = new LinkedList<Address>();
   // addresses.add(address);
   // return getTransactionInventory(addresses, callbackHandler);
   // }
   //
   // /**
   // * Get the transaction summary for a list of transaction hashes.
   // *
   // * @param callbackHandler
   // * The callback handler to call
   // * @return an {@link AsyncTask} instance that allows the caller to cancel
   // the
   // * call back.
   // */
   // public AsyncTask getTransactionSummary(final List<Sha256Hash>
   // transactionHashes,
   // AbstractCallbackHandler<QueryTransactionSummaryResponse> callbackHandler)
   // {
   // AbstractCaller<QueryTransactionSummaryResponse> caller = new
   // AbstractCaller<QueryTransactionSummaryResponse>(
   // callbackHandler) {
   //
   // @Override
   // protected void callFunction() throws ApiException {
   // QueryTransactionSummaryRequest request = new
   // QueryTransactionSummaryRequest(transactionHashes);
   // _response = _api.queryTransactionSummary(request);
   // }
   //
   // };
   // executeRequest(caller);
   // return caller;
   // }
   //
   /**
    * Get the unspent outputs of an address.
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask getUnspentOutputs(final Address address,
         AbstractCallbackHandler<QueryUnspentOutputsResponse> callbackHandler) {
      AbstractCaller<QueryUnspentOutputsResponse> caller = new AbstractCaller<QueryUnspentOutputsResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            QueryUnspentOutputsRequest request = new QueryUnspentOutputsRequest(address);
            _response = _api.queryUnspentOutputs(request);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /**
    * Broadcast a transaction
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask broadcastTransaction(final Transaction transaction,
         AbstractCallbackHandler<BroadcastTransactionResponse> callbackHandler) {
      AbstractCaller<BroadcastTransactionResponse> caller = new AbstractCaller<BroadcastTransactionResponse>(
            callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            BroadcastTransactionRequest request = new BroadcastTransactionRequest(transaction);
            _response = _api.broadcastTransaction(request);
         }

      };
      executeRequest(caller);
      return caller;
   }

   /*
    * XXX: All the crap below is a hack to in a crude way connect to the MPB and
    * get invoice PDFs for addresses.
    */

   /**
    * XXX: A very crude quick and dirty implementation for looking up invoices
    * based on Bitcoin addresses
    * 
    * @param callbackHandler
    *           The callback handler to call
    * @return an {@link AsyncTask} instance that allows the caller to cancel the
    *         call back.
    */
   public AsyncTask lookupInvoices(final List<String> invoiceIds,
         AbstractCallbackHandler<Map<String, String>> callbackHandler) {
      AbstractCaller<Map<String, String>> caller = new AbstractCaller<Map<String, String>>(callbackHandler) {

         @Override
         protected void callFunction() throws ApiException {
            HttpURLConnection connection = sendRequest(serialize(invoiceIds));
            _response = receiveResponse(connection);
         }

         private Map<String, String> receiveResponse(HttpURLConnection connection) throws ApiException {
            try {
               int contentLength = connection.getContentLength();
               String string = new String(readBytes(contentLength, connection.getInputStream()));
               return parseResult(string);
            } catch (IOException e) {
               throw new ApiException(BitcoinClientApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
            }

         }

         private Map<String, String> parseResult(String string) throws ApiException {
            Map<String, String> result = new HashMap<String, String>();
            if (!string.startsWith("[") || !string.endsWith("]")) {
               throw new ApiException(BitcoinClientApi.ERROR_CODE_INVALID_SERVER_RESPONSE, "Invalid server response");
            }
            while (true) {
               int startIndex = string.indexOf('{');
               int endIndex = string.indexOf('}');
               if (startIndex == -1 || endIndex == -1) {
                  break;
               }
               String item = string.substring(startIndex + 1, endIndex);
               String[] entry = getEntry(item);
               if (entry == null) {
                  break;
               }
               result.put(entry[0], entry[1]);
               string = string.substring(endIndex + 1);
            }
            return result;
         }

         private String[] getEntry(String item) {
            int startIndex = item.indexOf('"');
            if (startIndex == -1) {
               return null;
            }
            int endIndex = item.indexOf('"', startIndex + 1);
            String address = item.substring(startIndex + 1, endIndex);

            startIndex = item.indexOf('"', endIndex + 1);
            if (startIndex == -1) {
               return null;
            }
            endIndex = item.indexOf('"', startIndex + 1);
            if (endIndex == -1) {
               return null;
            }
            String url = item.substring(startIndex + 1, endIndex);
            return new String[] { address, url };
         }

         private byte[] readBytes(int size, InputStream inputStream) throws IOException {
            byte[] bytes = new byte[size];
            int index = 0;
            int toRead;
            while ((toRead = size - index) > 0) {
               int read = inputStream.read(bytes, index, toRead);
               if (read == -1) {
                  throw new IOException();
               }
               index += read;
            }
            return bytes;
         }

         private String serialize(List<String> ids) {
            StringBuilder sb = new StringBuilder();
            sb.append('[');
            boolean first = true;
            for (String id : ids) {
               if (first) {
                  first = false;
               } else {
                  sb.append(',');
               }
               sb.append('"').append(id).append('"');
            }
            sb.append(']');
            return sb.toString();
         }

         private HttpURLConnection sendRequest(String request) throws ApiException {
            try {
               HttpURLConnection connection = getHttpConnection();
               byte[] toSend = request.getBytes();
               connection.setRequestProperty("Content-Length", String.valueOf(toSend.length));
               connection.getOutputStream().write(toSend);
               int status = connection.getResponseCode();
               if (status != 200) {
                  throw new ApiException(BitcoinClientApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE,
                        "Unexpected status code: " + status);
               }
               int contentLength = connection.getContentLength();
               if (contentLength == -1) {
                  throw new ApiException(BitcoinClientApi.ERROR_CODE_UNEXPECTED_SERVER_RESPONSE,
                        "Invalid content-length");
               }
               return connection;
            } catch (IOException e) {
               throw new ApiException(BitcoinClientApi.ERROR_CODE_COMMUNICATION_ERROR, e.getMessage());
            }
         }

         private HttpURLConnection getHttpConnection() throws IOException {
            URL url = new URL("https://mps.mycelium.com/mpb/1/invoice/lookup");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            configureTrustedCertificate(connection);
            connection.setReadTimeout(60000);
            connection.setDoInput(true);
            connection.setDoOutput(true);
            return connection;
         }

         private void configureTrustedCertificate(URLConnection connection) {
            if (!(connection instanceof HttpsURLConnection)) {
               return;
            }

            HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) connection;
            httpsUrlConnection.setHostnameVerifier(HOST_NAME_VERIFIER);
            httpsUrlConnection.setSSLSocketFactory(SSL_SOCKET_FACTORY);
         }

      };
      executeRequest(caller);
      return caller;
   }

   private static final HostnameVerifier HOST_NAME_VERIFIER;
   private static final SSLSocketFactory SSL_SOCKET_FACTORY;

   static {

      // Used for disabling host name verification. This is safe because we
      // trust the server certificate explicitly
      HOST_NAME_VERIFIER = new HostnameVerifier() {
         @Override
         public boolean verify(String hostname, SSLSession session) {
            return true;
         }
      };

      // Make a trust manager that trusts the specified server certificate and
      // nothing else
      TrustManager[] trustOneCert = new TrustManager[] { new X509TrustManager() {
         public X509Certificate[] getAcceptedIssuers() {
            return null;
         }

         public void checkClientTrusted(X509Certificate[] certs, String authType)
               throws java.security.cert.CertificateException {
            // We do not use a client side certificate
            throw new CertificateException();
         }

         public void checkServerTrusted(X509Certificate[] certs, String authType)
               throws java.security.cert.CertificateException {
            if (certs == null || certs.length == 0) {
               throw new CertificateException();
            }
            for (X509Certificate certificate : certs) {
               String sslThumbprint = generateCertificateThumbprint(certificate);
               if ("B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3".equalsIgnoreCase(sslThumbprint)) {
                  return;
               }
            }
            throw new CertificateException();
         }
      } };

      // Create an SSL socket factory which trusts the BCCAPi server certificate
      try {
         SSLContext sc = SSLContext.getInstance("TLS");
         sc.init(null, trustOneCert, null);
         SSL_SOCKET_FACTORY = sc.getSocketFactory();
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (KeyManagementException e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * Generates an SSL thumbprint from a certificate
    * 
    * @param certificate
    *           The certificate
    * @return The thumbprint of the certificate
    */
   private static String generateCertificateThumbprint(Certificate certificate) {
      try {
         MessageDigest md;
         try {
            md = MessageDigest.getInstance("SHA-1");
         } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
         }
         byte[] encoded;

         try {
            encoded = certificate.getEncoded();
         } catch (CertificateEncodingException e) {
            throw new RuntimeException(e);
         }
         byte[] digest = md.digest(encoded);
         return toHex(digest, 0, digest.length, ":");
      } catch (Exception e) {
         return null;
      }
   }

   /**
    * Encodes an array of bytes as hex symbols.
    * 
    * @param bytes
    *           the array of bytes to encode
    * @param offset
    *           the start offset in the array of bytes
    * @param length
    *           the number of bytes to encode
    * @param separator
    *           the separator to use between two bytes, can be null
    * @return the resulting hex string
    */
   private static String toHex(byte[] bytes, int offset, int length, String separator) {
      StringBuffer result = new StringBuffer();
      for (int i = 0; i < length; i++) {
         int unsignedByte = bytes[i + offset] & 0xff;

         if (unsignedByte < 16) {
            result.append("0");
         }

         result.append(Integer.toHexString(unsignedByte));
         if (separator != null && i + 1 < length) {
            result.append(separator);
         }
      }
      return result.toString();
   }

}
