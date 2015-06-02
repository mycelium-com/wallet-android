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
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnTouch;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.mycelium.wallet.BitcoinUri;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.paymentrequest.PaymentRequestException;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
import com.mycelium.wallet.paymentrequest.PaymentRequestInformation;
import com.mycelium.wallet.paymentrequest.PkiVerificationData;
import com.squareup.otto.Subscribe;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.units.JustNow;
import org.ocpsoft.prettytime.units.Millisecond;

import java.text.DateFormat;
import java.util.*;

public class VerifyPaymentRequestActivity extends ActionBarActivity {

   private static final String CALLBACK_URI = "payment_uri";
   private static final String RAW_PR = "raw_pr";
   private static final String PAYMENT_REQUEST_HANDLER_ID = "paymentRequestHandlerId";
   @InjectView(R.id.tvMerchant) TextView tvMerchant;
   @InjectView(R.id.tvAmount) TextView tvAmount;
   @InjectView(R.id.tvMessage) TextView tvMessage;
   @InjectView(R.id.tvTimeExpires) TextView tvTimeExpires;
   @InjectView(R.id.tvTimeCreated) TextView tvTimeCreated;
   @InjectView(R.id.tvValid) TextView tvValid;
   @InjectView(R.id.btAccept) Button btAccept;
   @InjectView(R.id.btDismiss) Button btDismiss;
   @InjectView(R.id.etMerchantMemo) EditText etMerchantMemo;
   @InjectView(R.id.llAmount) LinearLayout llAmount;
   @InjectView(R.id.llMessage) LinearLayout llMessage;
   @InjectView(R.id.llTime) LinearLayout llTime;
   @InjectView(R.id.llTimeExpires) LinearLayout llTimeExpires;
   @InjectView(R.id.llMessageToMerchant) LinearLayout llMessageToMerchant;
   @InjectView(R.id.ivSignatureWarning) ImageView ivSignatureWarning;

   private BitcoinUri bitcoinUri;
   private ProgressDialog progress;
   private PaymentRequestHandler requestHandler;
   private Throwable requestException;
   private MbwManager mbw;
   private PaymentRequestInformation requestInformation;
   private String paymentRequestHandlerUuid;
   private Handler checkExpired;
   private Runnable expiredUpdater;
   private byte[] rawPaymentRequest;


   public static Intent getIntent(Activity currentActivity, BitcoinUri uri) {
      Intent intent = new Intent(currentActivity, VerifyPaymentRequestActivity.class);
      intent.putExtra(CALLBACK_URI, uri);
      return intent;
   }

   public static Intent getIntent(Activity currentActivity, byte[] rawPaymentRequest) {
      Intent intent = new Intent(currentActivity, VerifyPaymentRequestActivity.class);
      intent.putExtra(RAW_PR, rawPaymentRequest);
      return intent;
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.verify_payment_request_activity);
      ButterKnife.inject(this);
      mbw = MbwManager.getInstance(this);
      mbw.getEventBus().register(this);

      // only popup the keyboard if the user taps the textbox
      this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

      bitcoinUri = (BitcoinUri) getIntent().getSerializableExtra(CALLBACK_URI);
      rawPaymentRequest = (byte[]) getIntent().getSerializableExtra(RAW_PR);

      // either one of them must be set...
      Preconditions.checkArgument(
            (bitcoinUri!=null && !Strings.isNullOrEmpty(bitcoinUri.callbackURL))
            || rawPaymentRequest!=null
      );


      progress = ProgressDialog.show(this, "", getString(R.string.payment_request_fetching_payment_request), true);
      btAccept.setEnabled(false);

      if (savedInstanceState != null) {
         paymentRequestHandlerUuid = savedInstanceState.getString(PAYMENT_REQUEST_HANDLER_ID);
         if (paymentRequestHandlerUuid != null) {
            requestHandler = (PaymentRequestHandler) mbw.getBackgroundObjectsCache()
                  .getIfPresent(paymentRequestHandlerUuid);
         }
      }
      if (requestHandler == null) {
         paymentRequestHandlerUuid = UUID.randomUUID().toString();
         requestHandler = new PaymentRequestHandler(mbw.getEventBus(), mbw.getNetwork());
         mbw.getBackgroundObjectsCache().put(paymentRequestHandlerUuid, requestHandler);
      }

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
      if (requestException != null) {
         tvMerchant.setText(requestException.getMessage());
         tvValid.setText(getString(R.string.payment_request_invalid_signature));
         btAccept.setEnabled(false);
         llAmount.setVisibility(View.GONE);
         llTime.setVisibility(View.GONE);
         llTimeExpires.setVisibility(View.GONE);
         llMessageToMerchant.setVisibility(View.GONE);
         llMessage.setVisibility(View.GONE);
      } else {
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
         } else {
            tvAmount.setText(getString(R.string.payment_request_no_amount_specified));
         }
         tvMessage.setText(requestInformation.getPaymentDetails().memo);

         if (!requestInformation.hasPaymentCallbackUrl()){
            llMessageToMerchant.setVisibility(View.GONE);
         }

         if (requestInformation.getPaymentDetails().time != null) {
            tvTimeCreated.setText(getFormattedDate(new Date(requestInformation.getPaymentDetails().time * 1000L)));
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
            tvTimeExpires.setText(String.format("%s\n(%s)", getFormattedDate(date), duration));
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

   private String getFormattedDate(Date date) {
      Locale locale = getResources().getConfiguration().locale;
      DateFormat format;
      Calendar calExpires = Calendar.getInstance(locale);
      calExpires.setTime(date);
      // show the date part if expire is not today
      if (calExpires.get(Calendar.DAY_OF_YEAR) != Calendar.getInstance(locale).get(Calendar.DAY_OF_YEAR)) {
         format = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM, locale);
      } else {
         format = DateFormat.getTimeInstance(DateFormat.MEDIUM, locale);
      }
      return format.format(date);
   }

}
