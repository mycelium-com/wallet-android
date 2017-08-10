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
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.activity.rmc.Keys;
import com.mycelium.wallet.activity.rmc.RmcActivity;
import com.mycelium.wallet.external.BuySellSelectFragment;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.external.BuySellServiceDescriptor;

import com.squareup.otto.Subscribe;

import java.net.URISyntaxException;
import java.util.Calendar;

import javax.annotation.Nullable;

public class BuySellFragment extends Fragment {
    private MbwManager _mbwManager;
    private View _root;
    private boolean showButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_buy_sell_fragment, container, false));
        return _root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(false);
        showButton = Iterables.any(_mbwManager.getEnvironmentSettings().getBuySellServices(), new Predicate<BuySellServiceDescriptor>() {
            @Override
            public boolean apply(@Nullable BuySellServiceDescriptor input) {
                return input.isEnabled(_mbwManager);
            }
        });
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
        updateUi();
        super.onResume();
    }

    private void updateUi() {
        View btBuySell = _root.findViewById(R.id.btBuySellBitcoin);
        WalletAccount account = Preconditions.checkNotNull(_mbwManager.getSelectedAccount());
        if(account instanceof ColuAccount) {
            btBuySell.setVisibility(View.GONE);
        } else {
            if(showButton) {
                btBuySell.setVisibility(View.VISIBLE);
                btBuySell.setOnClickListener(buySellOnClickListener);
            } else {
                btBuySell.setVisibility(View.GONE);
            }
        }
        View btBuySellRmc = _root.findViewById(R.id.btBuySellRMC);
        if(Calendar.getInstance().before(Keys.getICOEnd())) {
            btBuySellRmc.setOnClickListener(buySellRmcOnClickListener);
            _root.findViewById(R.id.btLearnMoreRMC).setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    try {
                        startActivity(Intent.parseUri("http://rmc.one/", 0));
                    } catch (URISyntaxException e) {
                        e.printStackTrace();
                    }
                }
            });
        }else {
            btBuySellRmc.setVisibility(View.GONE);
            _root.findViewById(R.id.btLearnMoreRMC).setVisibility(View.GONE);
        }
        super.onResume();
    }

    OnClickListener buySellOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent intent = new Intent(getActivity(), BuySellSelectFragment.class);
            startActivity(intent);
        }
    };

    OnClickListener buySellRmcOnClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            Utils.showOptionalMessage(getActivity(), R.string.mycelium_no_responaility_rmc, new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(getActivity(), RmcActivity.class));
                }
            });
        }
    };

   /**
    * The selected Account changed, update UI to enable/disable purchase
    */
   @Subscribe
   public void selectedAccountChanged(SelectedAccountChanged event) {
      updateUi();
   }

}
