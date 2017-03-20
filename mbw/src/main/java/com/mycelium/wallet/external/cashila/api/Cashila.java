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

import com.mycelium.wallet.external.cashila.api.request.*;
import com.mycelium.wallet.external.cashila.api.response.*;
import retrofit.http.*;
import rx.Observable;

import java.util.List;
import java.util.UUID;

public interface Cashila {

   @PUT("/account")
   Observable<CashilaResponse<CashilaAccountResponse>> createAccount(@Body CashilaAccountRequest newAccount);

   @PUT("/account")
   Observable<CashilaResponse<CashilaAccountResponse>> loginExistingAccount(@Body CashilaAccountLoginRequest newAccount);

   @POST("/bitid/request-token")
   Observable<CashilaResponse<BitIdRequestToken>> getRequestToken();

   @POST("/request-signup?bitid=1")
   Observable<CashilaResponse<SignUpRequestToken>> getSignUpRequestToken();

   @GET("/supported-countries")
   Observable<CashilaResponse<SupportedCountries>> getSupportedCountries();

   @GET("/terms-of-use")
   @Headers({"Accept:text/plain"})
   Observable<String> getTermsOfUse();

   @GET("/account/limits")
   Observable<CashilaResponse<AccountLimits>> getAccountLimits();

   @GET("/billpays/recent")
   Observable<CashilaResponse<List<BillPayExistingRecipient>>> getBillPaysRecent();

   @PUT("/recipients/{id}")
   Observable<CashilaResponse<BillPayExistingRecipient>> saveRecipient(
         @Path("id") UUID recipientId, @Body SaveRecipient saveRecipient);

   @PUT("/billpays/create/{newPaymentId}")
   Observable<CashilaResponse<BillPay>> createBillPay(
         @Path("newPaymentId") UUID newPaymentId, @Body CreateBillPay createBillPayRequest);

   @POST("/billpays/{paymentId}/revive")
   Observable<CashilaResponse<BillPay>> reviveBillPay(@Path("paymentId") UUID paymentId);

   @GET("/billpays/{paymentId}")
   Observable<CashilaResponse<BillPay>> getBillPay(@Path("paymentId") UUID paymentId);

   @DELETE("/billpays/{paymentId}")
   Observable<CashilaResponse<List<Void>>> deleteBillPay(@Path("paymentId") UUID paymentId);

   @GET("/billpays")
   Observable<CashilaResponse<List<BillPay>>> getBillPays(
         @Query("status") String statusFilter,
         @Query("id") String idList, @Query("exclude_status") String excludeStatus);

   @GET("/billpays")
   Observable<CashilaResponse<List<BillPay>>> getBillPays(
         @Query("status") String statusFilter); //, @Query("exclude_status") String excludeStatus);

   @GET("/billpays")
   Observable<CashilaResponse<List<BillPay>>> getBillPays();

   @POST("/account/deep-link")
   Observable<CashilaResponse<DeepLink>> getDeepLink(@Body() GetDeepLink getDeepLink);
}
