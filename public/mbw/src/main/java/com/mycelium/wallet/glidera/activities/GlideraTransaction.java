package com.mycelium.wallet.glidera.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableRow;
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

        if( getActionBar() != null )
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
        TextView tvTransactionUUID = (TextView) findViewById(R.id.tvTransactionUUID);
        TextView tvTransactionDate = (TextView) findViewById(R.id.tvTransactionDate);
        TextView tvDeliveryDate = (TextView) findViewById(R.id.tvDeliveryDate);
        TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
        TextView tvType = (TextView) findViewById(R.id.tvType);
        TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
        TextView tvSubtotal = (TextView) findViewById(R.id.tvSubtotal);
        TextView tvFees = (TextView) findViewById(R.id.tvFees);
        TextView tvTotal = (TextView) findViewById(R.id.tvTotal);
        TextView tvPricePerBtc = (TextView) findViewById(R.id.tvPricePerBtc);
        TableRow trDeliveryDate = (TableRow) findViewById(R.id.trDeliveryDate);

        String status = transactionResponse.getStatus().toString().substring(0, 1).toUpperCase() + transactionResponse.getStatus()
                .toString().substring(1).toLowerCase();
        String type = transactionResponse.getType().toString().substring(0, 1).toUpperCase() + transactionResponse.getType().toString()
                .substring(1).toLowerCase();

        String transactionDate = DateFormat.getDateInstance().format(transactionResponse.getTransactionDate());

        String amount = GlideraUtils.formatBtcForDisplay(transactionResponse.getQty()) + " for " + GlideraUtils.formatFiatForDisplay(transactionResponse.getTotal());

        tvTransactionUUID.setText(transactionResponse.getTransactionUuid().toString());
        tvTransactionDate.setText(transactionDate);

        if (transactionResponse.getEstimatedDeliveryDate() != null ) {
            String estimatedDeliveryDate = DateFormat.getDateInstance().format(transactionResponse.getEstimatedDeliveryDate());
            tvDeliveryDate.setText(estimatedDeliveryDate);
        } else {
            trDeliveryDate.setVisibility(View.GONE);
        }

        tvStatus.setText(status);
        tvType.setText(type);
        tvAmount.setText(amount);
        tvPricePerBtc.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getPrice()));
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

    @Override
    public void onBackPressed() {
        Intent intent = new Intent(GlideraTransaction.this, GlideraMainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle bundle = new Bundle();
        bundle.putString("tab", "history");
        intent.putExtras(bundle);
        startActivity(intent);
    }
}
