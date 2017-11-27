package com.mycelium.wallet.activity.rmc;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.util.QrImageView;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 *
 */

public class EthPaymentRequestActivity extends ActionBarActivity {
    String address;
    String paymentURI;
    String amount;

    @BindView(R.id.ivQrCode)
    QrImageView ivQrCode;

    @BindView(R.id.btShare)
    Button btShare;

    @BindView(R.id.address)
    TextView addressView;

    @BindView(R.id.infoSendAddress)
    TextView infoSendAddressView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_eth_pay_request);
        ButterKnife.bind(this);
        paymentURI = getIntent().getStringExtra(Keys.PAYMENT_URI);
        address = getIntent().getStringExtra(Keys.ADDRESS);
        amount = getIntent().getStringExtra(Keys.ETH_COUNT);
        infoSendAddressView.setText("Please send " + amount + " ETH to this address");
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
    }

    void update() {
        String paymentUri = getPaymentUri();
        ivQrCode.setQrCode(paymentUri);

        addressView.setText(getAddress());
    }

    public void shareRequest(View view) {
        Intent s = new Intent(android.content.Intent.ACTION_SEND);
        s.setType("text/plain");
        s.putExtra(Intent.EXTRA_SUBJECT, getResources().getString(R.string.payment_request));
        s.putExtra(Intent.EXTRA_TEXT, getPaymentUri());
        startActivity(Intent.createChooser(s, getResources().getString(R.string.share_payment_request)));
    }


    public void copyAddressToClipboard(View view) {
        Utils.setClipboardString(address, this);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public void copyAmountToClipboard(View view) {
        Utils.setClipboardString(amount, this);
        Toast.makeText(this, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
    }

    public String getPaymentUri() {
        return paymentURI;
    }

    public String getAddress() {
        return address;
    }

}
