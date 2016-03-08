package com.mycelium.wallet.glidera.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.response.GlideraError;
import com.mycelium.wallet.glidera.api.response.StatusResponse;
import com.mycelium.wallet.lt.LocalTraderManager;

import rx.Observer;
import rx.android.schedulers.AndroidSchedulers;

public class BuySellSelect extends FragmentActivity {
    private MbwManager _mbwManager;
    private LocalTraderManager _ltManager;
    private GlideraService glideraService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glidera_buy_sell);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        this.glideraService = GlideraService.getInstance();

        final LinearLayout glideraRow = (LinearLayout) findViewById(R.id.glideraBuySell);

        glideraRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                glideraService.status()
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(new Observer<StatusResponse>() {
                            @Override
                            public void onCompleted() {
                            }

                            @Override
                            public void onError(Throwable e) {
                                GlideraError error = GlideraService.convertRetrofitException(e);
                                if (error != null && error.getCode() != null) {
                                    Log.i("Glidera", error.toString());
                                    if (error.getCode() == 1103) {
                                        //Invalid credentials, send to bitid registration
                                        String uri = glideraService.getBitidRegistrationUrl();
                                        Log.i("Glidera", "No account for that bitid");
                                        Log.i("Glidera", "redirect to " + uri);
                                        Utils.openWebsite(BuySellSelect.this, uri);
                                    } else {
                                        Log.i("Glidera", "unaccounted error");
                                    }
                                } else {
                                    //throw new RuntimeException(e);

                                }
                            }

                            @Override
                            public void onNext(StatusResponse statusResponse) {
                                if (statusResponse.isUserCanTransact()) {
                                    Log.i("Glidera", "BuySellSelect: Bitid account found, setup complete");
                                    Intent intent = new Intent(BuySellSelect.this, GlideraMainActivity.class);
                                    startActivity(intent);
                                    finish();
                                    return;
                                } else {
                                    //Send to setup
                                    String uri = glideraService.getSetupUrl();
                                    Log.i("Glidera", "Bitid account found, setup incomplete");
                                    Log.i("Glidera", "redirect to " + uri);
                                    Utils.openWebsite(BuySellSelect.this, uri);
                                }
                            }
                        });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                Intent intent = new Intent(this, ModernMain.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
                return true;

        }
        return super.onOptionsItemSelected(item);
    }
}
