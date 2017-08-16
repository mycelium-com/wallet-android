package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.mycelium.wallet.R;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.math.BigDecimal;

import butterknife.BindView;

/**
 * Created by elvis on 20.06.17.
 */

public class RmcEthAmountFragment extends RmcCommonAmountFragment {

    @BindView(R.id.etETH)
    protected EditText etETH;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_eth_amount, container, false);
    }

    @Override
    protected void initView() {
        super.initView();
        etCryptoWatcher = new InputWatcher(etETH, "ETH", 8);
    }

    @Override
    protected void fillBundle(Bundle bundle) {
        bundle.putSerializable(Keys.ETH_COUNT, new BigDecimal(etETH.getText().toString()));
        bundle.putString(Keys.PAY_METHOD, "ETH");
    }

    protected void addChangeListener() {
        super.addChangeListener();
        etETH.addTextChangedListener(etCryptoWatcher);
    }

    protected void removeChangeListener() {
        super.removeChangeListener();
        etETH.removeTextChangedListener(etCryptoWatcher);
    }

    @Override
    protected void updateCrypto(CurrencyValue currencyValue) {
        BigDecimal ethValue = CurrencyValue.fromValue(currencyValue, "ETH", exchangeRateManager).getValue();
        try {
            etETH.setText(ethValue.toPlainString());
        } catch (ArithmeticException e) {
            etETH.setText("");
        }
    }
}

