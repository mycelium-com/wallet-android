package com.mycelium.wallet.activity.modern.adapter.holder;


import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;

import butterknife.BindView;
import butterknife.ButterKnife;

public class NewsViewHolder extends RecyclerView.ViewHolder {
    @BindView(R.id.title)
    public TextView title;

    @BindView(R.id.description)
    public TextView description;

    @BindView(R.id.date)
    public TextView date;

    @BindView(R.id.bt_share)
    public View share;

    @BindView(R.id.author)
    public TextView author;

    @BindView(R.id.logo)
    public ImageView authorLogo;


    public NewsViewHolder(View itemView) {
        super(itemView);
        ButterKnife.bind(this, itemView);
    }
}
