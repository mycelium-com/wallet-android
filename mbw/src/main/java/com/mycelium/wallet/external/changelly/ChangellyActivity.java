package com.mycelium.wallet.external.changelly;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;

public class ChangellyActivity extends AppCompatActivity {
    public static final int REQUEST_OFFER = 100;
    private static String TAG = "ChangellyActivity";

    public enum ChangellyUITypes {
        Loading,
        RetryLater,
        Main
    }

    @BindView(R.id.tvMinAmountValue)
    TextView tvMinAmountValue;

    @BindView(R.id.fromLayout)
    View fromLayout;

    @BindView(R.id.fromValue)
    TextView fromValue;

    @BindView(R.id.fromCurrency)
    TextView fromCurrency;

    @BindView(R.id.toLayout)
    View toLayout;

    @BindView(R.id.toValue)
    TextView toValue;

    @BindView(R.id.btChangellyCreateTransaction)
    Button btTakeOffer;

    @BindView(R.id.currencySelector)
    SelectableRecyclerView currencySelector;

    @BindView(R.id.accountSelector)
    SelectableRecyclerView accountSelector;

    @BindView(R.id.numeric_keyboard)
    ValueKeyboard valueKeyboard;

    @BindView(R.id.title)
    View titleView;

    @BindView(R.id.subtitle)
    View subtitleView;

    @BindView(R.id.llChangellyErrorWrapper)
    View llChangellyErrorWrapper;

    @BindView(R.id.llChangellyLoadingProgress)
    View llChangellyLoadingProgress;

    @BindView(R.id.llChangellyMain)
    ScrollView llChangellyMain;

    @BindView(R.id.llChangellyValidationWait)
    View llChangellyValidationWait;

    private CurrencyAdapter currencyAdapter;
    private AccountAdapter accountAdapter;
    private Receiver receiver;

    private Double minAmount;

    private void requestOfferFunction(String amount, String fromCurrency, String toCurrency) {
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            Toast.makeText(ChangellyActivity.this, "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.FROM, fromCurrency)
                .putExtra(ChangellyService.TO, toCurrency)
                .putExtra(ChangellyService.AMOUNT, dblAmount);
        startService(changellyServiceIntent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.changelly_activity);
        setTitle(getString(R.string.exchange_altcoins_to_btc));
        ButterKnife.bind(this);
        MbwManager mbwManager = MbwManager.getInstance(this);

        tvMinAmountValue.setVisibility(View.GONE); // cannot edit field before selecting a currency

        valueKeyboard.setMaxDecimals(8);
        valueKeyboard.setInputListener(new ValueKeyboard.SimpleInputListener() {
            @Override
            public void done() {
                titleView.setVisibility(View.VISIBLE);
                subtitleView.setVisibility(View.VISIBLE);
                fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
                toLayout.setAlpha(Constants.INACTIVE_ALPHA);
            }
        });
        fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);

        currencySelector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        accountSelector.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        int senderFinalWidth = getWindowManager().getDefaultDisplay().getWidth();
        int firstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;

        currencyAdapter = new CurrencyAdapter(firstItemWidth);
        currencySelector.setAdapter(currencyAdapter);
        currencySelector.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                CurrencyAdapter.Item item = currencyAdapter.getItem(position);
                if (item != null) {
                    fromCurrency.setText(item.currency);
                    fromValue.setText(null);
                    minAmount = 0.0;
                    toValue.setText("");

                    // load min amount
                    Intent changellyServiceIntent = new Intent(ChangellyActivity.this, ChangellyService.class)
                            .setAction(ChangellyService.ACTION_GET_MIN_EXCHANGE)
                            .putExtra(ChangellyService.FROM, item.currency)
                            .putExtra(ChangellyService.TO, ChangellyService.BTC);
                    startService(changellyServiceIntent);
                }
            }
        });
        List<WalletAccount> toAccounts = new ArrayList<>();
        toAccounts.addAll(AccountManager.INSTANCE.getBTCBip44Accounts().values());
        toAccounts.addAll(AccountManager.INSTANCE.getBTCSingleAddressAccounts().values());
        toAccounts.addAll(AccountManager.INSTANCE.getCoinapultAccounts().values());
        accountAdapter = new AccountAdapter(mbwManager, toAccounts, firstItemWidth);
        accountSelector.setAdapter(accountAdapter);
        accountSelector.setSelectedItem(mbwManager.getSelectedAccount());

        //display the loading spinner
        setLayout(ChangellyActivity.ChangellyUITypes.Loading);

        receiver = new Receiver();
        // The filter's action is BROADCAST_ACTION
        for (String action : new String[]{
                ChangellyService.INFO_CURRENCIES,
                ChangellyService.INFO_EXCH_AMOUNT,
                ChangellyService.INFO_MIN_AMOUNT,
                ChangellyService.INFO_TRANSACTION,
                ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(this).registerReceiver(receiver, intentFilter);
        }

        Intent changellyServiceIntent = new Intent(this, ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_CURRENCIES);
        startService(changellyServiceIntent);
    }


    @Override
    protected void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    /* Activity UI logic Start */
    private void setLayout(ChangellyUITypes uiType) {
        llChangellyValidationWait.setVisibility(View.GONE);
        llChangellyLoadingProgress.setVisibility(View.GONE); // always gone
        llChangellyErrorWrapper.setVisibility(View.GONE);
        llChangellyMain.setVisibility(View.GONE);
        switch (uiType) {
            case Loading:
                llChangellyValidationWait.setVisibility(View.VISIBLE);
                break;
            case RetryLater:
                llChangellyErrorWrapper.setVisibility(View.VISIBLE);
            case Main:
                llChangellyMain.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onBackPressed() {
        if (valueKeyboard.getVisibility() == View.VISIBLE) {
            valueKeyboard.done();
        } else if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }

    private boolean avoidTextChangeEvent = false;

    @OnTextChanged(value = R.id.fromValue, callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputFrom(Editable editable) {
        if (!avoidTextChangeEvent && isValueForOfferOk(true)) {
            requestOfferFunction(fromValue.getText().toString()
                    , currencyAdapter.getItem(currencySelector.getSelectedItem()).currency
                    , ChangellyService.BTC);
        }
        if (!avoidTextChangeEvent && fromValue.getText().toString().isEmpty()) {
            avoidTextChangeEvent = true;
            toValue.setText(null);
            avoidTextChangeEvent = false;
        }
    }

    @OnTextChanged(value = R.id.toValue, callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputTo(Editable editable) {
        if (!avoidTextChangeEvent && !toValue.getText().toString().isEmpty()) {
            requestOfferFunction(toValue.getText().toString()
                    , ChangellyService.BTC
                    , currencyAdapter.getItem(currencySelector.getSelectedItem()).currency);
        }
        if (!avoidTextChangeEvent && toValue.getText().toString().isEmpty()) {
            avoidTextChangeEvent = true;
            fromValue.setText(null);
            avoidTextChangeEvent = false;
        }
    }

    @OnClick(R.id.fromLayout)
    void clickFromValue() {
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setInputTextView(fromValue);
        valueKeyboard.setEntry(fromValue.getText().toString());
        fromLayout.setAlpha(Constants.ACTIVE_ALPHA);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);

        llChangellyMain.post(new Runnable() {
            @Override
            public void run() {
                llChangellyMain.smoothScrollTo(0, fromLayout.getTop());
            }
        });
    }

    @OnClick(R.id.toLayout)
    void clickToValue() {
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setInputTextView(toValue);
        valueKeyboard.setEntry(toValue.getText().toString());
        fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
        toLayout.setAlpha(Constants.ACTIVE_ALPHA);

        llChangellyMain.post(new Runnable() {
            @Override
            public void run() {
                llChangellyMain.smoothScrollTo(0, toLayout.getTop());
            }
        });
    }

    @OnClick(R.id.btChangellyCreateTransaction)
    void offerClick() {
        String txtAmount = fromValue.getText().toString();
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            toast("Error exchanging value");
            btTakeOffer.setEnabled(false);
            return;
        }
        CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
        WalletAccount walletAccount = accountAdapter.getItem(accountSelector.getSelectedItem()).account;
        startActivityForResult(new Intent(ChangellyActivity.this, ChangellyOfferActivity.class)
                .putExtra(ChangellyService.FROM, item.currency)
                .putExtra(ChangellyService.TO, ChangellyService.BTC)
                .putExtra(ChangellyService.AMOUNT, dblAmount)
                .putExtra(ChangellyService.DESTADDRESS, walletAccount.getReceivingAddress().get().toString()), REQUEST_OFFER);
    }

    boolean isValueForOfferOk(boolean checkMin) {
        tvMinAmountValue.setVisibility(View.GONE);
        String txtAmount = fromValue.getText().toString();
        if (txtAmount.isEmpty()) {
            btTakeOffer.setEnabled(false);
            return false;
        }
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            toast("Error exchanging value");
            btTakeOffer.setEnabled(false);
            return false;
        }

        if (checkMin && minAmount == 0) {
            btTakeOffer.setEnabled(false);
            toast("Please wait while loading minimum amount information.");
            return false;
        } else if (checkMin && dblAmount.compareTo(minAmount) < 0) {
            btTakeOffer.setEnabled(false);
            tvMinAmountValue.setVisibility(View.VISIBLE);
            return false;
        } // TODO: compare with maximum
        btTakeOffer.setEnabled(true);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OFFER) {
            if (resultCode == ChangellyOfferActivity.RESULT_FINISH) {
                finish();
            }
        }
    }

    public boolean containsCaseInsensitive(String str, String[] strings) {
        for (String string : strings) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            String from, to;
            double amount;

            switch (intent.getAction()) {
                case ChangellyService.INFO_CURRENCIES:
                    Log.d(TAG, "receiver, got currencies");
                    ArrayList<String> currenciesRes = intent.getStringArrayListExtra(ChangellyService.CURRENCIES);
                    if (currenciesRes != null) {
                        Log.d(TAG, "currencies=" + currenciesRes);
                        Collections.sort(currenciesRes);
                        List<CurrencyAdapter.Item> itemList = new ArrayList<>();
                        itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                        String[] skipCurrencies = getResources().getStringArray(R.array.changelly_skip_currencies);
                        for (String curr : currenciesRes) {
                            if (!curr.equalsIgnoreCase("btc") &&
                                    !containsCaseInsensitive(curr, skipCurrencies)) {
                                itemList.add(new CurrencyAdapter.Item(curr.toUpperCase(), CurrencyAdapter.VIEW_TYPE_ITEM));
                            }
                        }
                        itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                        currencyAdapter.setItems(itemList);
                        setLayout(ChangellyUITypes.Main);
                    }
                    break;
                case ChangellyService.INFO_MIN_AMOUNT:
                    from = intent.getStringExtra(ChangellyService.FROM);
                    to = intent.getStringExtra(ChangellyService.TO);
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                    CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
                    if (item != null && from != null && to != null && to.equalsIgnoreCase(ChangellyService.BTC)
                            && from.equalsIgnoreCase(item.currency)) {
                        Log.d(TAG, "Received minimum amount: " + amount + " " + from);
                        minAmount = amount;
                        tvMinAmountValue.setText(getString(R.string.exchange_minimum_amount
                                , decimalFormat.format(minAmount), item.currency));
                    }
                    break;
                case ChangellyService.INFO_EXCH_AMOUNT:
                    from = intent.getStringExtra(ChangellyService.FROM);
                    to = intent.getStringExtra(ChangellyService.TO);
                    double fromAmount = intent.getDoubleExtra(ChangellyService.FROM_AMOUNT, 0);
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                    item = currencyAdapter.getItem(currencySelector.getSelectedItem());
                    if (item != null && from != null && to != null) {
                        Log.d(TAG, "Received offer: " + amount + " " + to);
                        avoidTextChangeEvent = true;
                        try {
                            if (to.equalsIgnoreCase(ChangellyService.BTC)
                                    && from.equalsIgnoreCase(item.currency)
                                    && fromAmount == Double.parseDouble(fromValue.getText().toString())) {
                                toValue.setText(decimalFormat.format(amount));
                            } else if (from.equalsIgnoreCase(ChangellyService.BTC)
                                    && to.equalsIgnoreCase(item.currency)
                                    && fromAmount == Double.parseDouble(toValue.getText().toString())) {
                                fromValue.setText(decimalFormat.format(amount));
                            }
                            isValueForOfferOk(true);

                        } catch (NumberFormatException ignore) {
                        }
                        avoidTextChangeEvent = false;
                    }
                    break;
                case INFO_ERROR:
                    Toast.makeText(ChangellyActivity.this,
                            "Service unavailable",
                            Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }
}
