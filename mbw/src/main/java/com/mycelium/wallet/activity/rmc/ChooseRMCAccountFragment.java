package com.mycelium.wallet.activity.rmc;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

/**
 * Created by elvis on 20.06.17.
 */

public class ChooseRMCAccountFragment extends Fragment {

    String rmcCount = "0";
    String payMethod;

    @BindView(R.id.create_new_rmc)
    protected View createRmcAccount;
    @BindView(R.id.new_rmc_account)
    protected View newRmcAccount;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            rmcCount = getArguments().getString(Keys.RMC_COUNT);
            payMethod = getArguments().getString(Keys.PAY_METHOD);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_choose_account, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        newRmcAccount.setVisibility(View.GONE);
        ((TextView) view.findViewById(R.id.rmcCount)).setText(rmcCount + " RMC");
    }

    @OnClick(R.id.btCreateNew)
    void clickCreateAcc() {
        createRmcAccount.setVisibility(View.GONE);
        newRmcAccount.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.btYes)
    void clickYes() {
        if (payMethod.equals("BTC")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("bitcoin:13sTW2pA3U8LwixoSapi92LsXyjyXPYhA3?amount=0.004179&r=https%3A%2F%2Fbitpay.com%2Fi%2FMLdKWpRhJXcTv8NKFGLPhT")));
        } else if (payMethod.equals("ETH")) {
            Intent intent = new Intent(getActivity(), EthPaymentRequestActivity.class);
            intent.putExtra(Keys.RMC_COUNT, rmcCount);
            startActivity(intent);
        } else {
            Intent intent = new Intent(getActivity(), BankPaymentRequestActivity.class);
            intent.putExtra(Keys.RMC_COUNT, rmcCount);
            startActivity(intent);
        }
    }

    @OnClick(R.id.btNo)
    void clickNo() {
        getActivity().finish();
    }
}
