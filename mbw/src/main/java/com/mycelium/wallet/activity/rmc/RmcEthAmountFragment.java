package com.mycelium.wallet.activity.rmc;

import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.rmc.model.EthRate;
import com.mycelium.wallet.api.retrofit.JacksonConverter;
import com.mycelium.wallet.external.NullBodyAwareOkClient;
import com.squareup.okhttp.OkHttpClient;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.RestAdapter;
import retrofit.http.GET;

/**
 * Created by elvis on 20.06.17.
 */

public class RmcEthAmountFragment extends Fragment {

    @BindView(R.id.btOk)
    protected View btnOk;

    private EditText etETH;
    private EditText etUSD;
    private EditText etRMC;

    private InputWatcher etETHWatcher;
    private InputWatcher etUSDWatcher;
    private InputWatcher etRMCWatcher;

    public double ethRate;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... voids) {
                ethRate = create().ethRate().rate.rate;
                return null;
            }
        }.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_eth_amount, container, false);
    }

    @OnClick(R.id.btOk)
    void okClick() {
        ChooseRMCAccountFragment rmcAccountFragment = new ChooseRMCAccountFragment();
        Bundle bundle = new Bundle();
        bundle.putString(Keys.ETH_COUNT, etETH.getText().toString());
        bundle.putString(Keys.RMC_COUNT, etRMC.getText().toString());
        bundle.putString(Keys.PAY_METHOD, "ETH");
        rmcAccountFragment.setArguments(bundle);
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, rmcAccountFragment)
                .commitAllowingStateLoss();
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        etETH = (EditText) view.findViewById(R.id.etETH);
        etUSD = (EditText) view.findViewById(R.id.etUSD);
        etRMC = (EditText) view.findViewById(R.id.etRMC);

        etETHWatcher = new InputWatcher(etETH, "ETH");
        etRMCWatcher = new InputWatcher(etRMC, "RMC");
        etUSDWatcher = new InputWatcher(etUSD, "USD");

        btnOk.setEnabled(false);
        addChangeListener();
    }

    private void addChangeListener() {
        etETH.addTextChangedListener(etETHWatcher);
        etRMC.addTextChangedListener(etRMCWatcher);
        etUSD.addTextChangedListener(etUSDWatcher);
    }

    private void removeChangeListener() {
        etETH.removeTextChangedListener(etETHWatcher);
        etRMC.removeTextChangedListener(etRMCWatcher);
        etUSD.removeTextChangedListener(etUSDWatcher);
    }

    public void update(String amount, String currency) {
        removeChangeListener();
        BigDecimal value = BigDecimal.ZERO;
        try {
            value = new BigDecimal(amount);
        } catch (NumberFormatException ignored) {
        }
        BigDecimal rmcValue = BigDecimal.ZERO;

        if (currency.equals("ETH")) {
            BigDecimal usdValue = value.multiply(BigDecimal.valueOf(ethRate));
            usdValue = usdValue == null ? BigDecimal.ZERO : usdValue;
            etUSD.setText(usdValue.toPlainString());
            rmcValue = usdValue.divide(BigDecimal.valueOf(4000));
            etRMC.setText(rmcValue.toPlainString());
        } else if (currency.equals("RMC")) {
            rmcValue = value;
            BigDecimal usdValue = rmcValue.multiply(BigDecimal.valueOf(4000));
            etUSD.setText(usdValue.toPlainString());
            try {
                BigDecimal ethValue = usdValue.divide(BigDecimal.valueOf(ethRate), MathContext.DECIMAL32);
                etETH.setText(ethValue.toPlainString());
            }catch (ArithmeticException e) {
                etETH.setText("");
            }
        } else if (currency.equals("USD")) {
            rmcValue = value.divide(BigDecimal.valueOf(4000));
            etRMC.setText(rmcValue.toPlainString());
            try {
                BigDecimal ethValue = value.divide(BigDecimal.valueOf(ethRate), MathContext.DECIMAL32);
                etETH.setText(ethValue.toPlainString());
            }catch (ArithmeticException e) {
                etETH.setText("");
            }
        }

        btnOk.setEnabled(rmcValue.compareTo(new BigDecimal("0.1")) > -1);

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

    EthService create() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false);

        OkHttpClient client = new OkHttpClient();
        client.setConnectTimeout(15000, TimeUnit.MILLISECONDS);
        client.setReadTimeout(15000, TimeUnit.MILLISECONDS);

        RestAdapter retrofit = new RestAdapter.Builder()
                .setEndpoint("https://coinmarketcap-nexuist.rhcloud.com/")
                .setLogLevel(RestAdapter.LogLevel.FULL)
//                .setLogLevel(RestAdapter.LogLevel.BASIC)
                .setConverter(new JacksonConverter(objectMapper))
                .setClient(new NullBodyAwareOkClient(client))
                .build();

        return retrofit.create(EthService.class);
    }
    interface EthService {
        @GET("/api/eth")
        EthRate ethRate();
    }
}

