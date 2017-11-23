package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


import com.mycelium.wallet.R;

public class ChangellyOfferActivity extends Activity {

    public static final String FROM_AMOUNT = "fromAmount";

    private static String TAG = "ChangellyOfferActivity";

    private TextView tvFromAmount, tvFromCurr, tvToAmount, tvToCurr, tvRateValue, tvCreatedAt, tvSendToAddress;
    private Button btCopyToClipboard;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);

        tvFromAmount = (TextView) findViewById(R.id.tvFromAmount);
        tvFromCurr = (TextView) findViewById(R.id.tvFromCurr);
        tvToAmount = (TextView) findViewById(R.id.tvToAmount);
        tvToCurr = (TextView) findViewById(R.id.tvToCurr);
        tvRateValue = (TextView) findViewById(R.id.tvRateValue);
        tvCreatedAt = (TextView) findViewById(R.id.tvCreatedAtValue);
        tvSendToAddress = (TextView) findViewById(R.id.tvSendToAddress);
        btCopyToClipboard = (Button) findViewById(R.id.btChangellyCopy);

        ChangellyTransactionOffer offer = (ChangellyTransactionOffer) getIntent().getExtras().getSerializable(ChangellyService.OFFER);

        tvFromAmount.setText(Double.toString(offer.amountFrom));
        tvFromCurr.setText(offer.currencyFrom);
        tvToAmount.setText(Double.toString(offer.amountTo));
        tvToCurr.setText(offer.currencyTo);
        if(offer.amountFrom > 0) {
            tvRateValue.setText(Double.toString(offer.amountTo / offer.amountFrom));
        } else {
            tvRateValue.setText("");
        }
        tvCreatedAt.setText(offer.createdAt);
        tvSendToAddress.setText(offer.payinAddress);

        btCopyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData cd = ClipData.newPlainText("address", tvSendToAddress.getText());
                cm.setPrimaryClip(cd);
            }
        });
    }

}