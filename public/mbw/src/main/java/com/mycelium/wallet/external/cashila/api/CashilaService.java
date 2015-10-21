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

package com.mycelium.wallet.external.cashila.api;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.base.Optional;
import com.mrd.bitlib.crypto.Hmac;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.api.retrofit.JacksonConverter;
import com.mycelium.wallet.bitid.BitIDSignRequest;
import com.mycelium.wallet.bitid.BitIdAuthenticator;
import com.mycelium.wallet.bitid.BitIdResponse;
import com.mycelium.wallet.external.cashila.ApiException;
import com.mycelium.wallet.external.cashila.ApiExceptionAuth;
import com.mycelium.wallet.external.cashila.api.request.CreateBillPay;
import com.mycelium.wallet.external.cashila.api.request.GetDeepLink;
import com.mycelium.wallet.external.cashila.api.response.*;
import com.mycelium.wapi.api.WapiJsonModule;
import com.squareup.okhttp.*;
import com.squareup.otto.Bus;
import okio.Buffer;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class CashilaService {
   private final static String API_CLIENT_ID = "61d9b5fd-9018-4654-bc7a-d3c7b87d9ada";
   private static final String HEADER_API_CLIENT = "API-Client";
   private static final String HEADER_API_USER = "API-User";
   private static final String HEADER_API_NONCE = "API-Nonce";
   private static final String HEADER_API_SIGN = "API-Sign";

   public static final String DEEP_LINK_DASHBOARD = "dashboard";
   public static final String DEEP_LINK_ADD_RECIPIENT = "recipients/add";
   public static final String CASHILA_CERT = "sha1/cl8wPAZF71fZyBWNmh5tvVV5UYM=";
   public static final String CASHILA_CERT_STAGE = "sha1/xxFB1MhSNsKWMxub9YFki5Wm/XM=";

   private final String baseUrl;
   private final Bus eventBus;
   private final ObjectMapper objectMapper;
   private long lastNonce;
   private Object nonceSynce = new Object();
   private ApiSecretToken securityToken;
   private Cashila cashila;


   public CashilaService(String baseUrl, String apiVersion, Bus eventBus) {
      this.baseUrl = baseUrl;
      this.eventBus = eventBus;

      OkHttpClient client = new OkHttpClient();
      client.setConnectTimeout(5000, TimeUnit.MILLISECONDS);
      client.setReadTimeout(5000, TimeUnit.MILLISECONDS);
      client.networkInterceptors().add(hmacInterceptor);
      CertificatePinner certPinner = new CertificatePinner.Builder()
            .add("cashila.com", CASHILA_CERT)
            .add("cashila-staging.com", CASHILA_CERT_STAGE)
            .build();
      client.setCertificatePinner(certPinner);

      // use jackson as json mapper, as we already use it in the rest of the project
      objectMapper = new ObjectMapper();
      objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
      objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.CAMEL_CASE_TO_LOWER_CASE_WITH_UNDERSCORES);
      objectMapper.registerModule(new WapiJsonModule());

      RestAdapter adapter = new RestAdapter.Builder()
            .setEndpoint(baseUrl + apiVersion + "/")
            .setLogLevel(RestAdapter.LogLevel.BASIC)
            //.setLogLevel(RestAdapter.LogLevel.FULL)
            .setConverter(new JacksonConverter(objectMapper))
            .setClient(new OkClient(client))
            .setRequestInterceptor(apiIdInterceptor)
            .build();

      cashila = adapter.create(Cashila.class);

      // initialise nonce with current time and increase by one on each call
      lastNonce = System.currentTimeMillis();
   }

   private volatile Observable<ApiSecretToken> requestToken = null;

   public synchronized Observable<ApiSecretToken> getSecurityToken() {
      if (requestToken == null) {
         // Call getRequestToken, this should return a BitID-URI
         // We authenticate against it and get back a random security token - with this we HMAC all
         // our future API calls
         requestToken = cashila.getRequestToken()
               .observeOn(Schedulers.newThread())
               .map(new Func1<CashilaResponse<RequestToken>, RequestToken>() {
                  @Override
                  public RequestToken call(CashilaResponse<RequestToken> requestTokenCashilaResponse) {
                     if (requestTokenCashilaResponse.isError()) {
                        throw new ApiExceptionAuth(requestTokenCashilaResponse);
                     }
                     return requestTokenCashilaResponse.result;
                  }
               }).map(new Func1<RequestToken, ApiSecretToken>() {
                  @Override
                  public ApiSecretToken call(RequestToken requestToken) {
                     // execute the BitID Login scheme on the returned uri
                     Optional<BitIDSignRequest> request = BitIDSignRequest.parse(Uri.parse(requestToken.uri));
                     if (!request.isPresent()) {
                        new ApiExceptionAuth("Unable to parse the BitID request");
                     }

                     MbwManager manager = MbwManager.getInstance(null);
                     String websiteId = baseUrl + "v1/bitid/pair";
                     InMemoryPrivateKey key = manager.getBitIdKeyForWebsite(websiteId);
                     Address address = key.getPublicKey().toAddress(manager.getNetwork());

                     // use the BitID authenticator to get the temporary cashila security token for all next api calls
                     BitIdAuthenticator authenticator = new BitIdAuthenticator(request.get(), true, key, address) {
                        @Override
                        protected Request getRequest(SignedMessage signature) {
                           Request request = super.getRequest(signature);
                           return request.newBuilder().addHeader("API-Client", API_CLIENT_ID).build();
                        }
                     };
                     BitIdResponse bitIdResponse = authenticator.queryServer();

                     if (bitIdResponse.status == BitIdResponse.ResponseStatus.SUCCESS) {

                        WrappedApiSecretToken apiSecurityToken = null;
                        try {
                           apiSecurityToken = objectMapper.readValue(bitIdResponse.message, WrappedApiSecretToken.class);
                        } catch (IOException e) {
                           Log.e("cashila", "error while deserialize bitid response", e);
                           apiSecurityToken = new WrappedApiSecretToken(e);
                        }

                        if (apiSecurityToken.isError()) {
                           throw new ApiExceptionAuth(apiSecurityToken.error.message);
                        }
                        return apiSecurityToken.result;
                     } else {
                        throw new ApiException("BitID query failed, " + bitIdResponse.toString());
                     }
                  }
               })
               .doOnError(new Action1<Throwable>() {
                  @Override
                  public void call(Throwable throwable) {
                     // reset everything to retry later
                     securityToken = null;
                     requestToken = null;
                  }
               })
               .observeOn(AndroidSchedulers.mainThread())
               .filter(new Func1<ApiSecretToken, Boolean>() {
                  @Override
                  public Boolean call(ApiSecretToken apiSecretToken) {
                     securityToken = apiSecretToken;
                     return true;
                  }
               })
               .cache();
         //.retry(2);

      }

      return requestToken;
   }

   public Observable<DeepLink> getDeepLink(final String resource) {
      return new ApiCaller<DeepLink>() {
         @Override
         Observable<CashilaResponse<DeepLink>> apiCall() {
            Observable<CashilaResponse<DeepLink>> deepLink = getApi().getDeepLink(new GetDeepLink(resource));
            return deepLink;
         }
      }.call();
   }

   // used for json de-serialisation
   private static class WrappedApiSecretToken extends CashilaResponse<ApiSecretToken> {
      private WrappedApiSecretToken() {
      }

      private WrappedApiSecretToken(Throwable e) {
         this.error = new Error();
         this.error.message = e.getMessage();
      }
   }

   Observable<CashilaResponse<List<BillPayRecentRecipient>>> billPaysRecentCache;

   // Call ApiMethods with a new security Token ensured
   public Observable<List<BillPayRecentRecipient>> getBillPaysRecent(final boolean fromCache) {
      return new ApiCaller<List<BillPayRecentRecipient>>() {
         @Override
         Observable<CashilaResponse<List<BillPayRecentRecipient>>> apiCall() {
            if (fromCache && billPaysRecentCache != null) {
               return billPaysRecentCache;
            } else {
               Observable<CashilaResponse<List<BillPayRecentRecipient>>> billPaysRecent = getApi().getBillPaysRecent();
               billPaysRecentCache = billPaysRecent.cache();
               return billPaysRecent;
            }
         }
      }.call();
   }

   public Observable<BillPay> createBillPay(final UUID newPaymentId, final CreateBillPay createBillPayRequest) {
      return new ApiCaller<BillPay>() {
         @Override
         Observable<CashilaResponse<BillPay>> apiCall() {
            return getApi().createBillPay(newPaymentId, createBillPayRequest);
         }
      }.call();
   }

   public Observable<BillPay> reviveBillPay(final UUID paymentId) {
      return new ApiCaller<BillPay>() {
         @Override
         Observable<CashilaResponse<BillPay>> apiCall() {
            return getApi().reviveBillPay(paymentId);
         }
      }.call();
   }

   public Observable<List<BillPay>> getBillPays() {
      return new ApiCaller<List<BillPay>>() {
         @Override
         Observable<CashilaResponse<List<BillPay>>> apiCall() {
            return getApi().getBillPays("pending,expired,transcribed");
         }
      }.call();
   }

   public Observable<List<BillPay>> getAllBillPays() {
      return new ApiCaller<List<BillPay>>() {
         @Override
         Observable<CashilaResponse<List<BillPay>>> apiCall() {
            return getApi().getBillPays();
         }
      }.call();
   }

   public Observable<List<Void>> deleteBillPay(final UUID paymentId) {
      return new ApiCaller<List<Void>>() {
         @Override
         Observable<CashilaResponse<List<Void>>> apiCall() {
            return getApi().deleteBillPay(paymentId);
         }
      }.call();
   }

   private abstract class ApiCaller<T> {
      public Observable<T> call() {
         Observable<CashilaResponse<T>> ret;
         // if no security token is available, first get it and call the requested api afterwards ...
         if (securityToken == null) {
            ret = getSecurityToken()
                  .flatMap(new Func1<ApiSecretToken, Observable<CashilaResponse<T>>>() {
                     @Override
                     public Observable<CashilaResponse<T>> call(ApiSecretToken apiSecretToken) {
                        return apiCall()
                              .observeOn(Schedulers.newThread());
                     }
                  });
         } else {
            // ... otherwise, call the api directly
            ret = apiCall()
                  .doOnError(new Action1<Throwable>() {
                     @Override
                     public void call(Throwable throwable) {
                        // On Api error, drop the security token and request a new one on the next call
                        if (throwable instanceof ApiException) {
                           // todo: make it more granular, when we want to drop the security token
                           securityToken = null;
                        }
                     }
                  })
                  .observeOn(Schedulers.newThread());
         }

         return ret
               .map(new CashilaResponseUnwrapper<T>())
               .observeOn(AndroidSchedulers.mainThread())
               .doOnError(new Action1<Throwable>() {
                             @Override
                             public void call(Throwable throwable) {
                                broadcastErrorHandler.call(throwable);
                             }
                          }
               );
      }

      abstract Observable<CashilaResponse<T>> apiCall();

      private class CashilaResponseUnwrapper<T> implements Func1<CashilaResponse<T>, T> {
         @Override
         public T call(CashilaResponse<T> tCashilaResponse) {
            if (tCashilaResponse.isError()) {
               throw new ApiException(tCashilaResponse);
            } else {
               return tCashilaResponse.result;
            }
         }
      }
   }

   public Cashila getApi() {
      return cashila;
   }

   private String getApiUriPathSegment(final String url) {
      if (url.startsWith(baseUrl)) {
         // only return everything after the base path
         return url.substring(baseUrl.length() - 1);
      } else {
         return url;
      }
   }

   // Retrofit interceptor
   private final RequestInterceptor apiIdInterceptor = new RequestInterceptor() {
      @Override
      public void intercept(RequestFacade request) {
         request.addHeader(HEADER_API_CLIENT, API_CLIENT_ID);
      }
   };

   // OkHttp Client interceptor for the hmac-auth
   private final Interceptor hmacInterceptor = new Interceptor() {
      @Override
      public Response intercept(Chain chain) throws IOException {
         Request request = chain.request();

         if (securityToken != null) {
            byte[] decodedSecret = Base64.decode(securityToken.secret, Base64.DEFAULT);
            Request.Builder requestBuilder = request.newBuilder();

            String uriPath = getApiUriPathSegment(request.urlString());
            String method = request.method().toUpperCase();
            synchronized (nonceSynce) {
               String nonce = String.valueOf(getNextNonce());

               ByteWriter hashBytes = new ByteWriter(1024);
               hashBytes.putRawStringUtf8(nonce);

               if (request.body() != null && request.body().contentLength() > 0) {
                  Buffer bodySink = new Buffer();
                  request.body().writeTo(bodySink);
                  hashBytes.putBytes(bodySink.readByteArray());
               }

               Sha256Hash contentHash = HashUtils.sha256(hashBytes.toBytes());

               ByteWriter hmacBytes = new ByteWriter(1024);

               // put method + uripath into hash
               hmacBytes.putRawStringUtf8(method + uriPath);

               // append the content hash
               hmacBytes.putBytes(contentHash.getBytes());

               byte[] hmac = Hmac.hmacSha512(decodedSecret, hmacBytes.toBytes());

               String hmacString = Base64.encodeToString(hmac, Base64.NO_WRAP);
               request = requestBuilder
                     .header(HEADER_API_USER, securityToken.token)
                     .header(HEADER_API_NONCE, nonce)
                     .header(HEADER_API_SIGN, hmacString)
                     .build();

               Response response = chain.proceed(request);
               return response;
            }
         } else {
            Response response = chain.proceed(request);
            return response;
         }
      }
   };

   private synchronized long getNextNonce() {
      return lastNonce++;
   }

   private final Action1<Throwable> broadcastErrorHandler = new Action1<Throwable>() {
      @Override
      public void call(Throwable throwable) {
         if (throwable instanceof ApiException) {
            eventBus.post(throwable);
         } else {
            //throw new RuntimeException(throwable);
            Log.e("cashila", "Error", throwable);
         }
      }
   };
}
