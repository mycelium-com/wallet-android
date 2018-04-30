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

package com.mycelium.wallet.activity.send;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.*;
import com.mycelium.paymentrequest.PaymentRequestException;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
import com.mycelium.paymentrequest.PaymentRequestInformation;
import com.mycelium.paymentrequest.PkiVerificationData;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.otto.Subscribe;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.util.*;

public class VerifyPaymentRequestActivity extends AppCompatActivity {
   private static final String CALLBACK_URI = "payment_uri";
   private static final String RAW_PR = "raw_pr";
   private static final String PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId";
   @BindView(R.id.tvMerchant) TextView tvMerchant;
   @BindView(R.id.tvAmount) TextView tvAmount;
   @BindView(R.id.tvFiatAmount) TextView tvFiatAmount;
   @BindView(R.id.tvMessage) TextView tvMessage;
   @BindView(R.id.tvErrorDetails) TextView tvErrorDetails;
   @BindView(R.id.tvTimeExpires) TextView tvTimeExpires;
   @BindView(R.id.tvTimeCreated) TextView tvTimeCreated;
   @BindView(R.id.tvValid) TextView tvValid;
   @BindView(R.id.btAccept) Button btAccept;
   @BindView(R.id.btDismiss) Button btDismiss;
   @BindView(R.id.etMerchantMemo) EditText etMerchantMemo;
   @BindView(R.id.llAmount) LinearLayout llAmount;
   @BindView(R.id.llMessage) LinearLayout llMessage;
   @BindView(R.id.llTime) LinearLayout llTime;
   @BindView(R.id.llTimeExpires) LinearLayout llTimeExpires;
   @BindView(R.id.llMessageToMerchant) LinearLayout llMessageToMerchant;
   @BindView(R.id.llErrorDetailsDisplay) LinearLayout llErrorDetailsDisplay;
   @BindView(R.id.ivSignatureWarning) ImageView ivSignatureWarning;

   private ProgressDialog progress;
   private PaymentRequestHandler requestHandler;
   private Throwable requestException;
   private MbwManager mbw;
   private PaymentRequestInformation requestInformation;
   private String paymentRequestHandlerUuid;
   private Handler checkExpired;
   private Runnable expiredUpdater;


   public static Intent getIntent(Activity currentActivity, BitcoinUri uri) {
      return new Intent(currentActivity, VerifyPaymentRequestActivity.class)
              .putExtra(CALLBACK_URI, uri);
   }

   public static Intent getIntent(Activity currentActivity, byte[] rawPaymentRequest) {
      return new Intent(currentActivity, VerifyPaymentRequestActivity.class)
              .putExtra(RAW_PR, rawPaymentRequest);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.verify_payment_request_activity);
      ButterKnife.bind(this);
      mbw = MbwManager.getInstance(this);
      mbw.getEventBus().register(this);

      // only popup the keyboard if the user taps the textbox
      getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      BitcoinUri bitcoinUri = (BitcoinUri) getIntent().getSerializableExtra(CALLBACK_URI);
      byte[] rawPaymentRequest = (byte[]) getIntent().getSerializableExtra(RAW_PR);

      // either one of them must be set...
      Preconditions.checkArgument(
            (bitcoinUri !=null && !Strings.isNullOrEmpty(bitcoinUri.callbackURL))
            || rawPaymentRequest !=null
      );

      btAccept.setEnabled(false);

      if (savedInstanceState != null) {
         paymentRequestHandlerUuid = savedInstanceState.getString(PAYMENT_REQUEST_HANDLER_ID);
         if (paymentRequestHandlerUuid != null) {
            requestHandler = (PaymentRequestHandler) mbw.getBackgroundObjectsCache()
                  .getIfPresent(paymentRequestHandlerUuid);
         }
      }
      String progressMsg = getString(R.string.payment_request_fetching_payment_request);

      if (requestHandler == null) {
         paymentRequestHandlerUuid = UUID.randomUUID().toString();

         // check if we are currently in TOR-only mode - if so, setup the PaymentRequestHandler
         // that all http(s) calls get routed over TOR
         if (mbw.getTorMode() == ServerEndpointType.Types.ONLY_TOR && mbw.getTorManager() != null){
            requestHandler = new PaymentRequestHandler(mbw.getEventBus(), mbw.getNetwork()){
               @Override
               protected OkHttpClient getHttpClient() {
                  OkHttpClient client = super.getHttpClient();
                  return mbw.getTorManager().setupClient(client);
               }
            };
            progressMsg += getString(R.string.payment_request_over_tor);

         } else {
            requestHandler = new PaymentRequestHandler(mbw.getEventBus(), mbw.getNetwork());
         }
         mbw.getBackgroundObjectsCache().put(paymentRequestHandlerUuid, requestHandler);
      }

      progress = ProgressDialog.show(this, "", progressMsg, true);

      if (rawPaymentRequest != null) {
         requestHandler.parseRawPaymentRequest(rawPaymentRequest);
      } else {
         requestHandler.fetchPaymentRequest(bitcoinUri);
      }

      checkExpired = new Handler();
      expiredUpdater = new Runnable() {
         @Override
         public void run() {
            updateExpireWarning();
            checkExpired.postDelayed(this, 1000);
         }
      };
   }

   @OnTouch(R.id.etMerchantMemo)
   public boolean scrollIntoView() {
      etMerchantMemo.requestLayout();
      VerifyPaymentRequestActivity.this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
      return false;
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putString(PAYMENT_REQUEST_HANDLER_ID, paymentRequestHandlerUuid);
   }

   @Override
   public void onResume() {
      mbw.getEventBus().register(this);
      expiredUpdater.run();
      super.onResume();
   }

   @Override
   public void onPause() {
      progress.dismiss();
      mbw.getEventBus().unregister(this);
      checkExpired.removeCallbacks(expiredUpdater);
      super.onPause();
   }

   @OnClick(R.id.btAccept)
   void onAcceptClick() {
      Intent result = new Intent();
      requestHandler.setMerchantMemo(etMerchantMemo.getText().toString());
      mbw.getBackgroundObjectsCache().put(paymentRequestHandlerUuid, requestHandler);
      result.putExtra("REQUEST_PAYMENT_HANDLER_ID", paymentRequestHandlerUuid);
      setResult(RESULT_OK, result);
      finish();
   }

   @OnClick(R.id.btDismiss)
   void onDismissClick() {
      setResult(RESULT_CANCELED);
      finish();
   }

   @OnClick(R.id.ivSignatureWarning)
   void onSignatureWarningClick(){
      Utils.showSimpleMessageDialog(this, getString(R.string.payment_request_warning_no_sig));
   }

   private void updateUi() {
      if (requestException == null && requestInformation == null){
         // no payment request (or error) available
         return;
      }

      if (requestException != null) {
         tvMerchant.setText(requestException.getMessage());
         tvValid.setText(getString(R.string.payment_request_invalid_signature));
         btAccept.setEnabled(false);
         llAmount.setVisibility(View.GONE);
         llTime.setVisibility(View.GONE);
         llTimeExpires.setVisibility(View.GONE);
         llMessageToMerchant.setVisibility(View.GONE);
         llMessage.setVisibility(View.GONE);
         llErrorDetailsDisplay.setVisibility(View.VISIBLE);
         final String message;
         final Throwable cause = requestException.getCause();
         if (cause != null){
            message = ", " + cause.getLocalizedMessage();
         } else {
            message = requestException.getLocalizedMessage();
         }
         tvErrorDetails.setText(message);
      } else {
         llErrorDetailsDisplay.setVisibility(View.GONE);

         if (requestInformation.hasValidSignature()) {
            tvValid.setText(getString(R.string.payment_request_signature_okay));
            PkiVerificationData pkiVerificationData = requestInformation.getPkiVerificationData();
            tvMerchant.setText(pkiVerificationData.displayName);
            ivSignatureWarning.setVisibility(View.GONE);
         } else {
            tvValid.setText(getString(R.string.payment_request_unsigned_request));
            tvMerchant.setText(getString(R.string.payment_request_unable_to_verify));
            ivSignatureWarning.setVisibility(View.VISIBLE);
         }

         if (!requestInformation.isExpired()) {
            btAccept.setEnabled(true);
         }

         if (requestInformation.hasAmount()) {
            long totalAmount = requestInformation.getOutputs().getTotalAmount();
            tvAmount.setText(mbw.getBtcValueString(totalAmount));
            CurrencySwitcher currencySwitcher = mbw.getCurrencySwitcher();
            if (currencySwitcher.isFiatExchangeRateAvailable()){
               tvFiatAmount.setVisibility(View.VISIBLE);
               tvFiatAmount.setText(
                     String.format("(~%s)",
                     currencySwitcher.getFormattedFiatValue(ExactBitcoinValue.from(totalAmount), true))
               );
            } else {
               tvFiatAmount.setVisibility(View.GONE);
            }
         } else {
            tvAmount.setText(getString(R.string.payment_request_no_amount_specified));
            tvFiatAmount.setVisibility(View.GONE);
         }
         tvMessage.setText(requestInformation.getPaymentDetails().memo);

         if (!requestInformation.hasPaymentCallbackUrl()){
            llMessageToMerchant.setVisibility(View.GONE);
         }

         if (requestInformation.getPaymentDetails().time != null) {
            tvTimeCreated.setText(Utils.getFormattedDate(this, new Date(requestInformation.getPaymentDetails().time * 1000L)));
         } else {
            tvTimeCreated.setText(getString(R.string.data_not_available_short));
         }

         updateExpireWarning();
      }
   }

   private void updateExpireWarning() {
      if (requestInformation != null) {

         if (requestInformation.getPaymentDetails().expires != null) {
            PrettyTime prettyTime = new PrettyTime(mbw.getLocale());
            Date date = new Date(requestInformation.getPaymentDetails().expires * 1000L);
            prettyTime.removeUnit(JustNow.class);
            prettyTime.removeUnit(Millisecond.class);
            String duration = prettyTime.format(date);
            tvTimeExpires.setText(String.format("%s\n(%s)", Utils.getFormattedDate(this, date), duration));
         } else {
            tvTimeExpires.setText(getString(R.string.data_not_available_short));
         }

         // show a red warning, if it is expired
         if (requestInformation.isExpired()) {
            tvTimeExpires.setTextColor(getResources().getColor(R.color.status_red));
            btAccept.setEnabled(false);
         } else {
            // reset color
            tvTimeExpires.setTextColor(tvTimeExpires.getTextColors().getDefaultColor());
         }
      }
   }

   @Subscribe
   public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
      updateUi();
   }

   @Subscribe
   public void onPaymentRequestFetched(PaymentRequestInformation paymentRequestInformation) {
      progress.dismiss();
      requestInformation = paymentRequestInformation;
      requestException = null;
      updateUi();
   }

   @Subscribe
   public void onPaymentRequestException(PaymentRequestException paymentRequestException) {
      progress.dismiss();
      requestInformation = null;
      requestException = paymentRequestException;
      updateUi();
   }
}
