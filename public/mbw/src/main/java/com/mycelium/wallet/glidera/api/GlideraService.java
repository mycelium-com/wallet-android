package com.mycelium.wallet.glidera.api;

import android.net.Uri;
import android.support.annotation.NonNull;
import android.util.Log;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.base.Charsets;
import com.mrd.bitlib.crypto.Hmac;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.api.retrofit.JacksonConverter;
import com.mycelium.wallet.external.cashila.api.NullBodyAwareOkClient;
import com.mycelium.wallet.glidera.api.request.AddPhoneRequest;
import com.mycelium.wallet.glidera.api.request.BuyPriceRequest;
import com.mycelium.wallet.glidera.api.request.BuyRequest;
import com.mycelium.wallet.glidera.api.request.ConfirmPhoneRequest;
import com.mycelium.wallet.glidera.api.request.SellPriceRequest;
import com.mycelium.wallet.glidera.api.request.SellRequest;
import com.mycelium.wallet.glidera.api.request.SetPersonalInfoRequest;
import com.mycelium.wallet.glidera.api.request.UpdateEmailRequest;
import com.mycelium.wallet.glidera.api.request.VerifyPictureIdRequest;
import com.mycelium.wallet.glidera.api.response.BuyPriceResponse;
import com.mycelium.wallet.glidera.api.response.BuyResponse;
import com.mycelium.wallet.glidera.api.response.GetPersonalInfoResponse;
import com.mycelium.wallet.glidera.api.response.GetPhoneResponse;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.GlideraResponse;
import com.mycelium.wallet.glidera.api.response.OAuth1Response;
import com.mycelium.wallet.glidera.api.response.SellAddressResponse;
import com.mycelium.wallet.glidera.api.response.SellPriceResponse;
import com.mycelium.wallet.glidera.api.response.SellResponse;
import com.mycelium.wallet.glidera.api.response.SetPersonalInfoResponse;
import com.mycelium.wallet.glidera.api.response.StatusResponse;
import com.mycelium.wallet.glidera.api.response.TestResponse;
import com.mycelium.wallet.glidera.api.response.TransactionLimitsResponse;
import com.mycelium.wallet.glidera.api.response.TransactionResponse;
import com.mycelium.wallet.glidera.api.response.TransactionsResponse;
import com.mycelium.wallet.glidera.api.response.TwoFactorResponse;
import com.mycelium.wallet.glidera.api.response.VerifyPictureIdResponse;
import com.mycelium.wapi.api.WapiJsonModule;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import okio.Buffer;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.schedulers.Schedulers;

/***
 * Olp
 */
public class GlideraService {
    private final static String GLIDERA_SERVICE = "glideraService";
    /*
    Mainnet credentials
     */
    private final static String MAINNET_CLIENT_ID = "1234";
    private final static String MAINNET_URL = "https://www.glidera.io";

    /*
    Testnet Credentials
     */
    private final static String TESTNET_CLIENT_ID = "f9ccf1184cc574064eacd50e7ac6f8c8";
    private final static String TESTNET_URL = "https://sandbox.glidera.io";

    private final static String API_VERSION = "v1";
    private final static String HEADER_CLIENT_ID = "X-CLIENT-ID";
    private final static String HEADER_ACCESS_KEY = "X-ACCESS-KEY";
    private final static String HEADER_ACCESS_NONCE = "X-ACCESS-NONCE";
    private final static String HEADER_ACCESS_SIGNATURE = "X-ACCESS-SIGNATURE";

    private final String clientId;
    private final String baseUrl;
    private final GlideraApi glideraApi;
    private OAuth1Response _oAuth1Response;
    private volatile Observable<OAuth1Response> oAuth1ResponseObservable = null;
    private InMemoryPrivateKey bitidKey;
    private final Object nonceSync = new Object();
    private final NetworkParameters networkParameters;
    private volatile Long nonce;

    private GlideraService(@NonNull final NetworkParameters networkParameters) {
        Preconditions.checkNotNull(networkParameters);

        this.networkParameters = networkParameters;
        this.baseUrl = getBaseUrl(networkParameters);

        if (networkParameters.isTestnet()) {
            clientId = TESTNET_CLIENT_ID;
        } else {
            clientId = MAINNET_CLIENT_ID;
        }

        /**
         * The Sha256 HMAC hash of the message. Use the secret matching the access_key to hash the message.
         * The message is the concatenation of the X-ACCESS-NONCE + URI of the request + message body JSON.
         * The final X-ACCESS-SIGNATURE is the HmacSha256 of the UTF-8 encoding of the message as a Hex encoded string
         */
        final Interceptor apiCredentialInterceptor = new Interceptor() {
            @Override
            public Response intercept(Chain chain) throws IOException {
                Request request = chain.request();

                if (_oAuth1Response != null) {
                    Request.Builder requestBuilder = request.newBuilder();

                    synchronized (nonceSync) {
                        final String nonce = String.valueOf(getNonce());
                        final String uri = request.urlString();

                        String message = nonce + uri;

                        if (request.body() != null && request.body().contentLength() > 0) {
                            Buffer bodyBuffer = new Buffer();
                            request.body().writeTo(bodyBuffer);
                            byte[] bodyBytes = bodyBuffer.readByteArray();

                            String body = new String(bodyBytes, Charsets.UTF_8);
                            message += body;
                        }

                        final byte[] messageBytes = message.getBytes(Charsets.UTF_8);
                        final byte[] secretBytes = _oAuth1Response.getSecret().getBytes(Charsets.UTF_8);
                        final byte[] signatureBytes = Hmac.hmacSha256(secretBytes, messageBytes);

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        Hex.encode(signatureBytes, stream);
                        final String signature = stream.toString();

                        request = requestBuilder
                                .header(HEADER_ACCESS_KEY, _oAuth1Response.getAccess_key())
                                .header(HEADER_ACCESS_NONCE, nonce)
                                .header(HEADER_ACCESS_SIGNATURE, signature)
                                .build();

                    }
                }

                return chain.proceed(request);
            }
        };

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(15000, TimeUnit.MILLISECONDS);
        client.networkInterceptors().add(apiCredentialInterceptor);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        //objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.);
        objectMapper.registerModule(new WapiJsonModule());

        /*
        We should always add client_id to the header
         */
        RequestInterceptor requestInterceptor = new RequestInterceptor() {
            @Override
            public void intercept(RequestFacade requestFacade) {
                requestFacade.addHeader(HEADER_CLIENT_ID, clientId);
            }
        };

        /*
        Create the json adapter
         */
        RestAdapter adapter = new RestAdapter.Builder()
                .setEndpoint(baseUrl + "/api/" + API_VERSION + "/")
                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setConverter(new JacksonConverter(objectMapper))
                .setClient(new NullBodyAwareOkClient(client))
                .setRequestInterceptor(requestInterceptor)
                .build();

        glideraApi = adapter.create(GlideraApi.class);
    }

    /*
    Getters
     */

    public static GlideraService getInstance() {
        final MbwManager mbwManager = MbwManager.getInstance(null);
        try {
            return (GlideraService) mbwManager.getBackgroundObjectsCache().get(GLIDERA_SERVICE, new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    return new GlideraService(mbwManager.getNetwork());
                }
            });
        } catch (ExecutionException executionException) {
            throw new RuntimeException(executionException);
        }
    }

    private synchronized InMemoryPrivateKey getBitidKey() {
        if (bitidKey == null) {
            MbwManager manager = MbwManager.getInstance(null);
            bitidKey = manager.getBitIdKeyForWebsite(baseUrl + "/api/v1/authentication/bitid");
        }
        return bitidKey;
    }

    public GlideraApi getApi() {
        return glideraApi;
    }

    private synchronized long getNonce() {
        if( nonce == null ) {
            nonce = System.currentTimeMillis();
        }
        else {
            nonce++;
        }

        return nonce;
    }

    public static String getBaseUrl(NetworkParameters networkParameters) {
        if (networkParameters.isTestnet()) {
            return TESTNET_URL;
        } else {
            return MAINNET_URL;
        }
    }

    public static GlideraError convertRetrofitException(Throwable throwable) {
        if (throwable instanceof RetrofitError) {
            return (GlideraError) ((RetrofitError) throwable).getBodyAs(GlideraError.class);
        } else {
            return null;
        }
    }

    public String getBitidRegistrationUrl() {
        final String nonce = String.valueOf(getNonce());
        final String uri = baseUrl + "/bitid/auth";
        final String bitidUri = Uri.parse(uri + "?x=" + nonce).buildUpon().scheme("bitid").toString();
        final String signature = getBitidKey().signMessage(bitidUri).getBase64Signature();

        return Uri.parse(uri)
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("bitid_address", getBitidKey().getPublicKey().toAddress(networkParameters).toString())
                .appendQueryParameter("bitid_uri", bitidUri)
                .appendQueryParameter("bitid_signature", signature)
                        //.appendQueryParameter("redirect_uri", baseUrl + "/user/setup")
                        //.appendQueryParameter("redirect_uri", "https://www.google.com")
                        //.appendQueryParameter("redirect_uri", getSetupUrl())
                .appendQueryParameter("redirect_uri", "mycelium://glideraRegistration")
                .appendQueryParameter("state", nonce)
                .build()
                .toString();
    }

    public String getSetupUrl() {
        return getDeepLink("/user/setup");
    }

    private String getDeepLink(@NonNull String path) {
        Preconditions.checkArgument(path.startsWith("/"));

        final String nonce = String.valueOf(getNonce());

        Uri uri = Uri.parse(baseUrl + path)
                .buildUpon()
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("bitid_address", getBitidKey().getPublicKey().toAddress(networkParameters).toString())
                .appendQueryParameter("nonce", nonce)
                .build();

        final String signature = getBitidKey().signMessage(uri.toString()).getBase64Signature();

        return uri.buildUpon().appendQueryParameter("bitid_signature", signature).toString();
    }

    /*
    Api calls
     */
    public Observable<TestResponse> test() {
        return getApi().test();
    }

    /**
     * Once the user has successfully connected with BitID, the client can request OAuth 1 credentials to use for making futher API calls
     * . The credentials returned have all the permissions.
     *
     * @return OAuth1 Credentials
     */
    public synchronized Observable<OAuth1Response> oauth1Create() {
       if (oAuth1ResponseObservable == null) {
          final Observable<Address> addressObservable = Observable.create(new Observable.OnSubscribe<Address>() {
             @Override
             public void call(Subscriber<? super Address> subscriber) {
                subscriber.onNext(getBitidKey().getPublicKey().toAddress(networkParameters));
                subscriber.onCompleted();
             }
          });

          final String nonce = String.valueOf(getNonce());
          final String uri = baseUrl + "/api/" + API_VERSION + "/authentication/oauth1/create?x=" + nonce;
          final String signature = getBitidKey().signMessage(uri).getBase64Signature();


          oAuth1ResponseObservable =
                  addressObservable.flatMap(new Func1<Address, Observable<OAuth1Response>>() {
                     @Override
                     public Observable<OAuth1Response> call(Address address) {
                        return glideraApi.oAuth1Create(address.toString(), uri, signature);
                     }
                  })
                          .observeOn(Schedulers.newThread())
                          .doOnError(new Action1<Throwable>() {
                             @Override
                             public void call(Throwable throwable) {
                                _oAuth1Response = null;
                                oAuth1ResponseObservable = null;
                             }
                          })
                          .map(new Func1<OAuth1Response, OAuth1Response>() {
                             @Override
                             public OAuth1Response call(OAuth1Response oAuth1Response) {
                                _oAuth1Response = oAuth1Response;
                                return oAuth1Response;
                             }
                          })
                          .cache();
       }

       return oAuth1ResponseObservable;
    }

    /**
     * @return Return a user's email address.
     */
    public Observable<GlideraResponse> getEmail() {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().getEmail();
            }
        }.call();
    }

    /**
     * Update user's email address. An email with a verification link is sent to the new email address. Until the new email is verified
     * the user will continiue to use the previous email address.
     * <p/>
     * Note: This API call is only available to BitID/OAuth 1 clients.
     *
     * @param updateEmailRequest Request containing updated email information
     * @return Returns error details if present
     */
    public Observable<GlideraResponse> updateEmail(final UpdateEmailRequest updateEmailRequest, final String twoFACode) {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().updateEmail(updateEmailRequest, twoFACode);
            }
        }.call();
    }

    /**
     * Request the verification email to be resent so user can verify their email address. Returns an error if the email address is
     * already verified.
     *
     * @return Returns error details if present
     */
    public Observable<GlideraResponse> resendVerificationEmail() {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().resendVerificationEmail();
            }
        }.call();
    }

    /**
     * Personal information includes the user's name, address, and status. The
     * status explains if the user can transact and has a valid bank account setup.
     *
     * @return Return a user's personally identifiable information.
     */
    public Observable<GetPersonalInfoResponse> getPersonalInfo() {
        return new ApiCaller<GetPersonalInfoResponse>() {
            @Override
            Observable<GetPersonalInfoResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().getPersonalInfo();
            }
        }.call();
    }

    /**
     * Sets a user's personal info and initiates KYC identification.
     * <p/>
     * Note: The user's phone number must be set before basic info can be updated.
     *
     * @param setPersonalInfoRequest Request object containing updated user personal information.
     * @return It returns the user's status. The status explains if the user can transact and has a valid bank account setup.
     */
    public Observable<SetPersonalInfoResponse> setPersonalInfo(final SetPersonalInfoRequest setPersonalInfoRequest) {
        return new ApiCaller<SetPersonalInfoResponse>() {
            @Override
            Observable<SetPersonalInfoResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().setPersonalInfo(setPersonalInfoRequest);
            }
        }.call();
    }

    /**
     * Pass in the padded base64 of a government issued identity document (driver’s license, state ID, or passport) for verification.
     * <p/>
     * Note: The user's personal info must be set before basic info can be updated.
     *
     * @param verifyPictureIdRequest Request containing picture id data
     * @return Returns the document's status.
     */
    public Observable<VerifyPictureIdResponse> verifyPictureId(final VerifyPictureIdRequest verifyPictureIdRequest) {
        return new ApiCaller<VerifyPictureIdResponse>() {
            @Override
            Observable<VerifyPictureIdResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().verifyPictureId(verifyPictureIdRequest);
            }
        }.call();
    }

    /**
     * A user must successfully complete setup for each item in the response to be allowed to transact ( buy / sell).
     *
     * @return Returns a user's status.
     */
    public Observable<StatusResponse> status() {
        return new ApiCaller<StatusResponse>() {
            @Override
            Observable<StatusResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().status();
            }
        }.call();
    }

    /**
     * Regulations require vendors to limit the amount transacted based upon the risk of the
     * person performing the transaction. There are limits per transaction as well as daily and monthly limits. Limits increase as the
     * person passes more KYC (Know Your Customer) steps to better prove their identity. Transactions submitted in excess of the user’s
     * remaining limit will cause an error.
     *
     * @return Returns the user's buy and sell limits.
     */
    public Observable<TransactionLimitsResponse> transactionLimits() {
        return new ApiCaller<TransactionLimitsResponse>() {
            @Override
            Observable<TransactionLimitsResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().transactionLimits();
            }
        }.call();
    }

    /**
     * Adds a phone number for the user. A verification code is sent to the phone number which must be confirmed to complete this step.
     * After calling this function, the wallet should call confirm phone number. To change phone numbers, first delete the user's phone
     * number and then add the new number.
     * <p/>
     * Note: The user email must be confirmed before adding a phone number. An email with a confirmation link is sent after successfully
     * registering the user.
     *
     * @param addPhoneRequest Request containing updated phone information
     * @return Returns error details if present
     */
    public Observable<GlideraResponse> addPhone(final AddPhoneRequest addPhoneRequest) {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().addPhone(addPhoneRequest);
            }
        }.call();
    }

    /**
     * After adding a phone number, users must confirm access by providing the verification codes sent to the new number.
     *
     * @param confirmPhoneRequest Request containing phone confirmation information.
     * @return Returns error details if present
     */
    public Observable<GlideraResponse> confirmPhone(final ConfirmPhoneRequest confirmPhoneRequest) {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().confirmPhone(confirmPhoneRequest);
            }
        }.call();
    }

    /**
     * Deletes the user's phone number. User will not be able to transact or update any information until new phone number is added and
     * verified.
     *
     * @return Returns error details if present
     */
    public Observable<GlideraResponse> deletePhone(final String twoFACode) {
        return new ApiCaller<GlideraResponse>() {
            @Override
            Observable<GlideraResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().deletePhone(twoFACode);
            }
        }.call();
    }

    /**
     * Phone number will be blank if added but not confirmed.
     *
     * @return Returns the user's phone number.
     */
    public Observable<GetPhoneResponse> getPhone() {
        return new ApiCaller<GetPhoneResponse>() {
            @Override
            Observable<GetPhoneResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().getPhone();
            }
        }.call();
    }

    /**
     * Price quotes can be passed into the buy api call or be for informational purposes only.
     * Quotes expire after one minute. Quotes are in the currency of the user's country. Quotes will vary based upon the amount of
     * bitcoin or fiat specified for purchase.
     *
     * @param buyPriceRequest Request containing buy price information
     * @return Return the current buy price from Glidera.
     */
    public Observable<BuyPriceResponse> buyPrice(final BuyPriceRequest buyPriceRequest) {
        return new ApiCaller<BuyPriceResponse>() {
            @Override
            Observable<BuyPriceResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().buyPrice(buyPriceRequest);
            }
        }.call();
    }

    /**
     * Buy Bitcoin and send it to the destinationAddress. The fiat being spent on the purchase is electronically debited from the user's
     * verified bank account (by ACH, EFT, SEPA, etc). The market price can be used or a current Glidera price quote from a previous Buy
     * Prices service call can be used. This service requires a Two Factor Authentication code by previously calling Get Two Factor Code
     * service.
     *
     * @param buyRequest Request containing buy information.
     * @return Returns a buy response containing transaction details and estimated delivery date.
     */
    public Observable<BuyResponse> buy(final BuyRequest buyRequest, final String twoFACode) {
        return new ApiCaller<BuyResponse>() {
            @Override
            Observable<BuyResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().buy(buyRequest, twoFACode);
            }
        }.call();
    }

    /**
     * Send Bitcoin to this address when using the Sell service.
     *
     * @return Return a Glidera sell address.
     */
    public Observable<SellAddressResponse> sellAddress() {
        return new ApiCaller<SellAddressResponse>() {
            @Override
            Observable<SellAddressResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().sellAddress();
            }
        }.call();
    }

    /**
     * Price quotes can be passed in to the sell api call or be for informational purposes only.
     * Quotes expire after one minute. Quotes are in the currency of the user's country. Sell prices will vary based upon quantity.
     *
     * @param sellPriceRequest Request containing sell information.
     * @return Return a sell price quote from Glidera.
     */
    public Observable<SellPriceResponse> sellPrice(final SellPriceRequest sellPriceRequest) {
        return new ApiCaller<SellPriceResponse>() {
            @Override
            Observable<SellPriceResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().sellPrice(sellPriceRequest);
            }
        }.call();
    }

    /**
     * Sell Bitcoin by sending in a signed raw transaction. Glidera will broadcast successful transactions. One of the outputs of this
     * signed transaction must be a Glidera sell address. Sell addresses are created using the Create Sell Address service. The current
     * market price can be used or a Glidera price quote from the Sell Prices service can be used. If a failure occurs, Glidera will NOT
     * broadcast the transaction and the client can double spend the inputs if it desires.
     *
     * @param sellRequest Request containg sell information.
     * @return Returns sell object containing transaction information as well as estimated delivery date.
     */
    public Observable<SellResponse> sell(final SellRequest sellRequest) {
        return new ApiCaller<SellResponse>() {
            @Override
            Observable<SellResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().sell(sellRequest);
            }
        }.call();
    }

    /**
     * @return Return information about all previously performed Buy or Sell transaction.
     */
    public Observable<TransactionsResponse> transaction() {
        return new ApiCaller<TransactionsResponse>() {
            @Override
            Observable<TransactionsResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().transaction();
            }
        }.call();
    }

    /**
     * @param transactionUuid The uuid of the transaction to return. The transactionUUID in the URL matches the transactionUuid for the
     *                        transaction on the Glidera website. This UUID is also returned when using the Buy and Sell services.
     * @return Return information about previously performed Buy or Sell transaction.
     */
    public Observable<TransactionResponse> transaction(final UUID transactionUuid) {
        return new ApiCaller<TransactionResponse>() {
            @Override
            Observable<TransactionResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().transaction(transactionUuid);
            }
        }.call();
    }

    /**
     * There are a number of endpoints that require 2FA codes if two-factor authentication is enabled by the user. This includes buy,
     * sell, etc. If the user is configured to recieve SMS for their two factor verification this API call causes Glidera to send an SMS
     * message to the user's phone. The 2FA code must be passed in on the next service call in the X-2FA-CODE header. Use this service
     * right before calling a Two Factor Required API call.
     * <p/>
     * Some users may be configured to use an authenticator app (Authy or Google Authenticator), and an SMS message will NOT be sent. In
     * either case, the wallet application will need to prompt the user to enter a proper 2FA code to successfully pass the subsequent
     * service call.
     * <p/>
     * Users may also have enabled PIN based two-factor authentication. In this case the application must prompt the user for a PIN, and
     * no SMS meesage will be sent. This API call will return the appropriate mode for the user's two factor authentication.
     *
     * @return Returns type of two factor, as well as confirmation two factor was sent if appropriate.
     */
    public Observable<TwoFactorResponse> getTwoFactor() {
        return new ApiCaller<TwoFactorResponse>() {
            @Override
            Observable<TwoFactorResponse> apiCall(OAuth1Response oAuth1Response) {
                return getApi().getTwoFactor();
            }
        }.call();
    }

    /**
     * Perform api call, will first get fresh api credentials if they are needed.  If an error occures while performing call, current api
     * credentials will be cleared
     *
     * @param <T> The glidera response we intend to recieve
     */
    private abstract class ApiCaller<T> {
        private final Observable<OAuth1Response> apiCredentialResponseObservable;

        public ApiCaller() {
            apiCredentialResponseObservable = oauth1Create();
        }

        public Observable<T> call() {
            Observable<T> responseObservable;
            if (_oAuth1Response == null) {
                responseObservable = apiCredentialResponseObservable
                        .flatMap(new Func1<OAuth1Response, Observable<T>>() {
                            @Override
                            public Observable<T> call(OAuth1Response apiSecretToken) {
                                return apiCall(apiSecretToken).observeOn(Schedulers.io());
                            }
                        });
            } else {
                responseObservable = apiCall(_oAuth1Response)
                        .doOnError(new Action1<Throwable>() {
                            @Override
                            public void call(Throwable throwable) {
                                if (throwable instanceof RetrofitError) {
                                    GlideraError error = convertRetrofitException(throwable);
                                    if (error != null) {
                                        //Log.e("Glidera", error.toString());
                                        if (error.getCode() == 2016 || error.getCode() == 2017) {
                                            _oAuth1Response = null;
                                        }
                                    }
                                }
                            }
                        })
                        .retry(new Func2<Integer, Throwable, Boolean>() {
                            @Override
                            public Boolean call(Integer integer, Throwable throwable) {
                                /*
                                Retry up to three times
                                 */
                                if (integer > 2) {
                                    return false;
                                }

                                GlideraError error = convertRetrofitException(throwable);
                                if (error != null) {
                                    /*
                                    If nonce is too little, null the nonce and try again
                                     */
                                    if (error.getCode() == 2018) {
                                        nonce = null;
                                        return true;
                                    }
                                }


                                return false;
                            }
                        })
                        .map(new Func1<T, T>() {
                            @Override
                            public T call(T t) {
                                return t;
                            }
                        })
                        .observeOn(Schedulers.newThread());
            }

            return responseObservable
                    .map(new Func1<T, T>() {
                        @Override
                        public T call(T t) {
                            //Log.d("Glidera", t.toString());
                            return t;
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread());
        }

        abstract Observable<T> apiCall(OAuth1Response apiSecretToken);
    }

}
