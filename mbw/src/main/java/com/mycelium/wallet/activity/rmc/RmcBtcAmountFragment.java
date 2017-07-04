package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
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

/**
 * Created by elvis on 20.06.17.
 */

public class RmcBtcAmountFragment extends Fragment {

    private EditText etBTC;
    private EditText etUSD;
    private EditText etRMC;

    private InputWatcher etBTCWatcher;
    private InputWatcher etUSDWatcher;
    private InputWatcher etRMCWatcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_btc_amount, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btOk).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ChooseRMCAccountFragment rmcAccountFragment = new ChooseRMCAccountFragment();
                Bundle bundle = new Bundle();
                bundle.putString("rmc_count", etRMC.getText().toString());
                bundle.putString("pay_method", "BTC");
                bundle.putString("btc_count", etBTC.getText().toString());
                rmcAccountFragment.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, rmcAccountFragment)
                        .commitAllowingStateLoss();
            }
        });

        etBTC = (EditText) view.findViewById(R.id.etBTC);
        etUSD = (EditText) view.findViewById(R.id.etUSD);
        etRMC = (EditText) view.findViewById(R.id.etRMC);

        etBTCWatcher = new InputWatcher(etBTC, "BTC");
        etRMCWatcher = new InputWatcher(etRMC, "RMC");
        etUSDWatcher = new InputWatcher(etUSD, "USD");

        addChangeListener();
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
            BigDecimal usdValue = CurrencyValue.fromValue(ExactBitcoinValue.from(value), "USD", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue();
            usdValue = usdValue == null ? BigDecimal.ZERO : usdValue;
            etUSD.setText(usdValue.toPlainString());
            etRMC.setText(usdValue.divide(BigDecimal.valueOf(4000)).toPlainString());
        } else if (currency.equals("RMC")) {
            BigDecimal usdValue = value.multiply(BigDecimal.valueOf(4000));
            etUSD.setText(usdValue.toPlainString());
            BigDecimal btcValue = CurrencyValue.fromValue(ExactFiatValue.from(usdValue, "USD"), "BTC", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue();
            etBTC.setText(btcValue.toPlainString());
        } else if (currency.equals("USD")) {
            etRMC.setText(value.divide(BigDecimal.valueOf(4000)).toPlainString());
            BigDecimal btcValue = CurrencyValue.fromValue(ExactFiatValue.from(value, "USD"), "BTC", MbwManager.getInstance(getActivity()).getExchangeRateManager()).getValue();
            etBTC.setText(btcValue.toPlainString());
        }
        addChangeListener();
    }

    class InputWatcher implements TextWatcher {
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
