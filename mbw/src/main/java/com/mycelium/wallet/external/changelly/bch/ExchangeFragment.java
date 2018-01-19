package com.mycelium.wallet.external.changelly.bch;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.external.changelly.AccountAdapter;
import com.mycelium.wallet.external.changelly.ChangellyService;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;

public class ExchangeFragment extends Fragment {
    public static final BigDecimal MAX_BITCOIN_VALUE = BigDecimal.valueOf(20999999);
    private static String TAG = "ChangellyActivity";

    @BindView(R.id.from_account_list)
    SelectableRecyclerView fromRecyclerView;

    @BindView(R.id.to_account_list)
    SelectableRecyclerView toRecyclerView;

    @BindView(R.id.numeric_keyboard)
    ValueKeyboard valueKeyboard;

    @BindView(R.id.fromValue)
    TextView fromValue;

    @BindView(R.id.toValue)
    TextView toValue;

    @BindView(R.id.fromValueLayout)
    View fromLayout;

    @BindView(R.id.toValueLayout)
    View toLayout;

    @BindView(R.id.tvMinAmountValue)
    TextView tvMinAmountValue;

    @BindView(R.id.buttonContinue)
    Button buttonContinue;

    @BindView(R.id.exchange_rate)
    TextView exchangeRate;

    @BindView(R.id.exchange_fiat_rate)
    TextView exchangeFiatRate;

    private MbwManager mbwManager;
    private AccountAdapter toAccountAdapter;
    private AccountAdapter fromAccountAdapter;

    private Double minAmount = 0.0;
    private boolean avoidTextChangeEvent = false;
    private Receiver receiver;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mbwManager = MbwManager.getInstance(getActivity());
        setRetainInstance(true);
        receiver = new Receiver();
        for (String action : new String[]{
                ChangellyService.INFO_EXCH_AMOUNT,
                ChangellyService.INFO_MIN_AMOUNT,
                ChangellyService.INFO_ERROR}) {
            IntentFilter intentFilter = new IntentFilter(action);
            LocalBroadcastManager.getInstance(getActivity()).registerReceiver(receiver, intentFilter);
        }
        getActivity().startService(new Intent(getActivity(), ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_MIN_EXCHANGE)
                .putExtra(ChangellyService.FROM, ChangellyService.BCH)
                .putExtra(ChangellyService.TO, ChangellyService.BTC));
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchage, container, false);
        ButterKnife.bind(this, view);
        fromRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        toRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        int senderFinalWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int firstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;

        List<WalletAccount> toAccounts = new ArrayList<>();
        toAccounts.addAll(AccountManager.INSTANCE.getBTCBip44Accounts().values());
        toAccounts.addAll(AccountManager.INSTANCE.getBTCSingleAddressAccounts().values());
        toAccounts.addAll(AccountManager.INSTANCE.getCoinapultAccounts().values());
        toAccountAdapter = new AccountAdapter(mbwManager,toAccounts, firstItemWidth);
        toAccountAdapter.setAccountUseType(AccountAdapter.AccountUseType.IN);
        toRecyclerView.setAdapter(toAccountAdapter);

        List<WalletAccount> fromAccounts = new ArrayList<>();
        fromAccounts.addAll(filterAccount(AccountManager.INSTANCE.getBCHBip44Accounts().values()));
        fromAccounts.addAll(filterAccount(AccountManager.INSTANCE.getBCHSingleAddressAccounts().values()));
        if (fromAccounts.isEmpty()) {
            toast(getString(R.string.no_spendable_accounts));
            getActivity().finish();
        }
        fromAccountAdapter = new AccountAdapter(mbwManager, fromAccounts, firstItemWidth);
        fromAccountAdapter.setAccountUseType(AccountAdapter.AccountUseType.OUT);
        fromRecyclerView.setAdapter(fromAccountAdapter);

        valueKeyboard.setMaxDecimals(8);
        valueKeyboard.setInputListener(new ValueKeyboard.SimpleInputListener() {
            @Override
            public void done() {
                fromRecyclerView.setVisibility(View.VISIBLE);
                toRecyclerView.setVisibility(View.VISIBLE);
                fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
                toLayout.setAlpha(Constants.INACTIVE_ALPHA);
            }
        });
        fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);

        valueKeyboard.setVisibility(android.view.View.GONE);
        buttonContinue.setEnabled(false);
        return view;
    }

    private List<WalletAccount> filterAccount(Collection<WalletAccount> accounts) {
        List<WalletAccount> result = new ArrayList<>();
        for (WalletAccount walletAccount : accounts) {
            if (walletAccount.canSpend() && !walletAccount.getCurrencyBasedBalance().confirmed.isZero()) {
                result.add(walletAccount);
            }
        }
        return result;
    }

    @Override
    public void onResume() {
        super.onResume();
        mbwManager.getEventBus().register(this);
    }

    @Override
    public void onPause() {
        mbwManager.getEventBus().unregister(this);
        super.onPause();
    }

    @OnClick(R.id.buttonContinue)
    void continueClick() {
        String txtAmount = fromValue.getText().toString();
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            toast("Error exchanging value");
            buttonContinue.setEnabled(false);
            return;
        }
        Fragment fragment = new ConfirmExchangeFragment();
        Bundle bundle = new Bundle();
        bundle.putDouble(Constants.FROM_AMOUNT, dblAmount);
        WalletAccount toAccount = toAccountAdapter.getItem(toRecyclerView.getSelectedItem()).account;
        bundle.putSerializable(Constants.DESTADDRESS, toAccount.getId());
        WalletAccount fromAccount = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account;
        bundle.putSerializable(Constants.FROM_ADDRESS, fromAccount.getId());

        fragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, "ConfirmExchangeFragment")
                .addToBackStack("ConfirmExchangeFragment")
                .commitAllowingStateLoss();
    }

    @OnClick(R.id.toValueLayout)
    void toValueClick() {
        valueKeyboard.setInputTextView(toValue);
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setEntry(toValue.getText().toString());
        fromRecyclerView.setVisibility(View.GONE);
        toRecyclerView.setVisibility(View.GONE);
        toLayout.setAlpha(Constants.ACTIVE_ALPHA);
        fromLayout.setAlpha(Constants.INACTIVE_ALPHA);
        valueKeyboard.setSpendableValue(BigDecimal.ZERO);
        valueKeyboard.setMaxValue(MAX_BITCOIN_VALUE);
    }

    @OnClick(R.id.fromValueLayout)
    void fromValueClick() {
        valueKeyboard.setInputTextView(fromValue);
        valueKeyboard.setVisibility(View.VISIBLE);
        valueKeyboard.setEntry(fromValue.getText().toString());
        fromLayout.setAlpha(Constants.ACTIVE_ALPHA);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);
        AccountAdapter.Item item = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem());
        valueKeyboard.setSpendableValue(getMaxSpend(item.account));
        valueKeyboard.setMaxValue(MAX_BITCOIN_VALUE);
    }

    @OnClick(R.id.use_all_funds)
    void useAllFundsClick() {
        AccountAdapter.Item item = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem());
        fromValue.setText(getMaxSpend(item.account).toPlainString());
    }

    private BigDecimal getMaxSpend(WalletAccount account) {
        return account.getCurrencyBasedBalance().confirmed.getValue()
                .add(BigDecimal.valueOf(ConfirmExchangeFragment.MINER_FEE).movePointLeft(8).negate());
    }

    @OnTextChanged(value = R.id.fromValue, callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputFrom(Editable editable) {
        if (!avoidTextChangeEvent && isValueForOfferOk(true)) {
            BigDecimal val = new BigDecimal(fromValue.getText().toString());
            if (val.compareTo(MAX_BITCOIN_VALUE) > 0) {
                val = MAX_BITCOIN_VALUE;
                fromValue.setText(val.toPlainString());
            }
            requestOfferFunction(val.toPlainString()
                    , ChangellyService.BCH, ChangellyService.BTC);
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
            avoidTextChangeEvent = true;
            BigDecimal val = new BigDecimal(toValue.getText().toString());
            if (val.compareTo(MAX_BITCOIN_VALUE) > 0) {
                val = MAX_BITCOIN_VALUE;
                toValue.setText(val.toPlainString());
            }
            fromValue.setText(CurrencyValue.fromValue(ExactBitcoinValue.from(val)
                    , CurrencyValue.BCH, mbwManager.getExchangeRateManager()).getValue().toPlainString());
            avoidTextChangeEvent = false;
        }
        if (!avoidTextChangeEvent && toValue.getText().toString().isEmpty()) {
            avoidTextChangeEvent = true;
            fromValue.setText(null);
            avoidTextChangeEvent = false;
        }
    }

    private void requestOfferFunction(String amount, String fromCurrency, String toCurrency) {
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(amount);
        } catch (NumberFormatException e) {
            Toast.makeText(getActivity(), "Error parsing double values", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent changellyServiceIntent = new Intent(getActivity(), ChangellyService.class)
                .setAction(ChangellyService.ACTION_GET_EXCHANGE_AMOUNT)
                .putExtra(ChangellyService.FROM, fromCurrency)
                .putExtra(ChangellyService.TO, toCurrency)
                .putExtra(ChangellyService.AMOUNT, dblAmount);
        getActivity().startService(changellyServiceIntent);

    }

    boolean isValueForOfferOk(boolean checkMin) {
        tvMinAmountValue.setVisibility(View.GONE);
        String txtAmount = fromValue.getText().toString();
        if (txtAmount.isEmpty()) {
            buttonContinue.setEnabled(false);
            return false;
        }
        Double dblAmount;
        try {
            dblAmount = Double.parseDouble(txtAmount);
        } catch (NumberFormatException e) {
            toast("Error exchanging value");
            buttonContinue.setEnabled(false);
            return false;
        }

        if (checkMin && minAmount == 0) {
            buttonContinue.setEnabled(false);
            toast("Please wait while loading minimum amount information.");
            return false;
        } else if (checkMin && dblAmount.compareTo(minAmount) < 0) {
            buttonContinue.setEnabled(false);
            tvMinAmountValue.setVisibility(View.VISIBLE);
            return false;
        } // TODO: compare with maximum
        buttonContinue.setEnabled(true);
        return true;
    }

    private void updateUi() {
        try {
            exchangeFiatRate.setText(Utils.formatFiatWithUnit(
                    mbwManager.getCurrencySwitcher().getAsFiatValue(
                            ExactBitcoinValue.from(new BigDecimal(toValue.getText().toString())))));
        } catch (NumberFormatException ignore) {
        }
        exchangeFiatRate.setVisibility(View.VISIBLE);

        exchangeRate.setText("1 BCH ~ " + CurrencyValue.fromValue(ExactBitcoinCashValue.ONE, "BTC", mbwManager.getExchangeRateManager()));
        exchangeRate.setVisibility(View.VISIBLE);
    }

    private void toast(String msg) {
        Toast.makeText(getActivity(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(receiver);
        super.onDestroy();
    }

    class Receiver extends BroadcastReceiver {
        private Receiver() {
        }  // prevents instantiation

        @Override
        public void onReceive(Context context, Intent intent) {
            String from, to;
            double amount;

            switch (intent.getAction()) {
                case ChangellyService.INFO_MIN_AMOUNT:
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);
                    Log.d(TAG, "Received minimum amount: " + amount);
                    minAmount = amount;
                    tvMinAmountValue.setText(getString(R.string.exchange_minimum_amount
                            , decimalFormat.format(minAmount), "BCH"));
                    break;
                case ChangellyService.INFO_EXCH_AMOUNT:
                    from = intent.getStringExtra(ChangellyService.FROM);
                    to = intent.getStringExtra(ChangellyService.TO);
                    double fromAmount = intent.getDoubleExtra(ChangellyService.FROM_AMOUNT, 0);
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, 0);

                    if (from != null && to != null) {
                        Log.d(TAG, "Received offer: " + amount + " " + to);
                        avoidTextChangeEvent = true;
                        try {

                            if (to.equalsIgnoreCase(ChangellyService.BTC)
                                    && from.equalsIgnoreCase(ChangellyService.BCH)
                                    && fromAmount == Double.parseDouble(fromValue.getText().toString())) {
                                toValue.setText(decimalFormat.format(amount));
                            } else if (from.equalsIgnoreCase(ChangellyService.BTC)
                                    && to.equalsIgnoreCase(ChangellyService.BCH)
                                    && fromAmount == Double.parseDouble(toValue.getText().toString())) {
                                fromValue.setText(decimalFormat.format(amount));
                            }

                            isValueForOfferOk(true);

                        } catch (NumberFormatException ignore) {
                        }
                        avoidTextChangeEvent = false;
                        updateUi();
                    }
                    break;
                case INFO_ERROR:
                    Toast.makeText(getActivity(), "Service unavailable", Toast.LENGTH_LONG).show();
                    break;
            }
        }
    }

    @Subscribe
    public void exchangeRatesRefreshed(ExchangeRatesRefreshed event){
        updateUi();
    }
}
