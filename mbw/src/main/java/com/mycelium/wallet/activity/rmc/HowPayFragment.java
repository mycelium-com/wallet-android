package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.R;

/**
 * Created by elvis on 20.06.17.
 */

public class HowPayFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_how_pay, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.btBtc).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RmcBtcAmountFragment())
                        .commitAllowingStateLoss();
            }
        });

        view.findViewById(R.id.btEth).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new RmcEthAmountFragment())
                        .commitAllowingStateLoss();

            }
        });
//        view.findViewById(R.id.btBankwire).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                getFragmentManager().beginTransaction()
//                        .replace(R.id.fragment_container, new RmcBankAmountFragment())
//                        .commitAllowingStateLoss();
//            }
//        });
    }
}
