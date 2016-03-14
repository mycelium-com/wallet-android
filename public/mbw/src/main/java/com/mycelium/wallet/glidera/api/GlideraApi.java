package com.mycelium.wallet.glidera.api;

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

import java.util.List;
import java.util.UUID;

import retrofit.http.Body;
import retrofit.http.DELETE;
import retrofit.http.GET;
import retrofit.http.Header;
import retrofit.http.POST;
import retrofit.http.Path;
import rx.Observable;


public interface GlideraApi {
    /*
    Test
     */
    @GET("/test/uri")
    Observable<TestResponse> test();

    /*
    BitID
     */
    @POST("/authentication/oauth1/create ")
    Observable<OAuth1Response> oAuth1Create(@Header("X-BITID-ADDRESS") String xBitidAddress,
                                            @Header("X-BITID-URI") String xBitidUri,
                                            @Header("X-BITID-SIGNATURE") String xBitidSignature);

    /*
    User
     */
    @GET("/user/email")
    Observable<GlideraResponse> getEmail();

    @POST("/user/email")
    Observable<GlideraResponse> updateEmail(@Body UpdateEmailRequest updateEmailRequest, @Header("X-2FA-Code") String twoFACode);

    @POST("/user/email/resend_verification")
    Observable<GlideraResponse> resendVerificationEmail();

    @GET("/user/personalinfo")
    Observable<GetPersonalInfoResponse> getPersonalInfo();

    @POST("/user/personalinfo")
    Observable<SetPersonalInfoResponse> setPersonalInfo(@Body SetPersonalInfoRequest setPersonalInfoRequest);

    @POST("/user/idverify")
    Observable<VerifyPictureIdResponse> verifyPictureId(@Body VerifyPictureIdRequest verifyPictureIdRequest);

    @GET("/user/status")
    Observable<StatusResponse> status();

    @GET("/user/limits")
    Observable<TransactionLimitsResponse> transactionLimits();

    /*
    Phone
     */

    @POST("/user/phone")
    Observable<GlideraResponse> addPhone(@Body AddPhoneRequest addPhoneRequest);

    @POST("/user/phone/confirm")
    Observable<GlideraResponse> confirmPhone(@Body ConfirmPhoneRequest confirmPhoneRequest);

    @DELETE("/user/phone")
    Observable<GlideraResponse> deletePhone(@Header("X-2FA-Code") String twoFACode);

    @GET("/user/phone")
    Observable<GetPhoneResponse> getPhone();

    /*
    Transact
     */
    @POST("/prices/buy")
    Observable<BuyPriceResponse> buyPrice(@Body BuyPriceRequest buyPriceRequest);

    @POST("/buy")
    Observable<BuyResponse> buy(@Body BuyRequest buyRequest, @Header("X-2FA-Code") String twoFACode);

    @GET("/user/create_sell_address")
    Observable<SellAddressResponse> sellAddress();

    @POST("/prices/sell")
    Observable<SellPriceResponse> sellPrice(@Body SellPriceRequest sellPriceRequest);

    @POST("/sell")
    Observable<SellResponse> sell(@Body SellRequest sellRequest);

    @GET("/transaction")
    Observable<TransactionsResponse> transaction();

    @GET("/transaction/{transactionUuid}")
    Observable<TransactionResponse> transaction(@Path("transactionUuid") UUID transactionUuid);

    /*
    Two Factor
     */
    @GET("/authentication/get2faCode")
    Observable<TwoFactorResponse> getTwoFactor();

}
