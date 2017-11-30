package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;

import java.util.Locale;

public class ChangellyOfferActivity extends Activity {
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);

        TextView tvFromAmount = (TextView) findViewById(R.id.tvFromAmount);
        TextView tvFromCurr = (TextView) findViewById(R.id.tvFromCurr);
        TextView tvToAmount = (TextView) findViewById(R.id.tvToAmount);
        TextView tvToCurr = (TextView) findViewById(R.id.tvToCurr);
        final TextView tvSendToAddress = (TextView) findViewById(R.id.tvSendToAddress);
        Button btCopyToClipboard = (Button) findViewById(R.id.btChangellyCopy);

        ChangellyTransactionOffer offer = (ChangellyTransactionOffer) getIntent().getExtras().getSerializable(ChangellyService.OFFER);

        tvFromAmount.setText(String.format(Locale.getDefault(),"%f", offer.amountFrom));
        tvFromCurr.setText(offer.currencyFrom);
        tvToAmount.setText(String.format(Locale.getDefault(),"%f", offer.amountTo));
        tvToCurr.setText(offer.currencyTo);
        tvSendToAddress.setText(offer.payinAddress);

        btCopyToClipboard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Utils.setClipboardString(tvSendToAddress.getText().toString(), ChangellyOfferActivity.this);
            }
        });
    }
}