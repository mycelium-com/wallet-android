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

package com.mycelium.wallet.activity.util;

import java.util.ArrayList;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.content.Context;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;

public class RecommendationAdapter extends ArrayAdapter<PartnerInfo> {

    Context context;
    int layoutResourceId;
    ArrayList<PartnerInfo> data = null;

    public RecommendationAdapter(Context context, int layoutResourceId, ArrayList<PartnerInfo> data) {
        super(context, layoutResourceId, data);
        this.context = context;
        this.layoutResourceId = layoutResourceId;
        this.data = data;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View row = convertView;
        ItemHolder holder;

        boolean addListener = false;

        if (row == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            row = inflater.inflate(layoutResourceId, parent, false);
            holder = new ItemHolder();
            holder.imgIcon = (ImageView) row.findViewById(R.id.ivIcon);
            holder.txtName = (TextView) row.findViewById(R.id.tvTitle);
            holder.txtDescription = (TextView) row.findViewById(R.id.tvDescription);
            row.setTag(holder);
            addListener = true;
        } else {
            holder = (ItemHolder) row.getTag();
        }

        final PartnerInfo bean = data.get(position);
        holder.txtName.setText(bean.getName());
        holder.txtDescription.setText(bean.getDescription());
        holder.imgIcon.setImageResource(bean.getIcon());

        if(addListener) {
            row.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (bean.getInfo() != null && bean.getInfo().length() > 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(context);
                        builder.setMessage(bean.getInfo());
                        builder.setTitle(R.string.warning_partner);
                        builder.setIcon(bean.getIcon());
                        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                if (bean.getUri() != null) {
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setData(Uri.parse(bean.getUri()));
                                    context.startActivity(intent);
                                }
                            }
                        });
                        builder.setNegativeButton(R.string.cancel, null);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        if (bean.getUri() != null) {
                            Intent i = new Intent(Intent.ACTION_VIEW);
                            i.setData(Uri.parse(bean.getUri().toString()));
                            context.startActivity(i);
                        }
                    }
                }
            });
        }
        return row;
    }

    @Override
    public int getCount() {

        if(data.size()<=0)
            return 1;
        return data.size();
    }

    @Override
    public PartnerInfo getItem(int position) {
        return data.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public static class ItemHolder {
        public ImageView imgIcon;
        public TextView txtName;
        public TextView txtDescription;
    }
}
