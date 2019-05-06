package com.coinapult.api.httpclient;

import com.coinapult.api.httpclient.CoinapultError.CoinapultException;
import com.coinapult.api.httpclient.CoinapultError.CoinapultExceptionECC;
import com.google.api.client.http.*;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.JsonObjectParser;
import com.google.api.client.json.JsonParser;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.common.base.Stopwatch;
import com.google.common.collect.Maps;
import com.mrd.bitlib.util.HexUtils;
import com.mycelium.WapiLogger;
import org.spongycastle.jce.provider.BouncyCastleProvider;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Client for the Coinapult HTTP API.
 *
 * @author Guilherme Polo
 */
public class CoinapultClient {

   private static final String MYCELIUM_REFERRAL_CODE = "rrc7dfk";

   private static final HttpTransport HTTP_TRANSPORT = new NetHttpTransport();
   private static final JsonFactory JSON_FACTORY = new JacksonFactory();

   private static final Set<String> SEARCH_CRITERIA = new HashSet<String>();

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

   private final EccUtil eccUtil;
   private final WapiLogger logger;

   private HttpRequestFactory requestFactory;

   private final PrivateKey eccPriv;
   private final String eccPubPEM;
   private final PublicKey coinapultPubkey;

   private SecureRandom rng;
   private final CoinapultConfig config;

   public CoinapultClient(KeyPair kp, EccUtil eccUtil, CoinapultConfig config, WapiLogger logger) {
      this.eccUtil = eccUtil;
      this.logger = logger;
      PublicKey eccPub = kp.getPublic();
      eccPubPEM = eccUtil.exportToPEM(eccPub);
      eccPriv = kp.getPrivate();
      this.config = config;
      initialize();
      coinapultPubkey = config.getPubKey();
   }

   public CoinapultClient(String privateKeyPay, EccUtil eccUtil, CoinapultConfig config, WapiLogger logger) {
      this(eccUtil.importFromPEM(privateKeyPay), eccUtil, config, logger);
   }

   private void initialize() {
      Security.addProvider(new BouncyCastleProvider());
      rng = new SecureRandom();
      requestFactory = HTTP_TRANSPORT
            .createRequestFactory(new HttpRequestInitializer() {
               @Override
               public void initialize(HttpRequest request) {
                  request.setParser(new JsonObjectParser(JSON_FACTORY));
               }
            });
   }

   private <T> T sendGetRequest(Class<T> t, GenericUrl url) throws IOException {
      Stopwatch sw = Stopwatch.createStarted();
      HttpRequest request = requestFactory.buildGetRequest(url);
      try {
         HttpResponse response = request.execute();
         logger.logInfo("Coinapult GET " + url + " [" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms]");
         return response.parseAs(t);
      } catch (IOException ex) {
         logger.logInfo("Coinapult ERR GET " + url);
         logger.logInfo("Coinapult ERR " + ex.getMessage());
         throw ex;
      }
   }

   private <T> T sendSignedRequest(Class<T> t, String endpoint,
                                   Map<String, String> options) throws
         NoSuchAlgorithmException, IOException {
      return sendECCRequest(t, endpoint, options, false);
   }

   private String getBaseUrl() {
      return config.getBaseUrl();
   }

   private <T> T sendECCRequest(Class<T> t, String endpoint,
                                Map<String, String> options, boolean newAccount)
         throws NoSuchAlgorithmException, IOException {
      GenericUrl url = new GenericUrl(getBaseUrl() + endpoint);
      HttpHeaders headers = new HttpHeaders();

      if (!newAccount) {
         options.put("nonce", generateNonce());
         options.put("endpoint", endpoint);
         headers.set("cpt-ecc-pub", sha256(eccPubPEM));
      } else {
         headers.set("cpt-ecc-new",
               Base64.encodeBase64String(eccPubPEM.getBytes()));
         //this puts our referral code in, for new accounts
         //so we get credited towards developers@mycelium.com on coinapult
         options.put("tag", MYCELIUM_REFERRAL_CODE);
      }
      options.put("timestamp", CoinapultClient.timestampNow());

      String signdata = Base64.encodeBase64String(JSON_FACTORY
            .toByteArray(options));
      headers.set("cpt-ecc-sign", eccUtil.generateSign(signdata, eccPriv));
      try {
         return makePostRequest(t, url, headers, signdata);
      } catch (IOException ex){
         logger.logInfo("Coinapult ERR Content " + options.toString());
         throw ex;
      }
   }

   private <T> T makePostRequest(Class<T> t, GenericUrl url,
                                 HttpHeaders headers, String signdata) throws IOException {
      Stopwatch sw = Stopwatch.createStarted();
      Map<String, String> param = new HashMap<String, String>();
      param.put("data", signdata);
      HttpContent content = new UrlEncodedContent(param);
      HttpRequest request = requestFactory.buildPostRequest(url, content);
      request.setHeaders(headers);
      try {
         HttpResponse response = request.execute();
         logger.logInfo("Coinapult POST " + url + " [" + sw.elapsed(TimeUnit.MILLISECONDS) + "ms]");
         return response.parseAs(t);
      } catch (IOException ex) {
         logger.logInfo("Coinapult ERR POST " + url);
         logger.logInfo("Coinapult ERR " + ex.getMessage());
         throw ex;
      }
   }

   private JsonParser receiveECC(SignedJson resp) throws
         IOException,
         CoinapultExceptionECC {
      if (resp.sign != null && resp.data != null) {
         if (!eccUtil.verifySign(resp.sign, resp.data, coinapultPubkey)) {
            throw new CoinapultExceptionECC("Invalid ECC signature");
         }
         String data = new String(Base64.decodeBase64(resp.data));
         return JSON_FACTORY.createJsonParser(data);
      } else {
         System.out.println(resp.toPrettyString());
         throw new CoinapultExceptionECC("Invalid ECC message");
      }
   }

   /**
    * Requests that do not require authentication.
    */

   public Ticker.Json ticker(String market, String filter) throws IOException {
      String endpoint = "/api/ticker";
      Ticker.Url url = new Ticker.Url(getBaseUrl() + endpoint);
      if (market != null) {
         url.setMarket(market);
      }
      if (filter != null) {
         url.setFilter(filter);
      }
      return sendGetRequest(Ticker.Json.class, url);
   }

   public Ticker.Json ticker() throws IOException {
      return ticker(null, null);
   }

   /**
    * TickerHistory
    *
    * @param market for now only "USD_BTC" is supported for historical tickers
    * @throws IOException
    */
   public TickerHistory.Json tickerHistory(long begin, long end, String market)
         throws IOException {
      String endpoint = "/api/ticker";
      TickerHistory.Url url = new TickerHistory.Url(getBaseUrl() + endpoint);
      if (begin > 0) {
         url.setBegin(begin);
      }
      if (end > 0) {
         url.setEnd(end);
      }
      if (market != null) {
         url.setMarket(market);
      }
      return sendGetRequest(TickerHistory.Json.class, url);
   }

   public TickerHistory.Json tickerHistory(long begin, long end)
         throws IOException {
      return tickerHistory(begin, end, null);
   }

   /**
    * Requests that require authentication.
    */
   public Transaction.Json send(Number amount, String inputCurrency,
                                String address, Number outAmount, String callback,
                                String extOID,
                                String otp) throws IOException,
         NoSuchAlgorithmException {
      String endpoint = "/api/t/send";

      BigDecimal inputAmount = new BigDecimal(amount.toString());
      BigDecimal outputAmount = new BigDecimal(outAmount.toString());

      Map<String, String> options = new HashMap<String, String>();
      if (inputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("amount", inputAmount.toString());
      }
      options.put("currency", inputCurrency);
      options.put("address", address);
      if (outputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("outAmount", outputAmount.toString());
      }
      if (callback != null) {
         options.put("callback", callback);
      }
      if (extOID != null) {
         options.put("extOID", extOID);
      }
      if (otp != null) {
         options.put("otp", otp);
      }

      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   public Transaction.Json convert(Number amount, String currency,
                                   Number outAmount, String outCurrency, String callback)
         throws IOException,
         NoSuchAlgorithmException {
      String endpoint = "/api/t/convert";

      BigDecimal inputAmount = new BigDecimal(amount.toString());
      BigDecimal outputAmount = new BigDecimal(outAmount.toString());

      Map<String, String> options = new HashMap<String, String>();
      if (inputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("amount", inputAmount.toString());
      }
      options.put("currency", currency);
      if (outputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("outAmount", outputAmount.toString());
      }
      options.put("outCurrency", outCurrency);
      if (callback != null) {
         options.put("callback", callback);
      }

      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   /**
    * Search.
    *
    * @param criteria this will be modified, pass a copy if necessary.
    */
   public Transaction.Json search(Map<String, String> criteria)
         throws IOException,
         NoSuchAlgorithmException, CoinapultException {
      String endpoint = "/api/t/search";

      Map<String, String> options;
      if (criteria.size() > 0
            && SEARCH_CRITERIA.containsAll(criteria.keySet())) {
         options = criteria;
      } else {
         throw new CoinapultException("Invalid search criteria");
      }

      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   public SearchMany.Json searchMany(Map<String, String> criteria, int page)
         throws IOException,
         NoSuchAlgorithmException, CoinapultException {
      String endpoint = "/api/t/search";

      Map<String, String> options;
      if (criteria.size() > 0
            && SEARCH_CRITERIA.containsAll(criteria.keySet())) {
         options = criteria;
      } else {
         throw new CoinapultError.CoinapultException(
               "Invalid search criteria");
      }
      options.put("many", "1");
      if (page > 0) {
         options.put("page", String.valueOf(page));
      }

      return sendSignedRequest(SearchMany.Json.class,
            endpoint, options);
   }

   public SearchMany.Json history(int page) throws CoinapultBackendException {
      SearchMany.Json result;
      try {
         String endpoint = "/api/t/search";

         Map<String, String> options = Maps.newHashMap();
         options.put("many", "1");
         options.put("page", String.valueOf(page));
         result = sendSignedRequest(SearchMany.Json.class,
               endpoint, options);

      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (IOException e) {
         throw new CoinapultBackendException(e);
      }
      return result;
   }

   public Transaction.Json lock(Number amount, Number outAmount,
                                String outCurrency, String callback) throws IOException,
         NoSuchAlgorithmException {
      String endpoint = "/api/t/lock";

      BigDecimal inputAmount = new BigDecimal(amount.toString());
      BigDecimal outputAmount = new BigDecimal(outAmount.toString());

      Map<String, String> options = new HashMap<String, String>();
      if (inputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("amount", inputAmount.toString());
      }
      if (outputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("outAmount", outputAmount.toString());
      }
      options.put("currency", outCurrency);
      if (callback != null) {
         options.put("callback", callback);
      }

      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   /**
    * Unlock.
    *
    * @param address   If specified the amount unlocked will be sent to the given
    *                  address, otherwise the amount will be credited to your
    *                  Coinapult wallet.
    * @param acceptNow If true, the unlock will be performed right away. If false,
    *                  it's possible to first check the operation that would be
    *                  performed and then it can be confirmed by using
    *                  CoinapultClient.unlockConfirm.
    */
   public Transaction.Json unlock(Number amount, String inCurrency,
                                  Number outAmount, String address, String callback,
                                  boolean acceptNow)
         throws IOException,
         NoSuchAlgorithmException {
      String endpoint = "/api/t/unlock";

      BigDecimal inputAmount = new BigDecimal(amount.toString());
      BigDecimal outputAmount = new BigDecimal(outAmount.toString());

      Map<String, String> options = new HashMap<String, String>();
      if (inputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("amount", inputAmount.toString());
      }
      options.put("currency", inCurrency);
      if (outputAmount.compareTo(BigDecimal.ZERO) > 0) {
         options.put("outAmount", outputAmount.toString());
      }
      if (address != null) {
         options.put("address", address);
      }
      if (callback != null) {
         options.put("callback", callback);
      }
      options.put("acceptNow", acceptNow ? "1" : "0");

      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   public Transaction.Json unlockConfirm(String tid) throws IOException,
         NoSuchAlgorithmException {
      String endpoint = "/api/t/unlock/confirm";

      Map<String, String> options = new HashMap<String, String>();
      options.put("transaction_id", tid);
      return sendSignedRequest(Transaction.Json.class,
            endpoint, options);
   }

   /**
    * Get a new bitcoin address.
    */
   public Address.Json getBitcoinAddress() throws
         NoSuchAlgorithmException, IOException {
      String endpoint = "/api/getBitcoinAddress";

      Map<String, String> options = new HashMap<String, String>();
      return sendSignedRequest(Address.Json.class, endpoint,
            options);
   }

   public Config.Json config(String address, String lockTo) throws
         NoSuchAlgorithmException, IOException {
      String endpoint = "/api/address/config";

      Map<String, String> options = new HashMap<String, String>();
      options.put("address", address);
      options.put("lockTo", lockTo);
      return sendSignedRequest(Config.Json.class, endpoint,
            options);
   }


   /**
    * Account information.
    *
    * @param balanceType one of "all", "normal", "locks"
    */
   public AccountInfo.Json accountInfo(String balanceType, boolean locksAsBTC)
         throws
         NoSuchAlgorithmException, IOException {
      String endpoint = "/api/accountInfo";

      Map<String, String> options = new HashMap<String, String>();
      options.put("balanceType", balanceType);
      options.put("locksAsBTC", locksAsBTC ? "1" : "0");
      return sendSignedRequest(AccountInfo.Json.class,
            endpoint, options);
   }

   public AccountInfo.Json accountInfo() throws
         NoSuchAlgorithmException, IOException {
      return accountInfo("all", false);
   }

   /**
    * Verify if an address belongs to the current account.
    */
   public AddressInfo.Json accountAddress(String address)
         throws
         NoSuchAlgorithmException, IOException {
      String endpoint = "/api/accountInfo/address";

      Map<String, String> options = new HashMap<String, String>();
      options.put("address", address);
      return sendSignedRequest(AddressInfo.Json.class,
            endpoint, options);
   }

   /**
    * Utility functions.
    */
   public boolean authenticateCallbackECC(String recvSign, String recvData) {
      return eccUtil.verifySign(recvSign, recvData, coinapultPubkey);
   }


   public String generateNonce() {
      byte[] nonce = new byte[10];
      rng.nextBytes(nonce);
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

   public boolean accountExists() throws CoinapultBackendException {
      try {
         accountInfo(); //ignored return value, just checking for exception
      } catch (NoSuchAlgorithmException e) {
         throw new RuntimeException(e);
      } catch (HttpResponseException e) {
         return false;
      } catch (IOException e) {
         throw new CoinapultBackendException(e);
      }
      return true;
   }

   public EmailAddress.Json setMail(String mail) throws IOException, NoSuchAlgorithmException {
      String endpoint = "/api/accountInfo/email";
      Map<String, String> options = new HashMap<String, String>();
      options.put("email", mail);
      options.put("setPrimary", "True");
      return sendSignedRequest(EmailAddress.Json.class, endpoint, options);
   }

   public EmailAddress.Json verifyMail(String link, String email) throws IOException, NoSuchAlgorithmException {
      String endpoint = "/api/accountInfo/email";
      Map<String, String> options = new HashMap<String, String>();
      options.put("verify", link);
      options.put("email", email);
      return sendSignedRequest(EmailAddress.Json.class, endpoint, options);
   }

   public static class CoinapultBackendException extends Exception {
      public CoinapultBackendException() {
      }

      public CoinapultBackendException(String message) {
         super(message);
      }

      public CoinapultBackendException(String message, Throwable cause) {
         super(message, cause);
      }

      public CoinapultBackendException(Throwable cause) {
         super(cause);
      }
   }
}
