package com.mycelium.wallet.activity.exchange;


import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;

import com.mycelium.wallet.R;

public class ExchangeActivity extends Activity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange);
        setTitle("Excange BCH to BTC");
        getFragmentManager().beginTransaction()
                .add(R.id.fragment_container, new ExchangeFragment(), "ExchangeFragment")
                .commitAllowingStateLoss();
    }
}
