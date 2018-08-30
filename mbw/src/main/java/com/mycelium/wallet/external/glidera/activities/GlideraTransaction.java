package com.mycelium.wallet.external.glidera.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TableRow;
import android.widget.TextView;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.TransactionDetailsLabel;
import com.mycelium.wallet.external.glidera.GlideraUtils;
import com.mycelium.wallet.external.glidera.api.GlideraService;
import com.mycelium.wallet.external.glidera.api.response.OrderState;
import com.mycelium.wallet.external.glidera.api.response.TransactionResponse;
import com.mycelium.wapi.wallet.GenericTransaction;

import java.text.DateFormat;
import java.util.UUID;

import rx.Observer;

public class GlideraTransaction extends Activity {
   private GlideraService glideraService;
   private MbwManager mbwManager;
   private UUID transactionUUID;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      glideraService = GlideraService.getInstance();
      mbwManager = MbwManager.getInstance(this);

      setContentView(R.layout.glidera_transaction);

      if (getActionBar() != null) {
         getActionBar().setDisplayHomeAsUpEnabled(true);
      }

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
        /*
        Details
         */
      TextView tvDetails = (TextView) findViewById(R.id.tvDetails);
      switch (transactionResponse.getStatus()) {
         case PROCESSING:
            tvDetails.setText(getString(R.string.gd_transaction_initiated));
            break;
         case COMPLETE:
            tvDetails.setText(getString(R.string.gd_transaction_complete));
            break;
         case PENDING_REVIEW:
            tvDetails.setText(getString(R.string.gd_transaction_reviewed));
            break;
         default:
            tvDetails.setText(getString(R.string.gd_transaction_error));
            break;
      }

        /*
        Transaction Hash
         */
      if (transactionResponse.getTransactionHash() != null && !transactionResponse.getTransactionHash().toString().isEmpty()) {
         TransactionDetailsLabel tvTransactionHash = ((TransactionDetailsLabel) findViewById(R.id.tvTransactionHash));
         GenericTransaction txDetails;
         try {
            txDetails = mbwManager.getSelectedAccountGeneric().getTransaction(transactionResponse.getTransactionHash().toString());

         } catch (RuntimeException runtimeException) {
                /*
                If this was not a mycelium transaction it will throw a runtime exception, This could happen if they buy on the Glidera
                website or their Glidera account is connected to multiple wallets
                 */
            txDetails = null;
         }

         if (txDetails != null) {
            tvTransactionHash.setTransaction(txDetails);
         } else {
            TableRow trTransactionHash = (TableRow) findViewById(R.id.trTransactionHash);
            trTransactionHash.setVisibility(View.GONE);
         }
      } else {
         TableRow trTransactionHash = (TableRow) findViewById(R.id.trTransactionHash);
         trTransactionHash.setVisibility(View.GONE);
      }

        /*
        Glidera transaction reference UUID
         */
      TextView tvTransactionUUID = (TextView) findViewById(R.id.tvTransactionUUID);
      tvTransactionUUID.setText(transactionResponse.getTransactionUuid().toString());

        /*
        Transaction date
         */
      String transactionDate = DateFormat.getDateInstance().format(transactionResponse.getTransactionDate());
      TextView tvTransactionDate = (TextView) findViewById(R.id.tvTransactionDate);
      tvTransactionDate.setText(transactionDate);

        /*
        Estimated delivery date
         */
      if (transactionResponse.getEstimatedDeliveryDate() != null) {
         TextView tvDeliveryDate = (TextView) findViewById(R.id.tvDeliveryDate);
         String estimatedDeliveryDate = DateFormat.getDateInstance().format(transactionResponse.getEstimatedDeliveryDate());
         tvDeliveryDate.setText(estimatedDeliveryDate);
      } else {
         TableRow trDeliveryDate = (TableRow) findViewById(R.id.trDeliveryDate);
         trDeliveryDate.setVisibility(View.GONE);
      }

        /*
        Status
         */
      TextView tvStatus = (TextView) findViewById(R.id.tvStatus);
      String status = transactionResponse.getStatus().toString().substring(0, 1).toUpperCase() + transactionResponse.getStatus()
              .toString().substring(1).toLowerCase();
      tvStatus.setText(status);

        /*
        Type
         */
      TextView tvType = (TextView) findViewById(R.id.tvType);
      String type = transactionResponse.getType().toString().substring(0, 1).toUpperCase() + transactionResponse.getType().toString()
              .substring(1).toLowerCase();
      tvType.setText(type);

        /*
        Amount
         */
      TextView tvAmount = (TextView) findViewById(R.id.tvAmount);
      String amount = String.format(
              getString(R.string.gd_buy_sell_amount_x_in_btc_for_amount_y_in_fiat),
              GlideraUtils.formatBtcForDisplay(transactionResponse.getQty()),
              GlideraUtils.formatFiatForDisplay(transactionResponse.getTotal())
      );
      tvAmount.setText(amount);

        /*
        Subtotal
         */
      TextView tvSubtotal = (TextView) findViewById(R.id.tvSubtotal);
      tvSubtotal.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getSubtotal()));

        /*
        Fees
         */
      TextView tvFees = (TextView) findViewById(R.id.tvFees);
      tvFees.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getFees()));

        /*
        Total
         */
      TextView tvTotal = (TextView) findViewById(R.id.tvTotal);
      tvTotal.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getTotal()));

        /*
        Price
         */
      TextView tvPricePerBtc = (TextView) findViewById(R.id.tvPricePerBtc);
      tvPricePerBtc.setText(GlideraUtils.formatFiatForDisplay(transactionResponse.getPrice()));
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
      Intent intent = new Intent(this, GlideraMainActivity.class);
      intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      Bundle bundle = new Bundle();
      bundle.putString("tab", "history");
      intent.putExtras(bundle);
      startActivity(intent);
   }
}
