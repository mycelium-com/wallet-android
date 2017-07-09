package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.currency.ExactFiatValue;

import java.math.BigDecimal;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by elvis on 20.06.17.
 */

public class RmcBtcAmountFragment extends Fragment {

    @BindView(R.id.btOk)
    protected View btnOk;

    private EditText etBTC;
    private EditText etUSD;
    private EditText etRMC;

    private InputWatcher etBTCWatcher;
    private InputWatcher etUSDWatcher;
    private InputWatcher etRMCWatcher;
    private MbwManager mbwManager;
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mbwManager = MbwManager.getInstance(getActivity());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_btc_amount, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        etBTC = (EditText) view.findViewById(R.id.etBTC);
        etUSD = (EditText) view.findViewById(R.id.etUSD);
        etRMC = (EditText) view.findViewById(R.id.etRMC);

        etBTCWatcher = new InputWatcher(etBTC, "BTC");
        etRMCWatcher = new InputWatcher(etRMC, "RMC");
        etUSDWatcher = new InputWatcher(etUSD, "USD");
        addChangeListener();
    }

    @OnClick(R.id.btOk)
    void okClick() {
        ChooseRMCAccountFragment rmcAccountFragment = new ChooseRMCAccountFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Keys.RMC_COUNT, etRMC.getText().toString());
        bundle.putString(Keys.PAY_METHOD, "BTC");
        bundle.putString(Keys.BTC_COUNT, etBTC.getText().toString());
        rmcAccountFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, rmcAccountFragment)
                .commitAllowingStateLoss();
    }

    private void addChangeListener() {
        etBTC.addTextChangedListener(etBTCWatcher);
        etRMC.addTextChangedListener(etRMCWatcher);
        etUSD.addTextChangedListener(etUSDWatcher);
    }

    private void removeChangeListener() {
        etBTC.removeTextChangedListener(etBTCWatcher);
        etRMC.removeTextChangedListener(etRMCWatcher);
        etUSD.removeTextChangedListener(etUSDWatcher);
    }

    public void update(String amount, String currency) {
        removeChangeListener();

        BigDecimal value = BigDecimal.ZERO;
        try {
            value = new BigDecimal(amount);
        } catch (NumberFormatException e) {
        }
        if (currency.equals("BTC")) {
            BigDecimal usdValue = !value.equals(BigDecimal.ZERO) ?
                    CurrencyValue.fromValue(ExactBitcoinValue.from(value), "USD", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue()
                    : BigDecimal.ZERO;
            usdValue = usdValue == null ? BigDecimal.ZERO : usdValue;
            etUSD.setText(usdValue.stripTrailingZeros().toPlainString());
            etRMC.setText(usdValue.divide(BigDecimal.valueOf(4000)).stripTrailingZeros().toPlainString());
        } else if (currency.equals("RMC")) {
            BigDecimal usdValue = value.multiply(BigDecimal.valueOf(4000));
            etUSD.setText(usdValue.stripTrailingZeros().toPlainString());
            BigDecimal btcValue = CurrencyValue.fromValue(ExactFiatValue.from(usdValue, "USD"), "BTC", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue();
            etBTC.setText(btcValue != null ? btcValue.stripTrailingZeros().toPlainString()
                    : getString(R.string.exchange_source_not_available, mbwManager.getExchangeRateManager().getCurrentExchangeSourceName()));
        } else if (currency.equals("USD")) {
            etRMC.setText(value.divide(BigDecimal.valueOf(4000)).stripTrailingZeros().toPlainString());
            BigDecimal btcValue = CurrencyValue.fromValue(ExactFiatValue.from(value, "USD"), "BTC", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue();
            etBTC.setText(btcValue != null ? btcValue.stripTrailingZeros().toPlainString()
                    : getString(R.string.exchange_source_not_available, mbwManager.getExchangeRateManager().getCurrentExchangeSourceName()));
        }
        btnOk.setEnabled(!value.equals(BigDecimal.ZERO));

        addChangeListener();
    }

    private class InputWatcher implements TextWatcher {
        EditText et;
        String currency;

        public InputWatcher(EditText et, String currency) {
            this.et = et;
            this.currency = currency;
        }

        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {
            update(et.getText().toString(), currency);
        }
    }
}
