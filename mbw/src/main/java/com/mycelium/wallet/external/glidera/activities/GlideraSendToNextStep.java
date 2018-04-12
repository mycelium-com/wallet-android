package com.mycelium.wallet.external.glidera.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.BuySellSelectActivity;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.response.StatusResponse;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class GlideraSendToNextStep extends Activity {
   private GlideraService glideraService;

   @Override
   protected void onResume() {
      super.onResume();

      String uriString = getIntent().getStringExtra("uri");

      Uri uri = Uri.parse(uriString);
      final String status;
      if (uri.isHierarchical()){
         status = uri.getQueryParameter("status");
      } else {
         // prevent UnsupportedOperationException
         status = null;
      }

      glideraService = GlideraService.getInstance();

      glideraService.status()
              .observeOn(AndroidSchedulers.mainThread())
              .subscribe(new Observer<StatusResponse>() {
                 @Override
                 public void onCompleted() {
                 }

                 @Override
                 public void onError(Throwable e) {
                    handleError();
                 }

                 @Override
                 public void onNext(StatusResponse statusResponse) {
                    if (statusResponse.isUserCanTransact()) {
                       //Send to buy
                       Intent intent = new Intent(GlideraSendToNextStep.this, GlideraMainActivity.class);
                       startActivity(intent);
                       finish();
                    } else {
                       if (status != null && status.equals("SUCCESS")) {
                          //Send to setup
                          String uri = glideraService.getSetupUrl();
                          Utils.openWebsite(GlideraSendToNextStep.this, uri);
                       } else if (status != null && status.equals("RETURN")) {
                          Intent intent = new Intent(GlideraSendToNextStep.this, BuySellSelectActivity.class);
                          startActivity(intent);
                          finish();
                       } else {
                          handleError();
                       }
                    }
                 }
              });
   }

   private void handleError() {
      Intent intent = new Intent(this, BuySellSelectActivity.class);
      startActivity(intent);
      finish();
   }
}
