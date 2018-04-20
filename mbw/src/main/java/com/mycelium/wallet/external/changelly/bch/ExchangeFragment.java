package com.mycelium.wallet.external.changelly.bch;


import android.app.AlertDialog;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.megiontechnologies.BitcoinCash;
import com.mycelium.wallet.AccountManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.send.event.SelectListener;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wallet.event.ExchangeRatesRefreshed;
import com.mycelium.wallet.external.changelly.AccountAdapter;
import com.mycelium.wallet.external.changelly.ChangellyService;
import com.mycelium.wallet.external.changelly.Constants;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.squareup.otto.Subscribe;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static com.mycelium.wallet.external.changelly.ChangellyService.INFO_ERROR;
import static com.mycelium.wallet.external.changelly.Constants.decimalFormat;

public class ExchangeFragment extends Fragment {
    public static final BigDecimal MAX_BITCOIN_VALUE = BigDecimal.valueOf(20999999);
    public static final String BCH_EXCHANGE = "bch_exchange";
    public static final String BCH_MIN_EXCHANGE_VALUE = "bch_min_exchange_value";
    public static final float NOT_LOADED = -1f;
    public static final String TO_ACCOUNT = "toAccount";
    public static final String FROM_ACCOUNT = "fromAccount";
    public static final String FROM_VALUE = "fromValue";
    public static final String ABOUT = "≈ ";
    private static String TAG = "ChangellyActivity";

    @BindView(R.id.scrollView)
    ScrollView scrollView;

    @BindView(R.id.from_account_list)
    SelectableRecyclerView fromRecyclerView;

    @BindView(R.id.to_account_list)
    SelectableRecyclerView toRecyclerView;

    @BindView(R.id.numeric_keyboard)
    ValueKeyboard valueKeyboard;

    @BindView(R.id.fromValue)
    TextView fromValue;

    @BindView(R.id.bchLabel)
    View bchLabel;

    @BindView(R.id.toValue)
    TextView toValue;

    @BindView(R.id.fromValueLayout)
    View fromLayout;

    @BindView(R.id.toValueLayout)
    View toLayout;

    @BindView(R.id.tvError)
    TextView tvError;

    @BindView(R.id.buttonContinue)
    Button buttonContinue;

    @BindView(R.id.exchange_rate)
    TextView exchangeRate;

    @BindView(R.id.exchange_fiat_rate)
    TextView exchangeFiatRate;

    @BindView(R.id.exchange_fiat_rate_from)
    TextView exchangeFiatRateFrom;

    @BindView(R.id.use_all_funds)
    View useAllFunds;

    private MbwManager mbwManager;
    private AccountAdapter toAccountAdapter;
    private AccountAdapter fromAccountAdapter;

    private double minAmount = NOT_LOADED;
    private boolean avoidTextChangeEvent = false;
    private Receiver receiver;
    private SharedPreferences sharedPreferences;

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
        sharedPreferences = getActivity().getSharedPreferences(BCH_EXCHANGE, Context.MODE_PRIVATE);
        minAmount = (double) sharedPreferences.getFloat(BCH_MIN_EXCHANGE_VALUE, NOT_LOADED);
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
        toAccountAdapter = new AccountAdapter(mbwManager, toAccounts, firstItemWidth);
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
        fromRecyclerView.setSelectedItem(mbwManager.getSelectedAccount());
        fromRecyclerView.setSelectListener(new SelectListener() {
            @Override
            public void onSelect(RecyclerView.Adapter adapter, int position) {
                WalletAccount fromAccount = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account;
                valueKeyboard.setSpendableValue(getMaxSpend(fromAccount));
                isValueForOfferOk(true);
            }
        });

        valueKeyboard.setMaxDecimals(8);
        valueKeyboard.setInputListener(new ValueKeyboard.SimpleInputListener() {
            @Override
            public void done() {
                setAlphaFromLayout(Constants.INACTIVE_ALPHA);
                toLayout.setAlpha(Constants.INACTIVE_ALPHA);
                stopCursor(fromValue);
                stopCursor(toValue);
                useAllFunds.setVisibility(View.VISIBLE);
            }
        });
        setAlphaFromLayout(Constants.INACTIVE_ALPHA);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);

        valueKeyboard.setVisibility(View.GONE);
        buttonContinue.setEnabled(false);
        return view;
    }

    private void startCursor(final TextView textView) {
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.input_cursor, 0);
        textView.post(new Runnable() {
            @Override
            public void run() {
                AnimationDrawable animationDrawable = (AnimationDrawable) textView.getCompoundDrawables()[2];
                if (!animationDrawable.isRunning()) {
                    animationDrawable.start();
                }
            }
        });
    }

    private void stopCursor(final TextView textView) {
        textView.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
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
                .add(R.id.fragment_container, fragment, "ConfirmExchangeFragment")
                .addToBackStack("ConfirmExchangeFragment")
                .commitAllowingStateLoss();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(FROM_VALUE, fromValue.getText().toString());
        outState.putSerializable(FROM_ACCOUNT, fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account.getId());
        outState.putSerializable(TO_ACCOUNT, toAccountAdapter.getItem(toRecyclerView.getSelectedItem()).account.getId());
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            fromValue.setText(savedInstanceState.getString(FROM_VALUE));
            fromRecyclerView.setSelectedItem(mbwManager.getWalletManager(false)
                    .getAccount((UUID) savedInstanceState.getSerializable(FROM_ACCOUNT)));
            toRecyclerView.setSelectedItem(mbwManager.getWalletManager(false)
                    .getAccount((UUID) savedInstanceState.getSerializable(TO_ACCOUNT)));
            requestOfferFunction(getFromExcludeFee().toPlainString(), ChangellyService.BCH, ChangellyService.BTC);
        }
    }

    @OnClick(R.id.toValueLayout)
    void toValueClick() {
        valueKeyboard.setInputTextView(toValue);
        valueKeyboard.setVisibility(View.VISIBLE);
        useAllFunds.setVisibility(View.GONE);
        valueKeyboard.setEntry(toValue.getText().toString());
        toLayout.setAlpha(Constants.ACTIVE_ALPHA);
        startCursor(toValue);
        stopCursor(fromValue);
        setAlphaFromLayout(Constants.INACTIVE_ALPHA);
        valueKeyboard.setSpendableValue(BigDecimal.ZERO);
        valueKeyboard.setMaxValue(MAX_BITCOIN_VALUE);

        scrollTo(toLayout.getBottom());
    }

    private void scrollTo(final int to) {
        scrollView.post(new Runnable() {
            @Override
            public void run() {
                scrollView.smoothScrollTo(0, to);
            }
        });
    }

    @OnClick(R.id.fromValueLayout)
    void fromValueClick() {
        valueKeyboard.setInputTextView(fromValue);
        valueKeyboard.setVisibility(View.VISIBLE);
        useAllFunds.setVisibility(View.GONE);
        valueKeyboard.setEntry(fromValue.getText().toString());
        setAlphaFromLayout(Constants.ACTIVE_ALPHA);
        startCursor(fromValue);
        stopCursor(toValue);
        toLayout.setAlpha(Constants.INACTIVE_ALPHA);
        AccountAdapter.Item item = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem());
        valueKeyboard.setSpendableValue(getMaxSpend(item.account));
        valueKeyboard.setMaxValue(MAX_BITCOIN_VALUE);

        scrollTo(fromLayout.getTop());
    }

    private void setAlphaFromLayout(float alpha) {
        fromValue.setAlpha(alpha);
        bchLabel.setAlpha(alpha);
    }

    @OnClick(R.id.use_all_funds)
    void useAllFundsClick() {
        AccountAdapter.Item item = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem());
        fromValue.setText(getMaxSpend(item.account).stripTrailingZeros().toPlainString());
    }

    //TODO call getMaxFundsTransferrable need refactoring, we should call account object
    private BigDecimal getMaxSpend(WalletAccount account) {
        if (account.getType() == WalletAccount.Type.BCHBIP44) {
            int accountIndex = ((Bip44Account) account).getAccountIndex();
            return ExactBitcoinCashValue.from(mbwManager.getSpvBchFetcher().getMaxFundsTransferrable(accountIndex)).getValue();
        } else if (account.getType() == WalletAccount.Type.BCHSINGLEADDRESS) {
            String accountGuid = account.getId().toString();
            return ExactBitcoinCashValue.from(mbwManager.getSpvBchFetcher().getMaxFundsTransferrableSingleAddress(accountGuid)).getValue();
        }

        return BigDecimal.valueOf(0);
    }


    @OnTextChanged(value = R.id.fromValue, callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputFrom(Editable editable) {
        isValueForOfferOk(true);
        if (!avoidTextChangeEvent && !fromValue.getText().toString().isEmpty()) {
            try {
                requestOfferFunction(getFromExcludeFee().toPlainString(), ChangellyService.BCH, ChangellyService.BTC);
            } catch (IllegalArgumentException e) {
                Log.e(TAG, e.getMessage(), e);
            }
        }
        if (!avoidTextChangeEvent && fromValue.getText().toString().isEmpty()) {
            avoidTextChangeEvent = true;
            toValue.setText(null);
            avoidTextChangeEvent = false;
        }
    }

    private BigDecimal getFromExcludeFee() {
        BigDecimal val = new BigDecimal(fromValue.getText().toString());
        if (val.compareTo(MAX_BITCOIN_VALUE) > 0) {
            val = MAX_BITCOIN_VALUE;
            fromValue.setText(val.toPlainString());
        }
        BigDecimal txFee = UtilsKt.estimateFeeFromTransferrableAmount(
                fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account,
                mbwManager, BitcoinCash.nearestValue(val).getLongValue());
        return val.add(txFee.negate());
    }

    @OnTextChanged(value = R.id.toValue, callback = AFTER_TEXT_CHANGED)
    public void afterEditTextInputTo(Editable editable) {
        if (!avoidTextChangeEvent && !toValue.getText().toString().isEmpty()) {
            BigDecimal val = new BigDecimal(toValue.getText().toString());
            if (val.compareTo(MAX_BITCOIN_VALUE) > 0) {
                val = MAX_BITCOIN_VALUE;
                toValue.setText(val.toPlainString());
            }
            requestOfferFunction(val.toPlainString()
                    , ChangellyService.BTC, ChangellyService.BCH);
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
        tvError.setVisibility(View.GONE);
        exchangeFiatRateFrom.setVisibility(View.VISIBLE);
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

        WalletAccount fromAccount = fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account;
        if (checkMin && minAmount == NOT_LOADED) {
            buttonContinue.setEnabled(false);
            toast("Please wait while loading minimum amount information.");
            return false;
        } else if (checkMin && dblAmount.compareTo(minAmount) < 0) {
            buttonContinue.setEnabled(false);
            if (dblAmount != 0) {
                tvError.setText(getString(R.string.exchange_minimum_amount
                        , decimalFormat.format(minAmount), "BCH"));
                tvError.setVisibility(View.VISIBLE);
                exchangeFiatRateFrom.setVisibility(View.INVISIBLE);
            }
            scrollTo(tvError.getTop());
            return false;
        } else if (fromAccount.getCurrencyBasedBalance().confirmed.getValue().compareTo(BigDecimal.valueOf(dblAmount)) < 0) {
            buttonContinue.setEnabled(false);
            tvError.setText(R.string.balance_error);
            tvError.setVisibility(View.VISIBLE);
            exchangeFiatRateFrom.setVisibility(View.INVISIBLE);
            return false;
        }
        buttonContinue.setEnabled(true);
        return true;
    }

    private void updateUi() {
        CurrencyValue currencyBTCValue = null;
        try {
            currencyBTCValue = mbwManager.getCurrencySwitcher().getAsFiatValue(
                    ExactBitcoinValue.from(new BigDecimal(toValue.getText().toString())));
        } catch (NumberFormatException ignore) {
            exchangeFiatRate.setVisibility(View.INVISIBLE);
        }
        if (currencyBTCValue != null && currencyBTCValue.getValue() != null) {
            exchangeFiatRate.setText(ABOUT + Utils.formatFiatWithUnit(currencyBTCValue));
            exchangeFiatRate.setVisibility(View.VISIBLE);
        } else {
            exchangeFiatRate.setVisibility(View.INVISIBLE);
        }


        CurrencyValue currencyBCHValue = null;
        try {
            currencyBCHValue = mbwManager.getCurrencySwitcher().getAsFiatValue(
                    ExactBitcoinCashValue.from(new BigDecimal(fromValue.getText().toString())));
        } catch (NumberFormatException ignore) {
        }
        if (currencyBCHValue != null && currencyBCHValue.getValue() != null) {
            exchangeFiatRateFrom.setText(ABOUT + Utils.formatFiatWithUnit(currencyBCHValue));
            exchangeFiatRateFrom.setVisibility(View.VISIBLE);
        } else {
            exchangeFiatRateFrom.setVisibility(View.INVISIBLE);
        }
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
                    amount = intent.getDoubleExtra(ChangellyService.AMOUNT, NOT_LOADED);
                    if (amount != NOT_LOADED) {
                        Log.d(TAG, "Received minimum amount: " + amount);
                        sharedPreferences.edit()
                                .putFloat(BCH_MIN_EXCHANGE_VALUE, (float) amount)
                                .apply();
                        minAmount = amount;
                    }
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
                                    && fromAmount == getFromExcludeFee().doubleValue()) {
                                toValue.setText(decimalFormat.format(amount));
                                if (fromAmount != 0 && amount != 0) {
                                    exchangeRate.setText("1 BCH ~ " + decimalFormat.format(amount / fromAmount) + " BTC");
                                    exchangeRate.setVisibility(View.VISIBLE);
                                }
                            } else if (from.equalsIgnoreCase(ChangellyService.BTC)
                                    && to.equalsIgnoreCase(ChangellyService.BCH)
                                    && fromAmount == Double.parseDouble(toValue.getText().toString())) {
                                BigDecimal txFee = UtilsKt.estimateFeeFromTransferrableAmount(
                                        fromAccountAdapter.getItem(fromRecyclerView.getSelectedItem()).account,
                                        mbwManager, BitcoinCash.valueOf(amount).getLongValue());
                                fromValue.setText(decimalFormat.format(amount + txFee.doubleValue()));
                                if (fromAmount != 0 && amount != 0) {
                                    exchangeRate.setText("1 BCH ~ " + decimalFormat.format(fromAmount / amount) + " BTC");
                                    exchangeRate.setVisibility(View.VISIBLE);
                                }
                            }

                            isValueForOfferOk(true);

                        } catch (NumberFormatException ignore) {
                        }
                        avoidTextChangeEvent = false;
                        updateUi();
                    }
                    break;
                case INFO_ERROR:
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.exchange_rate_unavailable_msg))
                            .setNegativeButton(R.string.button_cancel, null)
                            .setPositiveButton(R.string.try_again, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    BigDecimal val = new BigDecimal(fromValue.getText().toString());
                                    requestOfferFunction(val.toPlainString()
                                            , ChangellyService.BCH, ChangellyService.BTC);

                                }
                            }).show();
                    break;
            }
        }
    }

    @Subscribe
    public void exchangeRatesRefreshed(ExchangeRatesRefreshed event) {
        updateUi();
    }
}
