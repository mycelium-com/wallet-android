package com.mycelium.wallet.external.changelly;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.external.changelly.model.ChangellyResponse;
import com.mycelium.wallet.external.changelly.model.ChangellyTransactionOffer;
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

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC;
import static com.mycelium.wallet.external.changelly.ChangellyConstants.decimalFormat;

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

    @BindView(R.id.extra_layout)
    View extraLayout;

    @BindView(R.id.payInExtraId)
    TextView payInExtraId;

    @BindView(R.id.extraIdText)
    TextView extraIdText;

    @BindView(R.id.transaction_id)
    TextView transactionId;

    private ChangellyTransactionOffer offer;
    private ProgressDialog progressDialog;
    private String currency;
    private Double amount;
    private String receivingAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_offer_activity);
        getSupportActionBar().hide();
        ButterKnife.bind(this);
        createOffer();
    }

    private void updateUI() {
        tvFromAmount.setText(getString(R.string.value_currency, offer.currencyFrom
                , ChangellyConstants.decimalFormat.format(amount)));
        tvSendToAddress.setText(offer.payinAddress);
        transactionId.setText(getString(R.string.exchange_operation_id_s, offer.id));

        if (offer.payinExtraId != null) {
            payInExtraId.setText(getExtraIdName(offer.currencyFrom) + ": " + offer.payinExtraId);
            extraIdText.setText(getString(R.string.changelly_indicate_extra, getExtraIdName(offer.currencyFrom)));
            extraLayout.setVisibility(VISIBLE);
        } else {
            extraLayout.setVisibility(GONE);
        }
    }

    private String getExtraIdName(String coin) {
        switch (coin) {
            case ChangellyConstants.XRP:
                return getString(R.string.changelly_destination_tag);
            case ChangellyConstants.XEM:
                return getString(R.string.changelly_message_name);
            default:
                return getString(R.string.changelly_memo_id_name);
        }
    }

    @OnClick(R.id.tvSendToAddress)
    void clickAddress() {
        if(offer != null) {
            Utils.setClipboardString(offer.payinAddress, this);
            toast("Address copied to clipboard");
        } else {
            toast("Something went wrong. No offer");
        }
    }

    @OnClick(R.id.payInExtraId)
    void clickExtraId() {
        if(offer != null) {
            Utils.setClipboardString(offer.payinExtraId, this);
            toast(getExtraIdName(offer.currencyFrom) + " copied to clipboard");
        } else {
            toast("Something went wrong. No offer");
        }
    }
    @OnClick(R.id.transaction_id)
    void transactionId() {
        if(offer != null) {
            Utils.setClipboardString(offer.id, this);
            toast("Operation id copied to clipboard");
        } else {
            toast("Something went wrong. No offer");
        }
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
        new Toaster(this).toast(msg, true);
    }

    private void createOffer() {
        amount = getIntent().getDoubleExtra(ChangellyAPIService.AMOUNT, 0);
        currency = getIntent().getStringExtra(ChangellyAPIService.FROM);
        receivingAddress = getIntent().getStringExtra(ChangellyAPIService.DESTADDRESS);
        ChangellyRetrofitFactory.INSTANCE.getChangellyApi()
                .createTransaction(currency, BTC, amount, receivingAddress)
                .enqueue(new GetOfferCallback(amount));
        progressDialog = new ProgressDialog(this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage(getString(R.string.waiting_offer));
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
        order.receivingAmount = offer.amountTo.stripTrailingZeros().toPlainString();
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

    class GetOfferCallback implements Callback<ChangellyResponse<ChangellyTransactionOffer>> {
        private double amountFrom;

        public GetOfferCallback(double amountFrom) {
            this.amountFrom = amountFrom;
        }

        @Override
        public void onResponse(@NonNull Call<ChangellyResponse<ChangellyTransactionOffer>> call,
                               @NonNull Response<ChangellyResponse<ChangellyTransactionOffer>> response) {
            ChangellyResponse<ChangellyTransactionOffer> result = response.body();
            if(result != null && result.getResult() != null) {
                progressDialog.dismiss();
                // if the amount changed after the offer was requested but before the offer was
                // received, we reset the amount to the requested amount instead of ignoring the
                // offer.
                // If the user requested a new offer meanwhile, the new amount will return with
                // the new offer, too.
                amount = amountFrom;
                offer = result.getResult();
                sendOrderToService(getOrder());
                updateUI();
            } else {
                toast("Something went wrong.");
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyResponse<ChangellyTransactionOffer>> call,
                              @NonNull Throwable t) {
            toast("Service unavailable");
        }
    }
}
