package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;

import com.mycelium.wallet.R;


public class BankPaymentRequestActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bank_payment_request);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, new HowBankPayFragment())
                .commitAllowingStateLoss();
    }
}
