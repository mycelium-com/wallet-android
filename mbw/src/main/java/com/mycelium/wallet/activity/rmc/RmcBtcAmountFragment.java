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

public class RmcBtcAmountFragment extends RmcCommonAmountFragment {

    @BindView(R.id.etBTC)
    protected EditText etBTC;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_btc_amount, container, false);
    }

    @Override
    protected void initView() {
        super.initView();
        etCryptoWatcher = new InputWatcher(etBTC, "BTC", 8);
    }

    @Override
    protected void fillBundle(Bundle bundle) {
        bundle.putString(Keys.PAY_METHOD, "BTC");
        bundle.putSerializable(Keys.BTC_COUNT, new BigDecimal(etBTC.getText().toString()));
    }

    protected void addChangeListener() {
        super.addChangeListener();
        etBTC.addTextChangedListener(etCryptoWatcher);
    }

    protected void removeChangeListener() {
        super.removeChangeListener();
        etBTC.removeTextChangedListener(etCryptoWatcher);
    }

    @Override
    protected void updateCrypto(CurrencyValue currencyValue) {
        BigDecimal btcValue = CurrencyValue.fromValue(currencyValue, "BTC", exchangeRateManager).getValue();
        etBTC.setText(btcValue != null ? btcValue.stripTrailingZeros().toPlainString()
                : getString(R.string.exchange_source_not_available, mbwManager.getExchangeRateManager().getCurrentExchangeSourceName()));
    }
}
