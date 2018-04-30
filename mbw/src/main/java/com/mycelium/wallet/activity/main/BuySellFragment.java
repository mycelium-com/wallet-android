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

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.adapter.ButtonAdapter;
import com.mycelium.wallet.activity.main.model.ActionButton;
import com.mycelium.wallet.activity.settings.SettingsPreference;
import com.mycelium.wallet.activity.util.CenterLayoutManager;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.external.BuySellSelectActivity;
import com.mycelium.wallet.external.BuySellServiceDescriptor;
import com.mycelium.wallet.external.changelly.ChangellyActivity;
import com.mycelium.wallet.external.changelly.bch.ExchangeActivity;
import com.mycelium.wapi.model.ExchangeRate;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import butterknife.BindView;
import butterknife.ButterKnife;

public class BuySellFragment extends Fragment {
    private MbwManager _mbwManager;

    @BindView(R.id.button_list)
    RecyclerView recyclerView;

    ButtonAdapter buttonAdapter;
    CenterLayoutManager layoutManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_buy_sell_fragment, container, false));
        ButterKnife.bind(this, root);
        buttonAdapter = new ButtonAdapter();
        layoutManager = new CenterLayoutManager(getActivity(), LinearLayoutManager.HORIZONTAL, false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(buttonAdapter);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @android.support.annotation.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        recreateActions();
    }

    private void recreateActions() {
        List<ActionButton> actions = new ArrayList<>();
        boolean showButton = Iterables.any(_mbwManager.getEnvironmentSettings().getBuySellServices(), new Predicate<BuySellServiceDescriptor>() {
            @Override
            public boolean apply(@Nullable BuySellServiceDescriptor input) {
                return input.isEnabled(_mbwManager);
            }
        });
        int scrollTo = 0;
        switch (_mbwManager.getSelectedAccount().getType()) {
            case BCHBIP44:
            case BCHSINGLEADDRESS:
                actions.add(new ActionButton(getString(R.string.exchange_bch_to_btc), new Runnable() {
                    @Override
                    public void run() {
                        startExchange(new Intent(getActivity(), ExchangeActivity.class));
                    }
                }));
                break;
            default:
                actions.add(new ActionButton(getString(R.string.exchange_altcoins_to_btc), new Runnable() {
                    @Override
                    public void run() {
                        startExchange(new Intent(getActivity(), ChangellyActivity.class));
                    }
                }));
                if (SettingsPreference.getInstance().isMyDFSEnabled()) {
                    ActionButton actionButton = new ActionButton(getString(R.string.buy_mydfs_token), R.drawable.ic_stars_black_18px, new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://mydfs.net/?ref=mycelium")));
                        }
                    });
                    actionButton.textColor = getResources().getColor(R.color.white);
                    actions.add(actionButton);
                    scrollTo = 1;
                }

                if (showButton) {
                    actions.add(new ActionButton(getString(R.string.gd_buy_sell_button), new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(getActivity(), BuySellSelectActivity.class));
                        }
                    }));
                }
        }

        buttonAdapter.setButtons(actions);
        if (scrollTo != 0) {
            recyclerView.postDelayed(new ScrollToRunner(scrollTo), 500);
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
        //TODO need find more right way to detect is Changelly available
        final ExchangeRate exchangeRate = _mbwManager.getExchangeRateManager().getExchangeRate("BCH");
        if (exchangeRate == null || exchangeRate.price == null) {
            new AlertDialog.Builder(getActivity(), R.style.MyceliumModern_Dialog)
                    .setMessage(R.string.exchange_service_unavailable)
                    .setPositiveButton(R.string.button_ok, null)
                    .create()
                    .show();
            _mbwManager.getExchangeRateManager().requestRefresh();
        } else {
            startActivity(intent);
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        _mbwManager = MbwManager.getInstance(activity);
    }

    @Override
    public void onResume() {
        _mbwManager.getEventBus().register(this);
        super.onResume();
        recreateActions();
    }

    @Override
    public void onPause() {
        _mbwManager.getEventBus().unregister(this);
        super.onPause();
    }

    @Subscribe
    public void selectedAccountChanged(SelectedAccountChanged event) {
        recreateActions();
    }
}
