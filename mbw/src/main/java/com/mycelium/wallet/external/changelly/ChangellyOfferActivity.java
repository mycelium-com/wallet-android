package com.mycelium.wallet.external.changelly;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyTransaction;
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

import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BCH;
import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC;
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
    private String currency;
    private Double amount;
    private String receivingAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);
        setTitle(getString(R.string.exchange_altcoins_to_btc));
        ButterKnife.bind(this);
        createOffer();
    }

    private void updateUI() {
        tvFromAmount.setText(getString(R.string.value_currency, offer.currencyFrom
                , Constants.decimalFormat.format(amount)));
        tvSendToAddress.setText(offer.payinAddress);
    }

    @OnClick(R.id.tvSendToAddress)
    void clickAddress() {
        Utils.setClipboardString(offer.payinAddress, this);
        toast("Address copied to clipboard");
    }

    @OnClick(R.id.tvFromAmount)
    void clickAmount() {
        Utils.setClipboardString(String.valueOf(amount), this);
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
        amount = getIntent().getDoubleExtra(ChangellyAPIService.AMOUNT, 0);
        currency = getIntent().getStringExtra(ChangellyAPIService.FROM);
        receivingAddress = getIntent().getStringExtra(ChangellyAPIService.DESTADDRESS);
        ChangellyAPIService.retrofit.create(ChangellyAPIService.class)
                .createTransaction(currency, BTC, amount, receivingAddress)
                .enqueue(new GetOfferCallback(amount));
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Waiting offer...");
        progressDialog.show();
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

    class GetOfferCallback implements Callback<ChangellyTransaction> {
        private double amountFrom;

        public GetOfferCallback(double amountFrom) {
            this.amountFrom = amountFrom;
        }

        @Override
        public void onResponse(@NonNull Call<ChangellyTransaction> call,
                               @NonNull Response<ChangellyTransaction> response) {
            ChangellyTransaction result = response.body();
            if(result != null && result.result != null) {
                progressDialog.dismiss();
                // if the amount changed after the offer was requested but before the offer was
                // received, we reset the amount to the requested amount instead of ignoring the
                // offer.
                // If the user requested a new offer meanwhile, the new amount will return with
                // the new offer, too.
                amount = amountFrom;
                offer = result.result;
                sendOrderToService(getOrder());
                updateUI();
            } else {
                toast("Something went wrong.");
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyTransaction> call,
                              @NonNull Throwable t) {
            toast("Service unavailable");
        }
    }
}
