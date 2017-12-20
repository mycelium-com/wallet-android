package com.mycelium.wallet.external.changelly.bch;


import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mycelium.wallet.activity.view.ValueKeyboard;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.exchange.adapter.ExchangeAccountAdapter;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wallet.external.changelly.AccountAdapter;
import com.mycelium.wapi.wallet.WalletManager;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExchangeFragment extends Fragment {
    @BindView(R.id.from_account_list)
    SelectableRecyclerView fromRecyclerView;

    @BindView(R.id.to_account_list)
    SelectableRecyclerView toRecyclerView;

    @BindView(R.id.numeric_keyboard)
    ValueKeyboard valueKeyboard;

    @BindView(R.id.fromValue)
    TextView fromValue;

    @BindView(R.id.toValue)
    TextView toValue;

    private MbwManager mbwManager;
    private WalletManager walletManager;
    private AccountAdapter toAccountAdapter;
    private AccountAdapter fromAccountAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mbwManager = MbwManager.getInstance(getActivity());
        walletManager = MbwManager.getInstance(getActivity()).getWalletManager(false);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exchage, container, false);
        ButterKnife.bind(this, view);
        fromRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        toRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false));
        int senderFinalWidth = getActivity().getWindowManager().getDefaultDisplay().getWidth();
        int firstItemWidth = (senderFinalWidth - getResources().getDimensionPixelSize(R.dimen.item_dob_width)) / 2;

        toAccountAdapter = new AccountAdapter(mbwManager, walletManager.getActiveAccounts(), firstItemWidth);
        toAccountAdapter.setAccountUseType(AccountAdapter.AccountUseType.IN);
        toRecyclerView.setAdapter(toAccountAdapter);

        fromAccountAdapter = new AccountAdapter(mbwManager, walletManager.getActiveAccounts(), firstItemWidth);
        fromAccountAdapter.setAccountUseType(AccountAdapter.AccountUseType.OUT);
        fromRecyclerView.setAdapter(fromAccountAdapter);

        valueKeyboard.setVisibility(android.view.View.GONE);
        return view;
    }


    @OnClick(R.id.buttonContinue)
    void continueClick() {
        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, new ConfirmExchangeFragment(), "ConfirmExchangeFragment")
                .addToBackStack("ConfirmExchangeFragment")
                .commitAllowingStateLoss();
    }

    @OnClick(R.id.toValue)
    void toValueClick() {
        valueKeyboard.setInputTextView(toValue);
        valueKeyboard.setVisibility(View.VISIBLE);
        fromRecyclerView.setVisibility(View.GONE);
        toRecyclerView.setVisibility(View.GONE);
        valueKeyboard.setInputListener(new ValueKeyboard.SimpleInputListener() {
            @Override
            public void done() {
                fromRecyclerView.setVisibility(View.VISIBLE);
                toRecyclerView.setVisibility(View.VISIBLE);
            }
        });
    }

    @OnClick(R.id.fromValue)
    void fromValueClick() {
        valueKeyboard.setInputTextView(fromValue);
        valueKeyboard.setVisibility(View.VISIBLE);
    }
}
