package com.mycelium.wallet.external.changelly;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransactionOffer;
import com.mycelium.wallet.external.changelly.model.Order;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RetrofitError;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;

public class ChangellyOfferActivity extends AppCompatActivity {
    public static final int RESULT_FINISH = 11;
    public static final String TAG = "ChangellyOfferActivity";

    static {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true);
    }

    @BindView(R.id.tvFromAmount)
    TextView tvFromAmount;

    @BindView(R.id.tvSendToAddress)
    TextView tvSendToAddress;

    private ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;
    private Receiver receiver;
    private String currency;
    private Double amount;
    private String receivingAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);
        setTitle(getString(R.string.exchange_altcoins_to_btc));
        ButterKnife.bind(this);
        receiver = new Receiver();
        for (String action : new String[]{ChangellyService.INFO_TRANSACTION, ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }
        createOffer();
    }

    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void updateUI() {
        tvFromAmount.setText(getString(R.string.value_currency, offer.currencyFrom
                , Constants.decimalFormat.format(offer.amountFrom)));
        tvSendToAddress.setText(offer.payinAddress);
    }

    @OnClick(R.id.tvSendToAddress)
    void clickAddress() {
        Utils.setClipboardString(offer.payinAddress, this);
        toast("Address copied to clipboard");
    }

    @OnClick(R.id.tvFromAmount)
    void clickAmount() {
        Utils.setClipboardString(String.valueOf(offer.amountFrom), this);
        toast("Amount copied to clipboard");
    }

    @OnClick(R.id.exchange_more)
    void clickExchangeMore() {
        finish();
    }

    @OnClick(R.id.check_balance)
    void clickCheckBalance() {
        setResult(RESULT_FINISH);
        finish();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void createOffer() {
        amount = getIntent().getDoubleExtra(ChangellyService.AMOUNT, 0);
        currency = getIntent().getStringExtra(ChangellyService.FROM);
        receivingAddress = getIntent().getStringExtra(ChangellyService.DESTADDRESS);
        ChangellyService.start(this, ChangellyService.ACTION_CREATE_TRANSACTION, currency,
                ChangellyService.BTC, amount, Address.fromString(receivingAddress));
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Waiting offer...");
        progressDialog.show();
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case ChangellyService.INFO_TRANSACTION:
                    progressDialog.dismiss();
                    offer = (ChangellyTransactionOffer) intent.getSerializableExtra(ChangellyService.OFFER);
                    logExchange();
                    updateUI();
                    break;
                case INFO_ERROR:
                    progressDialog.dismiss();
                    new AlertDialog.Builder(ChangellyOfferActivity.this)
                            .setMessage(R.string.exchange_service_unavailable)
                            .setPositiveButton(R.string.button_ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    finish();
                                }
                            }).create().show();
                    break;
            }
        }
    }

    private void logExchange() {
        final Order order = getOrder();
        sendOrderToService(order);
    }

    @NonNull
    private Order getOrder() {
        final Order order = new Order();
        order.transactionId = "";
        order.order_id = offer.id;
        order.exchangingAmount = decimalFormat.format(amount);
        order.exchangingCurrency = currency;
        order.receivingAddress = receivingAddress;
        order.receivingAmount = decimalFormat.format(offer.amountTo);
        order.receivingCurrency = CurrencyValue.BTC;
        order.timestamp = SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.LONG, SimpleDateFormat.LONG, Locale.ENGLISH)
                .format(new Date());
        return order;
    }

    private void sendOrderToService(Order order) {
        try {
            ExchangeLoggingService.exchangeLoggingService.saveOrder(order).enqueue(new Callback<Void>() {
                @Override
                public void onResponse(Call<Void> call, Response<Void> response) {
                    Log.d(TAG, "logging success ");
                }

                @Override
                public void onFailure(Call<Void> call, Throwable t) {
                    Log.d(TAG, "logging failure", t);
                }
            });
        } catch (RetrofitError e) {
            Log.e(TAG, "Excange logging error", e);
        }
    }
}