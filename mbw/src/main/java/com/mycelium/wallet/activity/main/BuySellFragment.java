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
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.external.BuySellSelectFragment;
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
import butterknife.OnClick;

public class BuySellFragment extends Fragment {
    private MbwManager _mbwManager;
    private View _root;
    @BindView(R.id.action_button)
    Button btAction;

    @BindView(R.id.rotate_button)
    View btRotate;

    private List<ActoinButton> actions = new ArrayList<>();
    private int current = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_buy_sell_fragment, container, false));
        ButterKnife.bind(this, _root);
        recreateActions();
        updateUI();
        return _root;
    }

    private void recreateActions() {
        actions.clear();
        boolean showButton = Iterables.any(_mbwManager.getEnvironmentSettings().getBuySellServices(), new Predicate<BuySellServiceDescriptor>() {
            @Override
            public boolean apply(@Nullable BuySellServiceDescriptor input) {
                return input.isEnabled(_mbwManager);
            }
        });
        switch (_mbwManager.getSelectedAccount().getType()) {
            case BCHBIP44:
            case BCHSINGLEADDRESS:
                actions.add(new ActoinButton(getString(R.string.exchange_bch_to_btc), new Runnable() {
                    @Override
                    public void run() {
                        startExchange(new Intent(getActivity(), ExchangeActivity.class));
                    }
                }));
                break;
            default:
                if (showButton) {
                    actions.add(new ActoinButton(getString(R.string.gd_buy_sell_button), new Runnable() {
                        @Override
                        public void run() {
                            startActivity(new Intent(getActivity(), BuySellSelectFragment.class));
                        }
                    }));
                }
                actions.add(new ActoinButton(getString(R.string.exchange_altcoins_to_btc), new Runnable() {
                    @Override
                    public void run() {
                        startExchange(new Intent(getActivity(), ChangellyActivity.class));
                    }
                }));
        }
        current = 0;
    }

    private void startExchange(Intent intent) {
        //TODO need find more right way to detect is Changelly available
        final ExchangeRate exchangeRate = _mbwManager.getExchangeRateManager().getExchangeRate("BCH");
        if (exchangeRate == null || exchangeRate.price == null) {
            new AlertDialog.Builder(getActivity())
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
        updateUI();
    }

    @Override
    public void onPause() {
        _mbwManager.getEventBus().unregister(this);
        super.onPause();
    }

    @OnClick(R.id.action_button)
    void clickAction() {
        actions.get(current).task.run();
    }

    @OnClick(R.id.rotate_button)
    void clickRotate() {
        current = (current + 1) % actions.size();
        updateUI();
    }

    void updateUI() {
        if (actions.isEmpty()) {
            _root.setVisibility(View.GONE);
        } else {
            _root.setVisibility(View.VISIBLE);
            btAction.setText(actions.get(current).text);
            btRotate.setVisibility(actions.size() == 1 ? View.GONE : View.VISIBLE);
        }
    }

    @Subscribe
    public void selectedAccountChanged(SelectedAccountChanged event) {
        recreateActions();
        updateUI();
    }

    class ActoinButton {
        public ActoinButton(String text, Runnable task) {
            this.text = text;
            this.task = task;
        }

        String text;
        Runnable task;
    }

}
