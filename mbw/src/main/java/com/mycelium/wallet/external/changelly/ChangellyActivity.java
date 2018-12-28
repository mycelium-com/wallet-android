package com.mycelium.wallet.external.changelly;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.google.common.base.Predicate;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.event.SelectListener;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.external.changelly.ChangellyAPIService.ChangellyAnswerDouble;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.HDAccount;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.mycelium.wallet.external.changelly.ChangellyAPIService.BTC;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;

public class ChangellyActivity extends AppCompatActivity {
    public static final int REQUEST_OFFER = 100;
    private static String TAG = "ChangellyActivity";
    private ChangellyAPIService changellyAPIService = ChangellyAPIService.retrofit.create(ChangellyAPIService.class);

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

    private Double minAmount;

    private void requestOfferFunction(String amount, String fromCurrency, String toCurrency) {
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            Toast.makeText(ChangellyActivity.this, "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        changellyAPIService.getExchangeAmount(fromCurrency, toCurrency, dblAmount).enqueue(new GetOfferCallback(fromCurrency, toCurrency, dblAmount));
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
                    changellyAPIService.getMinAmount(item.currency, BTC)
                            .enqueue(new GetMinCallback(item.currency));
                }
            }
        });
        List<WalletAccount> toAccounts = new ArrayList<>();
        Collection<WalletAccount> btcBip44Accounts = AccountManager.INSTANCE.getBTCBip44Accounts().values();
        List<WalletAccount> supportedHDAccounts = new ArrayList<>();
        for (WalletAccount account: btcBip44Accounts) {
            if (((HDAccount) account).getAvailableAddressTypes().contains(AddressType.P2SH_P2WPKH) ||
                ((HDAccount) account).getAvailableAddressTypes().contains(AddressType.P2PKH)) {
                supportedHDAccounts.add(account);
            }
        }
        Collection<WalletAccount> btcSingleAccounts = AccountManager.INSTANCE.getBTCSingleAddressAccounts().values();
        List<WalletAccount> supportedSAAccounts = new ArrayList<>();
        for (WalletAccount account: btcSingleAccounts) {
            if (((AbstractAccount) account).getAvailableAddressTypes().contains(AddressType.P2SH_P2WPKH) ||
                ((AbstractAccount) account).getAvailableAddressTypes().contains(AddressType.P2PKH)) {
                supportedSAAccounts.add(account);
            }
        }
        toAccounts.addAll(supportedHDAccounts);
        toAccounts.addAll(supportedSAAccounts);
        toAccounts.addAll(AccountManager.INSTANCE.getCoinapultAccounts().values());
        accountAdapter = new AccountAdapter(mbwManager, toAccounts, firstItemWidth);
        accountSelector.setAdapter(accountAdapter);
        accountSelector.setSelectedItem(mbwManager.getSelectedAccount());

        //display the loading spinner
        setLayout(ChangellyActivity.ChangellyUITypes.Loading);
        changellyAPIService.getCurrencies().enqueue(new Callback<ChangellyAPIService.ChangellyAnswerListString>() {
            @Override
            public void onResponse(Call<ChangellyAPIService.ChangellyAnswerListString> call, Response<ChangellyAPIService.ChangellyAnswerListString> response) {
                if (response.body() == null || response.body().result == null) {
                    toast("Can't load currencies.");
                    return;
                }
                Log.d(TAG, "currencies=" + response.body().result);
                Collections.sort(response.body().result);
                List<CurrencyAdapter.Item> itemList = new ArrayList<>();
                itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                String[] skipCurrencies = getResources().getStringArray(R.array.changelly_skip_currencies);
                for (String curr : response.body().result) {
                    if (!curr.equalsIgnoreCase("btc") &&
                            !containsCaseInsensitive(curr, skipCurrencies)) {
                        itemList.add(new CurrencyAdapter.Item(curr.toUpperCase(), CurrencyAdapter.VIEW_TYPE_ITEM));
                    }
                }
                itemList.add(new CurrencyAdapter.Item(null, CurrencyAdapter.VIEW_TYPE_PADDING));
                currencyAdapter.setItems(itemList);
                setLayout(ChangellyUITypes.Main);
            }

            @Override
            public void onFailure(Call<ChangellyAPIService.ChangellyAnswerListString> call, Throwable t) {
                toast("Can't load currencies: " + t);
            }
        });
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
        if (!avoidTextChangeEvent && isValueForOfferOk()) {
            requestOfferFunction(fromValue.getText().toString()
                    , currencyAdapter.getItem(currencySelector.getSelectedItem()).currency
                    , BTC);
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
                    , BTC
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
        String destination = walletAccount.getReceivingAddress().get().toString();
        if (walletAccount instanceof CoinapultAccount) {
            destination = walletAccount.getReceivingAddress().get().toString();
        } else {
            AbstractAccount account = (AbstractAccount) walletAccount;
            if (account.getReceivingAddress(AddressType.P2SH_P2WPKH) != null) {
                destination = account.getReceivingAddress(AddressType.P2SH_P2WPKH).toString();
            } else if (account.getReceivingAddress(AddressType.P2PKH) != null) {
                destination = account.getReceivingAddress(AddressType.P2PKH).toString();
            }
        }
        startActivityForResult(new Intent(ChangellyActivity.this, ChangellyOfferActivity.class)
                .putExtra(ChangellyAPIService.FROM, item.currency)
                .putExtra(ChangellyAPIService.TO, BTC)
                .putExtra(ChangellyAPIService.AMOUNT, dblAmount)
                .putExtra(ChangellyAPIService.DESTADDRESS, destination), REQUEST_OFFER);
    }

    boolean isValueForOfferOk() {
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

        if (minAmount == 0) {
            btTakeOffer.setEnabled(false);
            toast("Please wait while loading minimum amount information.");
            return false;
        } else if (dblAmount.compareTo(minAmount) < 0) {
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

    class GetMinCallback implements Callback<ChangellyAnswerDouble> {
        String from;

        GetMinCallback(String from) {
            this.from = from;
        }

        @Override
        public void onResponse(@NonNull Call<ChangellyAnswerDouble> call,
                               @NonNull Response<ChangellyAnswerDouble> response) {
            ChangellyAnswerDouble result = response.body();
            if(result == null || result.result == -1) {
                Log.e("MyceliumChangelly", "Minimum amount could not be retrieved");
                toast("Service unavailable");
                return;
            }
            double min = result.result;
            // service available
            CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
            if (item != null && from != null
                    && from.equalsIgnoreCase(item.currency)) {
                Log.d(TAG, "Received minimum amount: " + min + " " + from);
                minAmount = min;
                tvMinAmountValue.setText(getString(R.string.exchange_minimum_amount
                        , decimalFormat.format(minAmount), item.currency));
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyAnswerDouble> call,
                              @NonNull Throwable t) {
            toast("Service unavailable");
        }
    }

    class GetOfferCallback implements Callback<ChangellyAnswerDouble> {
        final String from;
        final String to;
        final double fromAmount;

        GetOfferCallback(@NonNull String from, @NonNull String to, double fromAmount) {
            this.from = from;
            this.to = to;
            this.fromAmount = fromAmount;
        }

        @Override
        public void onResponse(@NonNull Call<ChangellyAnswerDouble> call,
                               @NonNull Response<ChangellyAnswerDouble> response) {
            ChangellyAnswerDouble result = response.body();
            if(result != null) {
                double amount = result.result;
                Log.d("MyceliumChangelly", "You will receive the following " + to + " amount: " + result.result);
                CurrencyAdapter.Item item = currencyAdapter.getItem(currencySelector.getSelectedItem());
                // check if the user still needs this reply or navigated to different amounts/currencies
                if (item != null) {
                    Log.d(TAG, "Received offer: " + amount + " " + to);
                    avoidTextChangeEvent = true;
                    try {
                        if (to.equalsIgnoreCase(BTC)
                                && from.equalsIgnoreCase(item.currency)
                                && fromAmount == Double.parseDouble(fromValue.getText().toString())) {
                            toValue.setText(decimalFormat.format(amount));
                        } else if (from.equalsIgnoreCase(BTC)
                                && to.equalsIgnoreCase(item.currency)
                                && fromAmount == Double.parseDouble(toValue.getText().toString())) {
                            fromValue.setText(decimalFormat.format(amount));
                        }
                        isValueForOfferOk();
                    } catch (NumberFormatException ignore) {
                    }
                    avoidTextChangeEvent = false;
                }
            }
        }

        @Override
        public void onFailure(@NonNull Call<ChangellyAnswerDouble> call,
                              @NonNull Throwable t) {
            toast("Service unavailable " + t);
        }
    }
}
