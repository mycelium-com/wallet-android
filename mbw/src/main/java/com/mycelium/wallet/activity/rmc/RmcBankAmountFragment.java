package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mycelium.wallet.R;

import java.math.BigDecimal;

/**
 * Created by elvis on 22.06.17.
 */

public class RmcBankAmountFragment extends Fragment {

    private EditText etUSD;
    private EditText etRMC;

    private InputWatcher etUSDWatcher;
    private InputWatcher etRMCWatcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_bank_amount, container, false);
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
                bundle.putString("pay_method", "BANK");
                rmcAccountFragment.setArguments(bundle);
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, rmcAccountFragment)
                        .commitAllowingStateLoss();
            }
        });

        etUSD = (EditText) view.findViewById(R.id.etUSD);
        etRMC = (EditText) view.findViewById(R.id.etRMC);

        etRMCWatcher = new InputWatcher(etRMC, "RMC");
        etUSDWatcher = new InputWatcher(etUSD, "USD");

        addChangeListener();
    }

    private void addChangeListener() {
        etRMC.addTextChangedListener(etRMCWatcher);
        etUSD.addTextChangedListener(etUSDWatcher);
    }

    private void removeChangeListener() {
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
        if (currency.equals("RMC")) {
            BigDecimal usdValue = value.multiply(BigDecimal.valueOf(4000));
            etUSD.setText(usdValue.toPlainString());
        } else if (currency.equals("USD")) {
            etRMC.setText(value.divide(BigDecimal.valueOf(4000)).toPlainString());
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