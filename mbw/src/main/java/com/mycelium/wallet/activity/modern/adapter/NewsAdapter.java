package com.mycelium.wallet.activity.modern.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.adapter.holder.LinksViewHolder;
import com.mycelium.wallet.activity.modern.adapter.holder.NewsViewHolder;

public class NewsAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public final static int TYPE_NEWS = 1;
    public final static int TYPE_LINKS = 2;

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_NEWS) {
            return new NewsViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_news, parent, false));
        } else if (viewType == TYPE_LINKS) {
            return new LinksViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_links, parent, false));
        }
        return null;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

    }

    @Override
    public int getItemCount() {
        return 5;
    }

    @Override
    public int getItemViewType(int position) {
        if (position == 0) {
            return TYPE_LINKS;
        } else {
            return TYPE_NEWS;
        }
    }
}