/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.activity.main;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.InfiniteLinearLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mycelium.view.ItemCentralizer;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.adapter.ButtonAdapter;
import com.mycelium.wallet.activity.main.adapter.ButtonClickListener;
import com.mycelium.wallet.activity.main.model.ActionButton;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.event.PageSelectedEvent;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.external.Ads;
import com.mycelium.wallet.external.BuySellSelectActivity;
import com.mycelium.wallet.external.BuySellServiceDescriptor;
import com.mycelium.wallet.external.changelly.ChangellyActivity;
import com.mycelium.wallet.external.changelly.bch.ExchangeActivity;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BuySellFragment extends Fragment implements ButtonClickListener {
    public enum ACTION {
        BCH, ALT_COIN, BTC, FIO
    }

    private MbwManager mbwManager;

    @BindView(R.id.button_list)
    RecyclerView recyclerView;

    ButtonAdapter buttonAdapter;
    RecyclerView.LayoutManager layoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_buy_sell_fragment, container, false));
        ButterKnife.bind(this, root);
        buttonAdapter = new ButtonAdapter();
        layoutManager = new InfiniteLinearLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(buttonAdapter);
        recyclerView.addOnScrollListener(new ItemCentralizer());
        buttonAdapter.setClickListener(this);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @androidx.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recreateActions();
    }

    private void recreateActions() {
        List<ActionButton> actions = new ArrayList<>();
        boolean showButton = Iterables.any(mbwManager.getEnvironmentSettings().getBuySellServices(), new Predicate<BuySellServiceDescriptor>() {
            @Override
            public boolean apply(@Nullable BuySellServiceDescriptor input) {
                return input.isEnabled(mbwManager);
            }
        });
        int scrollTo = 1;
        if (mbwManager.getSelectedAccount() instanceof Bip44BCHAccount ||
                mbwManager.getSelectedAccount() instanceof SingleAddressBCHAccount) {

            actions.add(new ActionButton(ACTION.BCH, getString(R.string.exchange_bch_to_btc)));
        } else {
            actions.add(new ActionButton(ACTION.ALT_COIN, getString(R.string.exchange_altcoins_to_btc)));
            if (showButton) {
                actions.add(new ActionButton(ACTION.BTC, getString(R.string.gd_buy_sell_button)));
            }
            addFio(actions);
        }
        buttonAdapter.setButtons(actions);
        if (scrollTo != 0) {
            recyclerView.postDelayed(new ScrollToRunner(scrollTo), 500);
        }
    }

    private void addFio(List<ActionButton> actions) {
        if (SettingsPreference.getFioEnabled()) {
            actions.add(new ActionButton(ACTION.FIO, getString(R.string.partner_fiopresale), R.drawable.ic_fiopresale_icon_small));
        }
    }

    @Override
    public void onClick(ActionButton actionButton) {
        switch (actionButton.getId()) {
            case BCH:
                startExchange(new Intent(getActivity(), ExchangeActivity.class));
                break;
            case ALT_COIN:
                startExchange(new Intent(getActivity(), ChangellyActivity.class));
                break;
            case BTC:
                startActivity(new Intent(getActivity(), BuySellSelectActivity.class));
                break;
            case FIO:
                Ads.openFio(requireContext());
                break;
        }
    }

    class ScrollToRunner implements Runnable {
        int scrollTo;

        public ScrollToRunner(int scrollTo) {
            this.scrollTo = scrollTo;
        }

        @Override
        public void run() {
            recyclerView.smoothScrollToPosition(scrollTo);
        }
    }

    private void startExchange(Intent intent) {
        startActivity(intent);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mbwManager = MbwManager.getInstance(context);
    }

    @Override
    public void onStart() {
        MbwManager.getEventBus().register(this);
        recreateActions();
        super.onStart();
    }

    @Override
    public void onStop() {
        MbwManager.getEventBus().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void selectedAccountChanged(SelectedAccountChanged event) {
        recreateActions();
    }

    @Subscribe
    public void pageSelectedEvent(PageSelectedEvent event) {
        if (event.position == 1) {
            recreateActions();
        }
    }
}
