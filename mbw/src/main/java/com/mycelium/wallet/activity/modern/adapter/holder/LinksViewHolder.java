package com.mycelium.wallet.activity.modern.adapter.holder;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.mycelium.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LinksViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.telegram_link)
    public TextView telegram;

    public LinksViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
