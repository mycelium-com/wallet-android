package com.mycelium.wallet.external.changelly;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ChangellyOfferActivity extends Activity {
    @BindView(R.id.tvFromAmount)
    TextView tvFromAmount;

    @BindView(R.id.tvToAmount)
    TextView tvToAmount;

    @BindView(R.id.toAddress)
    TextView toAddress;

    @BindView(R.id.tvSendToAddress)
    TextView tvSendToAddress;

    private ChangellyTransactionOffer offer;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);
        ButterKnife.bind(this);

        offer = (ChangellyTransactionOffer) getIntent().getExtras().getSerializable(ChangellyService.OFFER);

        tvFromAmount.setText(getString(R.string.value_currency, offer.amountFrom, offer.currencyFrom));
        tvToAmount.setText(getString(R.string.value_currency, offer.amountTo, offer.currencyTo));
        toAddress.setText(offer.payoutAddress);
        tvSendToAddress.setText(offer.payinAddress);
    }

    @OnClick(R.id.btChangellyCopy)
    void clickCopyToClipboard() {
        Utils.setClipboardString(offer.payinAddress, ChangellyOfferActivity.this);
    }
}