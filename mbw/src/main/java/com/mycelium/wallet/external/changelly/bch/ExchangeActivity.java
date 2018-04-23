package com.mycelium.wallet.external.changelly.bch;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.view.ValueKeyboard;

public class ExchangeActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exchange);
        setTitle(getString(R.string.excange_title));
        ActionBar bar = getSupportActionBar();
        bar.setDisplayShowHomeEnabled(true);
        bar.setIcon(R.drawable.action_bar_logo);

        getWindow().setBackgroundDrawableResource(R.drawable.background_witherrors_centered);

        if (getFragmentManager().findFragmentById(R.id.fragment_container) == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, new ExchangeFragment(), "ExchangeFragment")
                    .addToBackStack("ExchangeFragment")
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onBackPressed() {
        ValueKeyboard valueKeyboard = findViewById(R.id.numeric_keyboard);
        if (valueKeyboard != null && valueKeyboard.getVisibility() == View.VISIBLE) {
            valueKeyboard.done();
        } else if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }
}
