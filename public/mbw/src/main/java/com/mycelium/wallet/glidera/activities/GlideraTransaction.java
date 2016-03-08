package com.mycelium.wallet.glidera.activities;

import android.app.Activity;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.glidera.GlideraUtils;
import com.mycelium.wallet.glidera.api.GlideraService;
import com.mycelium.wallet.glidera.api.response.TransactionResponse;

import java.text.DateFormat;
import java.util.UUID;

import rx.Observer;

public class GlideraTransaction extends Activity {
    private GlideraService glideraService;
    private UUID transactionUUID;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        glideraService = GlideraService.getInstance();

        setContentView(R.layout.glidera_transaction);

        getActionBar().setDisplayHomeAsUpEnabled(true);

        Bundle bundle = getIntent().getExtras();
        String uuid = bundle.getString("transactionuuid");

        transactionUUID = UUID.fromString(uuid);
    }

    @Override
    protected void onResume() {
        super.onResume();

        glideraService.transaction(transactionUUID).subscribe(new Observer<TransactionResponse>() {
            @Override
            public void onCompleted() {

            }

            @Override
            public void onError(Throwable e) {

            }

            @Override
            public void onNext(TransactionResponse transactionResponse) {
                updateTransaction(transactionResponse);
            }
        });

    }

    private void updateTransaction(TransactionResponse transactionResponse) {
        TextView tvTransactionDate = (TextView) findViewById(R.id.tvTransactionDate);
        TextView tvDeliveryDate = (TextView) findViewById(R.id.tvDeliveryDate);
        TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
        TextView tvType = (TextView) findViewById(R.id.tvType);
        TextView tvBtc = (TextView) findViewById(R.id.tvBtc);
        TextView tvSubtotal = (TextView) findViewById(R.id.tvSubtotal);
        TextView tvFees = (TextView) findViewById(R.id.tvFees);
        TextView tvTotal = (TextView) findViewById(R.id.tvTotal);

        tvTransactionDate.setText(DateFormat.getDateInstance().format(transactionResponse.getTransactionDate()));
        if (transactionResponse.getEstimatedDeliveryDate() != null) {
            tvDeliveryDate.setText(DateFormat.getDateInstance().format(transactionResponse.getEstimatedDeliveryDate()));
        } else {
            tvDeliveryDate.setVisibility(View.GONE);
        }
        tvStatus.setText(transactionResponse.getStatus().toString().substring(0, 1).toUpperCase() + transactionResponse.getStatus()
                .toString().substring(1).toLowerCase());
        tvType.setText(transactionResponse.getType().toString().substring(0, 1).toUpperCase() + transactionResponse.getType().toString()
                .substring(1).toLowerCase());
        tvBtc.setText(GlideraUtils.formatBtcForDisplay(transactionResponse.getQty()));
        tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getSubtotal()));
        tvFees.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getFees()));
        tvTotal.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getTotal()));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
