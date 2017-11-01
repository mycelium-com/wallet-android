package com.mycelium.wallet.activity.modern.adapter.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;

import com.mycelium.wallet.R;


public class AccountViewHolder extends RecyclerView.ViewHolder {
    public View llAddress;

    public AccountViewHolder(View itemView) {
        super(itemView);
        llAddress = itemView.findViewById(R.id.llAddress);
    }
}