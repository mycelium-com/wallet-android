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
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.text.method.LinkMovementMethod;
import android.widget.Button;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;

public class AdFragment extends Fragment {
    private View _root;
    private Button btAdvice;

    private CharSequence adBuy;
    private CharSequence adUrl;
    private CharSequence adInfo;
    private int adIcon;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        _root = Preconditions.checkNotNull(inflater.inflate(R.layout.main_ad_fragment, container, false));
        btAdvice = (Button) _root.findViewById(R.id.btAdvice);
        updateAdContent();
        btAdvice.setMovementMethod(LinkMovementMethod.getInstance());
        return _root;
    }

    private void updateAdContent() {
        double dice = Math.random();
        if(dice < 0.3334) {
            adIcon = R.drawable.purse_small;
            adBuy = getText(R.string.ad_buy_purse);
            adUrl = getText(R.string.ad_purse_url);
            adInfo = getText(R.string.ad_purse_info);
        } else if (dice < 0.6667){
            adIcon = R.drawable.trezor2;
            adBuy = getText(R.string.ad_buy_trezor);
            adUrl = getText(R.string.ad_trezor_url);
            adInfo = getText(R.string.ad_trezor_info);
        } else {
            adIcon = R.drawable.hashing24;
            adBuy = getText(R.string.ad_buy_hashing24);
            adUrl = getText(R.string.ad_hashing24_url);
            adInfo = getText(R.string.ad_hashing24_info);
        }
        btAdvice.setCompoundDrawablesWithIntrinsicBounds(adIcon, 0, 0, 0);
        btAdvice.setText(adBuy);
        btAdvice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                if(adInfo != null && adInfo.length() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(adInfo);
                    builder.setTitle(R.string.warning_partner);
                    builder.setIcon(adIcon);
                    builder.setPositiveButton(R.string.ok,  new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if(adUrl != null) {
                                Intent i = new Intent(Intent.ACTION_VIEW); i.setData(Uri.parse(adUrl.toString()));
                                startActivity(i);
                            }
                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                        }
                    });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    if (adUrl != null) {
                        Intent i = new Intent(Intent.ACTION_VIEW);
                        i.setData(Uri.parse(adUrl.toString()));
                        startActivity(i);
                    }
                }
            }
        });
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(false);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onResume() {
        updateAdContent();
        super.onResume();
    }

}
