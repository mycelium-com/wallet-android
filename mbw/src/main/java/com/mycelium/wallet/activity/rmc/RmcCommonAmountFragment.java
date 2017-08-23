package com.mycelium.wallet.activity.rmc;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactCurrencyValue;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by elvis on 16.08.17.
 */

public abstract class RmcCommonAmountFragment extends Fragment {
    @BindView(R.id.etUSD)
    protected EditText etUSD;
    @BindView(R.id.etRMC)
    protected EditText etRMC;
    @BindView(R.id.btOk)
    protected View btnOk;

    protected InputWatcher etCryptoWatcher;
    protected InputWatcher etUSDWatcher;
    protected InputWatcher etRMCWatcher;

    protected MbwManager mbwManager;
    protected ExchangeRateManager exchangeRateManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mbwManager = MbwManager.getInstance(getActivity());
        exchangeRateManager = MbwManager.getInstance(getActivity()).getExchangeRateManager();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        initView();
        btnOk.setEnabled(false);
        addChangeListener();
    }

    protected void initView() {
        etRMCWatcher = new InputWatcher(etRMC, "RMC", 4);
        etUSDWatcher = new InputWatcher(etUSD, "USD", 2);
    }

    @OnClick(R.id.btOk)
    void okClick() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
        View view = getActivity().getCurrentFocus();
        if (view != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        ChooseRMCAccountFragment rmcAccountFragment = new ChooseRMCAccountFragment();
        Bundle bundle = new Bundle();
        bundle.putSerializable(Keys.RMC_COUNT, new BigDecimal(etRMC.getText().toString()));
        fillBundle(bundle);
        rmcAccountFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, rmcAccountFragment)
                .commitAllowingStateLoss();
    }

    protected abstract void fillBundle(Bundle bundle);

    protected void update(CurrencyValue currencyValue) {
        BigDecimal rmcValue = CurrencyValue.fromValue(currencyValue, "RMC", exchangeRateManager).getValue();
        BigDecimal usdValue = CurrencyValue.fromValue(currencyValue, "USD", exchangeRateManager).getValue();

        switch (currencyValue.getCurrency()) {
            case "RMC":
                etUSD.setText(usdValue.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString());
                updateCrypto(currencyValue);
                break;
            case "USD":
                etRMC.setText(rmcValue.setScale(4, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString());
                updateCrypto(currencyValue);
                break;
            default:
                etUSD.setText(usdValue.setScale(2, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString());
                etRMC.setText(rmcValue.setScale(4, BigDecimal.ROUND_HALF_UP).stripTrailingZeros().toPlainString());

        }
        btnOk.setEnabled(rmcValue.compareTo(new BigDecimal("0.1")) > -1 || BuildConfig.DEBUG);
    }

    protected abstract void updateCrypto(CurrencyValue currencyValue);

    private int decimalsAfterDot(String _entry) {
        int dotIndex = _entry.indexOf('.');
        if (dotIndex == -1) {
            return 0;
        }
        return _entry.length() - dotIndex - 1;
    }

    protected void addChangeListener() {
        etRMC.addTextChangedListener(etRMCWatcher);
        etUSD.addTextChangedListener(etUSDWatcher);
    }

    protected void removeChangeListener() {
        etRMC.removeTextChangedListener(etRMCWatcher);
        etUSD.removeTextChangedListener(etUSDWatcher);
    }

    public class InputWatcher implements TextWatcher {
        EditText et;
        String currency;
        int derivation;

        public InputWatcher(EditText et, String currency, int derivation) {
            this.et = et;
            this.currency = currency;
            this.derivation = derivation;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            removeChangeListener();
            String amount = et.getText().toString();
            if (decimalsAfterDot(amount) > derivation) {
                amount = amount.substring(0, amount.length() - 1);
                et.setText(amount);
                et.setSelection(amount.length());

            }
            BigDecimal value = BigDecimal.ZERO;
            try {
                value = new BigDecimal(et.getText().toString());
            } catch (Exception ignore) {
            }
            update(ExactCurrencyValue.from(value, currency));
            addChangeListener();
        }
    }
}
