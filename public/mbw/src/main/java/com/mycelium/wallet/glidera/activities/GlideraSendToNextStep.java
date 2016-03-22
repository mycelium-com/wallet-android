package com.mycelium.wallet.glidera.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.mycelium.wallet.Utils;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.response.StatusResponse;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class GlideraSendToNextStep extends Activity {
    private GlideraService glideraService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String uriString = getIntent().getStringExtra("uri");

        Uri uri = Uri.parse(uriString);

        if (uri.getQueryParameter("status").equals("SUCCESS")) {
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
                                //Send to setup
                                String uri = glideraService.getSetupUrl();
                                Utils.openWebsite(GlideraSendToNextStep.this, uri);
                            }
                        }
                    });
        } else {
            handleError();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

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
                            //Send to buy/sell select
                            Intent intent = new Intent(GlideraSendToNextStep.this, BuySellSelect.class);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    private void handleError() {
        Intent intent = new Intent(GlideraSendToNextStep.this, BuySellSelect.class);
        startActivity(intent);
        finish();
    }
}
