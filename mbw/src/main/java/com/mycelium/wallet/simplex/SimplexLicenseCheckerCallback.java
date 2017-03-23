package com.mycelium.wallet.simplex;

import android.os.Handler;
import android.util.Log;

import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.squareup.otto.Bus;

/**
 * Created by tomb on 11/23/16.
 */

public class SimplexLicenseCheckerCallback implements LicenseCheckerCallback {
   private Handler activityHandler;
   private Bus eventBus;

   public SimplexLicenseCheckerCallback(Handler activityHandler, Bus eventBus) {
      this.activityHandler = activityHandler;
      this.eventBus = eventBus;
   }

   public void allow(int reason) {
      // remove logic - there is no in app validation
   }

   public void dontAllow(int policyReason) {
      // remove logic - there is no in app validation
   }

   public void applicationError(int errorCode) {
      // remove logic - there is no in app validation
   }

   @Override
   public void licensingResponse(int responseCode, String signedData, String signature) {
      Log.d("simplex", "got licensingResponse");
      AuthEvent eventData = new AuthEvent();
      eventData.isSuccess = true;
      eventData.activityHandler = activityHandler;
      eventData.responseData = eventData.new LvlResponse();
      eventData.responseData.responseCode = responseCode;
      eventData.responseData.signedData = signedData;
      eventData.responseData.signature = signature;
      eventBus.post(eventData);
   }
}
