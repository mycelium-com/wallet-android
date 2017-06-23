package com.mycelium.wallet.activity.rmc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.QrImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * Created by elvis on 22.06.17.
 */

public class EthPaymentRequestActivity extends ActionBarActivity {
    String tokenCount;

    @BindView(R.id.ivQrCode)
    QrImageView ivQrCode;

    @BindView(R.id.btShare)
    Button btShare;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eth_pay_request);
        ButterKnife.bind(this);
        tokenCount = getIntent().getStringExtra(Keys.RMC_COUNT);
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
    }

    void update() {
        String paymentUri =  getPaymentUri();
        ivQrCode.setQrCode(paymentUri);
    }

    public void shareRequest(View view) {
        Intent s = new Intent(android.content.Intent.ACTION_SEND);
        s.setType("text/plain");
        if (tokenCount.isEmpty()) {
            s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.bitcoin_address_title));
            s.putExtra(Intent.EXTRA_TEXT, getAddress());
            startActivity(Intent.createChooser(s, getResources().getString(R.string.share_bitcoin_address)));
        } else {
            s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.payment_request));
            s.putExtra(Intent.EXTRA_TEXT, getPaymentUri());
            startActivity(Intent.createChooser(s, getResources().getString(R.string.share_payment_request)));
        }
    }


    public void copyToClipboard(View view) {
        String text;
        if (tokenCount.isEmpty()) {
            text = getAddress();
        } else {
            text = getPaymentUri();
        }
        Utils.setClipboardString(text, this);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public String getPaymentUri() {
        final StringBuilder uri = new StringBuilder("ethereum:");
        uri.append(getAddress());
        if (!tokenCount.isEmpty()) {
            uri.append("?value=").append(tokenCount);
        }
        return uri.toString();
    }

    private String getAddress() {
        return "0x8b7f8bd6b3e7997666871b86c3a4297252e8e5ad";
    }
}
