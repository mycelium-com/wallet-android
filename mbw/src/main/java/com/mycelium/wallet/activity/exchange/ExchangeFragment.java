package com.mycelium.wallet.activity.exchange;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.exchange.adapter.ExchangeAccountAdapter;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wapi.wallet.WalletManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExchangeFragment extends Fragment {
    @BindView(R.id.from_account_list)
    SelectableRecyclerView fromRecyclerView;

    @BindView(R.id.to_account_list)
    SelectableRecyclerView toRecyclerView;

    private WalletManager walletManager;
    private ExchangeAccountAdapter toAccountAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        walletManager = MbwManager.getInstance(getActivity()).getWalletManager(false);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchage, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @OnClick(R.id.buttonContinue)
    void continueClick() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ConfirmExchangeFragment())
                .commitAllowingStateLoss();
        fromRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        toRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        int senderFinalWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int feeFirstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;

        toAccountAdapter = new ExchangeAccountAdapter(walletManager.getActiveAccounts()
                , feeFirstItemWidth);
        toRecyclerView.setAdapter(toAccountAdapter);
    }
}
