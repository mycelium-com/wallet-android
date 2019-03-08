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
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.net.ServerEndpointType;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.main.address.AddressFragment;
import com.mycelium.wallet.activity.rmc.RMCAddressFragment;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.event.SelectedAccountChanged;
import com.mycelium.wallet.event.TorStateChanged;
import com.mycelium.wapi.wallet.WalletAccount;
import com.squareup.otto.Subscribe;

public class BalanceMasterFragment extends Fragment {
    private TextView tvTor;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = Preconditions.checkNotNull(inflater.inflate(R.layout.balance_master_fragment, container, false));
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        WalletAccount account = MbwManager.getInstance(this.getActivity()).getSelectedAccount();
        defineAddressAccountView(fragmentTransaction, account);
        fragmentTransaction.replace(R.id.phFragmentBalance, new BalanceFragment());
        fragmentTransaction.replace(R.id.phFragmentNotice, new NoticeFragment());
        fragmentTransaction.replace(R.id.phFragmentBuySell, new BuySellFragment());
        fragmentTransaction.commitAllowingStateLoss();
        return view;
    }

    private void defineAddressAccountView(FragmentTransaction fragmentTransaction, WalletAccount account) {
        fragmentTransaction.replace(R.id.phFragmentAddress,
                account instanceof ColuAccount && ((ColuAccount) account).getColuAsset().assetType == ColuAccount.ColuAssetType.RMC ?
                        new RMCAddressFragment() : new AddressFragment());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart() {
        Activity activity = getActivity();
        // Set build version
        ((TextView) activity.findViewById(R.id.tvBuildText)).setText(getResources().getString(R.string.build_text,
                BuildConfig.VERSION_NAME));

        MbwManager mbwManager = MbwManager.getInstance(activity);
        tvTor = activity.findViewById(R.id.tvTorState);
        if (mbwManager.getTorMode() == ServerEndpointType.Types.ONLY_TOR && mbwManager.getTorManager() != null) {
            tvTor.setVisibility(View.VISIBLE);
            showTorState(mbwManager.getTorManager().getInitState());
        } else {
            tvTor.setVisibility(View.GONE);
        }
        updateAddressView();
        MbwManager.getEventBus().register(this);
        super.onStart();
    }

    @Override
    public void onStop() {
        MbwManager.getEventBus().unregister(this);
        super.onStop();
    }

    @Subscribe
    public void onTorState(TorStateChanged torState) {
        showTorState(torState.percentage);
    }

    @Subscribe
    public void selectedAccountChanged(SelectedAccountChanged event) {
        updateAddressView();
    }

    private void updateAddressView() {
        WalletAccount account = MbwManager.getInstance(this.getActivity()).getSelectedAccount();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        defineAddressAccountView(fragmentTransaction, account);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void showTorState(int percentage) {
        if (percentage == 0 || percentage == 100) {
            tvTor.setText("");
        } else {
            tvTor.setText(getString(R.string.tor_state_init));
        }
    }
}
