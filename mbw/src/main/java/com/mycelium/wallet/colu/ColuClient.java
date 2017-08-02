package com.mycelium.wallet.colu;

import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.common.base.Stopwatch;
import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.Transaction;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;
import com.mycelium.wallet.colu.json.AddressInfo;
import com.mycelium.wallet.colu.json.AddressTransactionsInfo;
import com.mycelium.wallet.colu.json.Asset;
import com.mycelium.wallet.colu.json.ColuBroadcastTxid;
import com.mycelium.wallet.colu.json.ColuTransactionRequest;
import com.mycelium.wallet.colu.json.ColuTxDest;
import com.mycelium.wallet.colu.json.ColuTxFlags;
import com.mycelium.wallet.colu.json.Utxo;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import org.json.JSONException;
import org.json.JSONObject;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import android.util.Log;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.*;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Client for the Colu HTTP API.
 */
public class ColuClient {

   private static final String TAG = "ColuClient";

   private static final String MYCELIUM_REFERRAL_CODE = "rrc7dfk";

   private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
   private static final JsonFactory JSON_FACTORY = new JacksonFactory();

   private static final Set<String> SEARCH_CRITERIA = new HashSet<String>();

   public static final boolean coluAutoSelectUtxo = true;

   public static final boolean traceRequests = true;

   public NetworkParameters network;

   static {
      SEARCH_CRITERIA.add("transaction_id");
      SEARCH_CRITERIA.add("type");
      SEARCH_CRITERIA.add("to");
      SEARCH_CRITERIA.add("from");
      SEARCH_CRITERIA.add("extOID");
      SEARCH_CRITERIA.add("situation");
      SEARCH_CRITERIA.add("txhash");
      SEARCH_CRITERIA.add("currency");
   }

   private /* final */ WapiLogger logger = null;

   private HttpRequestFactory requestFactory;

   private final String config;

   private static String MAINNET_COLOREDCOINS_API_URL = "https://api.coloredcoins.org/v3/";
   private static String TESTNET_COLOREDCOINS_API_URL = "http://testnet.api.coloredcoins.org/v3/";
   private static String MAINNET_COLU_BLOCK_EXPLORER_URL = "https://explorer.coloredcoins.org/api/";
   private static String TESTNET_COLU_BLOCK_EXPLORER_URL = "http://testnet.explorer.coloredcoins.org/api/";

   public ColuClient(NetworkParameters network) {
      this.logger = logger;
      this.network = network;
      config = null;
      // Level.CONFIG logs everything but Authorization header
      // Level.ALL also logs Authorization header
      // Type this to really enable: adb shell setprop log.tag.HttpTransport DEBUG
      Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.CONFIG);
      initialize();
   }

   public ColuClient(KeyPair kp, Object eccUtil, //EccUtil eccUtil, 
                     String config, WapiLogger logger) {
      this.logger = logger;
      PublicKey eccPub = kp.getPublic();
      this.config = config;
      initialize();
   }

   private void initialize() {
      Security.addProvider(new BouncyCastleProvider());
      //rng = new SecureRandom();
      Logger.getLogger(HttpTransport.class.getName()).setLevel(Level.CONFIG);
      requestFactory = HTTP_TRANSPORT
              .createRequestFactory(new HttpRequestInitializer() {
                 @Override
                 public void initialize(HttpRequest request) {
                    request.setParser(new JsonObjectParser(JSON_FACTORY));
                 }
              });
   }

   private String getColoredCoinsApiURL() {
      if (this.network.isTestnet()) {
         return TESTNET_COLOREDCOINS_API_URL;
      }
      return MAINNET_COLOREDCOINS_API_URL;
   }

   private String getColuBlockExplorerUrl() {
      if (this.network.isTestnet()) {
         return TESTNET_COLU_BLOCK_EXPLORER_URL;
      }
      return MAINNET_COLU_BLOCK_EXPLORER_URL;
   }

   private <T> T sendGetRequest(Class<T> t, GenericUrl url) throws IOException {
      Stopwatch sw = Stopwatch.createStarted();
      if (requestFactory != null) {
         HttpRequest request = requestFactory.buildGetRequest(url);
         try {
            HttpResponse response = request.execute();
            Log.d("ColuClient", "Colu GET " + url + " [" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms]");
            return response.parseAs(t);
         } catch (IOException ex) {
            Log.d("ColuClient", "Colu ERR GET " + url);
            Log.e("ColuClient", "Colu ERR " + ex.getMessage());
            throw ex;
         }
      } else {
         Log.e("ColuClient", "RequestFactory not initialized ! Bailing out");
         return null;
      }
   }

   private <T> T sendSignedRequest(Class<T> t, String endpoint,
                                   Map<String, String> options) throws
           NoSuchAlgorithmException, IOException {
      return sendECCRequest(t, endpoint, options, false);
   }

   private String getBaseUrl() {
      //return config.getBaseUrl();
      return config;
   }

   private <T> T sendECCRequest(Class<T> t, String endpoint,
                                Map<String, String> options, boolean newAccount)
           throws NoSuchAlgorithmException, IOException {
      GenericUrl url = new GenericUrl(getBaseUrl() + endpoint);
      HttpHeaders headers = new HttpHeaders();

      if (!newAccount) {
         options.put("nonce", generateNonce());
         options.put("endpoint", endpoint);
      } else {
         headers.set("cpt-ecc-new", "");
         //Base64.encodeBase64String(eccPubPEM.getBytes()));

         //this puts our referral code in, for new accounts
         //so we get credited towards developers@mycelium.com on coinapult
         options.put("tag", MYCELIUM_REFERRAL_CODE);
      }
      options.put("timestamp", ColuClient.timestampNow());

      String signdata = Base64.encodeBase64String(JSON_FACTORY
              .toByteArray(options));
      headers.set("cpt-ecc-sign", null // eccUtil.generateSign(signdata, eccPriv)i
      );
      try {
         return makePostRequest(t, url, headers, signdata);
      } catch (IOException ex) {
         logger.logInfo("Coinapult ERR Content " + options.toString());
         throw ex;
      }
   }

   // object is a JSON key value object mapping
   private <T> T makePostRequest(Class<T> t, GenericUrl url,
                                 HttpHeaders headers, Object data) throws IOException {
      Stopwatch sw = Stopwatch.createStarted();
//      Map<String, String> param = new HashMap<String, String>();
      HttpContent content = new JsonHttpContent(new JacksonFactory(), data);
      HttpRequest request = requestFactory.buildPostRequest(url, content);
      if (headers != null) {
         request.setHeaders(headers);
      }
      if (ColuClient.traceRequests) {
         Log.d(TAG, "Attempting POST " + url);
      }
      try {
         HttpResponse response = request.execute();
         //logger.logInfo("Colu POST " + url + " [" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms]");
         Log.d(TAG, "Colu POST " + url + " [" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms]");
         return response.parseAs(t);
      } catch (IOException ex) {
         Log.e(TAG, "Colu ERR POST " + url);
         Log.e(TAG, "Colu ERR " + ex.getMessage());
         //TODO: attempt parsing JSON in error message

         // next line should be JSON
         String lines[] = ex.getMessage().split("\\n+");
         if (lines.length > 1) {

            try {
               // lines[0] should be 500 Internal Server Error. But we get this in status field.
               JSONObject serverAnswer = new JSONObject(lines[1]);
               String errorName = serverAnswer.getString("name");
               String message = serverAnswer.getString("message");
               int errCode = serverAnswer.getInt("code");
               int status = serverAnswer.getInt("status");
               int fee = serverAnswer.getInt("fee");
               int totalCost = serverAnswer.getInt("totalCost");
               int missing = serverAnswer.getInt("missing");
               Log.d(TAG, "Colu transaction info: fee=" + fee + " totalCost=" + totalCost + " missing=" + missing + " status=" + status + " message=" + message);
               if (errCode == 20003) {
                  // insufficient funds. The server tells us how much we need !
                  //TODO: throws ColuInsufficientFundsException
                  Log.e(TAG, "Colu transaction requires " + totalCost + ". Only " + fee + " were provided. Needs " + missing + " more.");
               }
            } catch (JSONException e) {
               Log.e(TAG, "Error parsing JSON server answer: " + e.getMessage());
            }
         }
         throw ex;
      }
   }

   public AddressInfo.Json getBalance(Address address) throws IOException {
      //TODO with colu: HTTPS Handshake failed when switching to TLS
      Log.d("ColuClient", " addressinfo uri is " + getColoredCoinsApiURL() + "addressinfo/" + address.toString());
      GenericUrl url = new GenericUrl(getColoredCoinsApiURL() + "addressinfo/" + address.toString());
      return sendGetRequest(AddressInfo.Json.class, url);
   }

   public AddressTransactionsInfo.Json getAddressTransactions(Address address) throws IOException {
      Log.d("ColuClient", " addressinfowithtransactions uri is " + getColuBlockExplorerUrl() + "getaddressinfowithtransactions?address=" + address.toString());
      GenericUrl url = new GenericUrl(getColuBlockExplorerUrl() + "getaddressinfowithtransactions?address=" + address.toString());
      return sendGetRequest(AddressTransactionsInfo.Json.class, url);
   }

   //TODO: move most of the logic to ColuManager
   public ColuBroadcastTxid.Json prepareTransaction(Address destAddress, List<Address> src,
                                                    ExactCurrencyValue nativeAmount, ColuAccount coluAccount,
                                                    long feePerKb)
           throws IOException {
      Log.d(TAG, "prepareTransaction");
      if (destAddress == null) {
         Log.e(TAG, "destAddress is null");
         return null;
      }
      if (src == null || src.size() == 0) {
         Log.e(TAG, "src is null or empty");
         return null;
      }
      if (nativeAmount == null) {
         Log.e(TAG, "nativeAmount is null");
         return null;
      }
      Log.d(TAG, "destAddress=" + destAddress.toString() + " src nb addr=" + src.size() + " src0=" + src.get(0).toString() + " nativeAmount=" + nativeAmount.toString());
      Log.d(TAG, " feePerKb=" + feePerKb);
      ColuTransactionRequest.Json request = new ColuTransactionRequest.Json();
      List<ColuTxDest.Json> to = new LinkedList<ColuTxDest.Json>();
      ColuTxDest.Json dest = new ColuTxDest.Json();
      dest.address = destAddress.toString();
      BigDecimal amountAssetSatoshi = (nativeAmount.getValue().multiply(new BigDecimal(10).pow(coluAccount.getColuAsset().scale)));
      dest.amount = amountAssetSatoshi.longValue();
      dest.assetId = coluAccount.getColuAsset().id;
      to.add(dest);

      request.to = to;
      request.fee = (int)feePerKb;

      ColuTxFlags.Json flags = new ColuTxFlags.Json();
      flags.splitChange = true;
      request.flags = flags;

      // v1: let colu chose source tx
      if (ColuClient.coluAutoSelectUtxo) {
         LinkedList<String> from = new LinkedList<String>();
         for (Address addr : src) {
            from.add(addr.toString());
         }
         request.from = from;
         request.financeOutputTxid = "";
      } else {
         // v2: chose utxo ourselves
         LinkedList<String> sendutxo = new LinkedList<String>();
         double selectedAmount = 0;
         double selectedSatoshiAmount = 0;
         for (Address addr : src) {
            Log.d(TAG, "Selected address " + addr.toString());
            // get list of address utxo and filter out those who have asset
            List<Utxo.Json> addressUnspent = coluAccount.getAddressUnspent(addr.toString());
            Log.d(TAG, "addressUnspent.size=" + addressUnspent.size());
            for (Utxo.Json utxo : addressUnspent) {
               Log.d(TAG, "Processing " + utxo.txid + ":" + utxo.index);

               // case 1: this is a BTC/satoshi utxo, we select it for fee finance
               // Colu server will only take as much as it needs from the utxo we send it
               if (utxo.assets == null || utxo.assets.size() == 0) {
                  Log.d(TAG, "utxo without asset, use it for fee ");
                  Log.d(TAG, "txid: " + utxo.txid + ":" + utxo.index + " value: " + utxo.value);
                  sendutxo.add(utxo.txid + ":" + utxo.index);
                  selectedSatoshiAmount = selectedSatoshiAmount + utxo.value;
               }
               // case 2: asset utxo. If it is of the type we care, and we need more, select it.
               for (Asset.Json asset : utxo.assets) {
                  Log.d(TAG, "Evaluating asset " + asset.assetId);
                  if (asset.assetId.compareTo(coluAccount.getColuAsset().id) == 0) {
                     if (selectedAmount < dest.amount) {
                        sendutxo.add(utxo.txid + ":" + utxo.index);
                        selectedAmount = selectedAmount + asset.amount;
                        Log.d(TAG, "Selected output " + utxo.txid + ":" + utxo.index +
                                " and added amount " + asset.amount);
                        Log.d(TAG, "Current amount is " + selectedAmount);
                     }
                  } else if (asset.assetId.isEmpty() || asset.assetId.compareTo("") == 0) {
                     Log.d(TAG, "utxo with empty asset, use it for fee ");
                     Log.d(TAG, "txid: " + utxo.txid + ":" + utxo.index + " value: " + utxo.value);
                     sendutxo.add(utxo.txid + ":" + utxo.index);
                     selectedSatoshiAmount = selectedSatoshiAmount + utxo.value;
                  }
               }
            }  // end for
         }
         request.sendutxo = sendutxo;
         // do we need to set this one as well ?
         request.financeOutputTxid = "";
      }
      return makePostRequest(ColuBroadcastTxid.Json.class,
              new GenericUrl(getColoredCoinsApiURL() + "sendasset"),
              null, request);
   }

   public static String bytesToHex(byte[] in) {
      final StringBuilder builder = new StringBuilder();
      for (byte b : in) {
         builder.append(String.format("%02x", b));
      }
      return builder.toString();
   }

   public ColuBroadcastTxid.Json broadcastTransaction(Transaction coluSignedTransaction) throws IOException {
      ColuBroadcastTxid.Json tx = new ColuBroadcastTxid.Json();
      byte[] signedTr = coluSignedTransaction.toBytes();
      tx.txHex = bytesToHex(signedTr);
      Log.d(TAG, "broadcastTransaction: hexbytes=" + tx.txHex);
      return makePostRequest(ColuBroadcastTxid.Json.class,
              new GenericUrl(getColoredCoinsApiURL() + "broadcast"),
              null, tx);
   }


   /**
    * Utility functions.
    */
   public boolean authenticateCallbackECC(String recvSign, String recvData) {
      return true;
   }

   public String generateNonce() {
      byte[] nonce = new byte[10];
      return HexUtils.toHex(nonce);
   }

   static public String timestampNow() {
      return String.valueOf(System.currentTimeMillis() / 1000);
   }

   static public String sha256(String val)
           throws UnsupportedEncodingException, NoSuchAlgorithmException {
      MessageDigest md = MessageDigest.getInstance("SHA-256");
      md.update(val.getBytes("UTF-8"));
      byte[] digest = md.digest();
      return HexUtils.toHex(digest);
   }

   public boolean accountExists() throws Exception {
      return true;
   }
}