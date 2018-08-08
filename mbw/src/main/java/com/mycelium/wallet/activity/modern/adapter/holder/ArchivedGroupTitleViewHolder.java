package com.mycelium.wallet.activity.modern.adapter.holder;

import android.view.View;

import com.mycelium.wallet.R;


public class ArchivedGroupTitleViewHolder extends GroupTitleViewHolder {
    public ArchivedGroupTitleViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvBalance = itemView.findViewById(R.id.tvBalance);
        tvBalance.setVisibility(View.GONE);
        tvAccountsCount = itemView.findViewById(R.id.tvAccountsCount);
        expandIcon = itemView.findViewById(R.id.expand);
    }
}