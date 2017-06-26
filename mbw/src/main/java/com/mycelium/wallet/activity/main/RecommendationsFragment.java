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

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.PartnerInfo;
import com.mycelium.wallet.activity.util.RecommendationAdapter;

import java.util.ArrayList;

import static com.mycelium.wallet.R.string.cancel;
import static com.mycelium.wallet.R.string.ok;
import static com.mycelium.wallet.R.string.partner_coinbase;
import static com.mycelium.wallet.R.string.partner_coinbase_info;
import static com.mycelium.wallet.R.string.partner_coinbase_short;
import static com.mycelium.wallet.R.string.partner_coinbase_url;
import static com.mycelium.wallet.R.string.partner_hashing24;
import static com.mycelium.wallet.R.string.partner_hashing24_info;
import static com.mycelium.wallet.R.string.partner_hashing24_short;
import static com.mycelium.wallet.R.string.partner_hashing24_url;
import static com.mycelium.wallet.R.string.partner_ledger;
import static com.mycelium.wallet.R.string.partner_ledger_info;
import static com.mycelium.wallet.R.string.partner_ledger_short;
import static com.mycelium.wallet.R.string.partner_ledger_url;
import static com.mycelium.wallet.R.string.partner_more_info_text;
import static com.mycelium.wallet.R.string.partner_purse;
import static com.mycelium.wallet.R.string.partner_purse_info;
import static com.mycelium.wallet.R.string.partner_purse_short;
import static com.mycelium.wallet.R.string.partner_purse_url;
import static com.mycelium.wallet.R.string.partner_trezor;
import static com.mycelium.wallet.R.string.partner_trezor_info;
import static com.mycelium.wallet.R.string.partner_trezor_short;
import static com.mycelium.wallet.R.string.partner_trezor_url;
import static com.mycelium.wallet.R.string.warning_partner;

public class RecommendationsFragment extends Fragment {
    ListView recommendationsList;
    TextView moreInformation;
    private AlertDialog alertDialog;

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.main_recommendations_view, container, false);

        recommendationsList = (ListView) root.findViewById(R.id.list);
        ArrayList<PartnerInfo> list = new ArrayList<>();

        list.add(getPartnerInfo(partner_ledger, partner_ledger_short, partner_ledger_info, partner_ledger_url, R.drawable.ledger_icon));
        list.add(getPartnerInfo(partner_trezor, partner_trezor_short, partner_trezor_info, partner_trezor_url, R.drawable.trezor2));
        list.add(getPartnerInfo(partner_purse, partner_purse_short, partner_purse_info, partner_purse_url, R.drawable.purse_small));
        list.add(getPartnerInfo(partner_coinbase, partner_coinbase_short, partner_coinbase_info, partner_coinbase_url, R.drawable.coinbase));
        list.add(getPartnerInfo(partner_hashing24, partner_hashing24_short, partner_hashing24_info, partner_hashing24_url, R.drawable.hashing24, R.drawable.hashing24_small));

        //View footerView = ((LayoutInflater) ActivityContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.footer_layout, null, false);
        View footerView = inflater.inflate(R.layout.main_recommendations_list_footer, null, false);
        recommendationsList.addFooterView(footerView);
        moreInformation = (TextView) footerView.findViewById(R.id.tvMoreInformation);
        moreInformation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
                builder.setTitle(R.string.your_privacy_out_priority);
                builder.setMessage(partner_more_info_text);
                builder.setPositiveButton(ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                            }
                        });
                builder.setIcon(R.drawable.mycelium_logo_transp_small);
                alertDialog = builder.create();
                alertDialog.show();
            }
        });
        RecommendationAdapter adapter = new RecommendationAdapter(getActivity(), R.layout.main_recommendations_list_item, list);
        recommendationsList.setAdapter(adapter);

        adapter.setClickListener(new RecommendationAdapter.ClickListener() {
            @Override
            public void itemClick(final PartnerInfo bean) {
                if (bean.getInfo() != null && bean.getInfo().length() > 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                    builder.setMessage(bean.getInfo());
                    builder.setTitle(warning_partner);
                    builder.setIcon(bean.getSmallIcon());
                    builder.setPositiveButton(ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            if (bean.getUri() != null) {
                                Intent intent = new Intent(Intent.ACTION_VIEW);
                                intent.setData(Uri.parse(bean.getUri()));
                                startActivity(intent);
                            }
                        }
                    });
                    builder.setNegativeButton(cancel, null);
                    alertDialog = builder.create();
                    alertDialog.show();
                } else {
                    if (bean.getUri() != null) {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(bean.getUri())));
                    }
                }
            }
        });

        return root;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        try {
            if (alertDialog != null && alertDialog.isShowing()) {
                alertDialog.dismiss();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private PartnerInfo getPartnerInfo(int name, int description, int disclaimer, int uri, int icon) {
        return new PartnerInfo(getString(name), getString(description), getString(disclaimer), getString(uri), icon);
    }

    private PartnerInfo getPartnerInfo(int name, int description, int disclaimer, int uri, int icon, int smallIcon) {
        return new PartnerInfo(getString(name), getString(description), getString(disclaimer), getString(uri), icon, smallIcon);
    }
}
