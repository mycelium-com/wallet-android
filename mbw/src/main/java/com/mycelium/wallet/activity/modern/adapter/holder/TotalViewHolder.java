package com.mycelium.wallet.activity.modern.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.TotalToggleableCurrencyButton;


public class TotalViewHolder extends RecyclerView.ViewHolder {
    public TotalToggleableCurrencyButton tcdBalance;

    public TotalViewHolder(View itemView) {
        super(itemView);
        tcdBalance = itemView.findViewById(R.id.tcdBalance);
    }
}