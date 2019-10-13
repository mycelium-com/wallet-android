//
//package com.mycelium.wallet.activity.send;
//
//import android.annotation.SuppressLint;
//import android.content.Intent;
//import android.os.Bundle;
//import android.support.v4.app.DialogFragment;
//import android.support.v4.app.FragmentActivity;
//import android.view.Window;
//import android.widget.ProgressBar;
//import butterknife.BindView;
//import butterknife.ButterKnife;
//import com.google.common.base.Strings;
//import com.mycelium.wallet.MbwManager;
//import com.mycelium.wallet.R;
//import com.mycelium.wallet.activity.send.event.BroadcastResultListener;
//import com.mycelium.wallet.event.ExchangeRatesRefreshed;
//import com.mycelium.wallet.event.SelectedCurrencyChanged;
//import com.mycelium.wallet.paymentrequest.PaymentRequestHandler;
//import com.mycelium.wapi.content.GenericAssetUri;
//import com.mycelium.wapi.content.WithCallback;
//import com.mycelium.wapi.wallet.BroadcastResult;
//import com.mycelium.wapi.wallet.coins.Value;
//import com.squareup.otto.Subscribe;
//import org.jetbrains.annotations.NotNull;
//
//import static android.view.View.GONE;
//
//public class SendMainActivity extends FragmentActivity implements BroadcastResultListener {
//    private static final int REQUEST_PAYMENT_HANDLER = 8;
//
//    public static final String ACCOUNT = "account";
//    private static final String AMOUNT = "amount";
//    public static final String IS_COLD_STORAGE = "isColdStorage";
//    public static final String TRANSACTION_LABEL = "transactionLabel";
//    public static final String ASSET_URI = "assetUri";
//    public static final String SIGNED_TRANSACTION = "signedTransaction";
//    public static final String TRANSACTION_FIAT_VALUE = "transaction_fiat_value";
//
//    @BindView(R.id.pbSend)
//    ProgressBar pbSend;
//
//    private MbwManager _mbwManager;
//
//    private PaymentRequestHandler _paymentRequestHandler;
//
//    private Value _amountToSend;
//
//    protected String _transactionLabel;
//    private GenericAssetUri genericUri;
//
//    private DialogFragment activityResultDialog;
//
//    @SuppressLint({"ShowToast", "StaticFieldLeak"})
//    @Override
//    public void onCreate(Bundle savedInstanceState) {
//        // TODO: profile. slow!
//        requestWindowFeature(Window.FEATURE_NO_TITLE);
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.send_main_activity);
//        ButterKnife.bind(this);
//        _mbwManager = MbwManager.getInstance(getApplication());
//
//        _amountToSend = (Value) getIntent().getSerializableExtra(AMOUNT);
//
//        //May be null
//        _transactionLabel = getIntent().getStringExtra(TRANSACTION_LABEL);
//        //May be null
//        genericUri = (GenericAssetUri) getIntent().getSerializableExtra(ASSET_URI);
//
//       // TODO here was init after loading
//
//        // lets check whether we got a payment request uri and need to fetch payment data
//        if (genericUri instanceof WithCallback
//                && !Strings.isNullOrEmpty(((WithCallback) genericUri).getCallbackURL()) && _paymentRequestHandler == null) {
//            verifyPaymentRequest(genericUri);
//        }
//    }
//
//    private void verifyPaymentRequest(GenericAssetUri uri) {
//        Intent intent = VerifyPaymentRequestActivity.getIntent(this, uri);
//        startActivityForResult(intent, REQUEST_PAYMENT_HANDLER);
//    }
//
//    @Override
//    protected void onResume() {
//        MbwManager.getEventBus().register(this);
//
//        // If we don't have a fresh exchange rate, now is a good time to request one, as we will need it in a minute
//        if (!_mbwManager.getCurrencySwitcher().isFiatExchangeRateAvailable()) {
//            _mbwManager.getExchangeRateManager().requestRefresh();
//        }
//
//        pbSend.setVisibility(GONE);
//
//        super.onResume();
//        if (activityResultDialog != null) {
//            activityResultDialog.show(getSupportFragmentManager(), "ActivityResultDialog");
//            activityResultDialog = null;
//        }
//    }
//
//    @Override
//    protected void onPause() {
//        MbwManager.getEventBus().unregister(this);
//        _mbwManager.getVersionManager().closeDialog();
//        super.onPause();
//    }
//
//    @Subscribe
//    public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
////        updateUi(); TODO
//    }
//
//    @Subscribe
//    public void selectedCurrencyChanged(SelectedCurrencyChanged event) {
////        updateUi(); TODO
//    }
//}
