package com.mycelium.wallet.activity.modern.adapter.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;


public class TotalViewHolder extends RecyclerView.ViewHolder {
    public ToggleableCurrencyButton tcdBalance;

    public TotalViewHolder(View itemView) {
        super(itemView);
        tcdBalance = itemView.findViewById(R.id.tcdBalance);
    }
}