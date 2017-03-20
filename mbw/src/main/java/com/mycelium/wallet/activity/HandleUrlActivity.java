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

package com.mycelium.wallet.activity;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.Toast;
import com.google.common.base.Preconditions;
import com.mycelium.paymentrequest.PaymentRequestException;
import com.mycelium.paymentrequest.PaymentRequestInformation;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.SendMainActivity;
import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;

public class HandleUrlActivity extends Activity {
   private static final String URI = "uri";
   private Uri uri;


   public static Intent getIntent(Context currentActivity, Uri uri) {
      Intent intent = new Intent(currentActivity, HandleUrlActivity.class);
      intent.putExtra(URI, uri);
      return intent;
   }

   @Override
   public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.wait_activity);
      Intent intent = getIntent();
      uri = Preconditions.checkNotNull((Uri) intent.getParcelableExtra(URI));
   }

   @Override
   protected void onStart() {
      super.onStart();
      new AsyncTask<Uri, Void, PaymentRequestAsyncTaskResult>() {
         @Override
         protected PaymentRequestAsyncTaskResult doInBackground(Uri... params) {
            MbwManager mbw = MbwManager.getInstance(HandleUrlActivity.this);
            PaymentRequestHandler paymentRequestHandler =
                  new PaymentRequestHandler(mbw.getEventBus(), mbw.getNetwork());

            try {
               PaymentRequestInformation paymentRequestInformation = paymentRequestHandler.fromCallback(uri.toString());
               return new PaymentRequestAsyncTaskResult(paymentRequestInformation, uri);
            } catch (PaymentRequestException ex){
               // not a valid payment request
               return new PaymentRequestAsyncTaskResult(null, uri);
            }
         }

         @Override
         protected void onPostExecute(PaymentRequestAsyncTaskResult paymentRequestAsyncTaskResult) {
            super.onPostExecute(paymentRequestAsyncTaskResult);
            if (paymentRequestAsyncTaskResult.paymentRequest != null){
               // handle the payment request
               MbwManager mbw = MbwManager.getInstance(HandleUrlActivity.this);

               Intent intent = SendMainActivity.getIntent(
                     HandleUrlActivity.this,
                     mbw.getSelectedAccount().getId(),
                     paymentRequestAsyncTaskResult.paymentRequest.getRawPaymentRequest(),
                     false);

               HandleUrlActivity.this.startActivity(intent);
            } else {
               // if its not a payment request, open the url in the browser...
               Intent browserIntent = new Intent(Intent.ACTION_VIEW, uri);
               if (browserIntent.resolveActivity(HandleUrlActivity.this.getPackageManager()) != null) {
                  Toast.makeText(HandleUrlActivity.this, R.string.opening_url_in_browser, Toast.LENGTH_LONG).show();
                  HandleUrlActivity.this.startActivity(browserIntent);
               } else {
                  Toast.makeText(HandleUrlActivity.this, R.string.error_no_browser, Toast.LENGTH_LONG).show();
               }
            }
            HandleUrlActivity.this.finish();
         }
      }.execute(uri);
   }

   private static class PaymentRequestAsyncTaskResult{
      public PaymentRequestInformation paymentRequest;
      public Uri uri;

      public PaymentRequestAsyncTaskResult(PaymentRequestInformation rawPaymentrequest, Uri uri) {
         this.paymentRequest = rawPaymentrequest;
         this.uri = uri;
      }
   }
}
