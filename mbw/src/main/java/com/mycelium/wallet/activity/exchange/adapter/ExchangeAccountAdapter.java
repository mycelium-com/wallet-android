package com.mycelium.wallet.activity.exchange.adapter;


import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView;
import com.mycelium.wapi.wallet.WalletAccount;

import java.util.List;

public class ExchangeAccountAdapter extends SelectableRecyclerView.Adapter<RecyclerView.ViewHolder> {
    private List<WalletAccount> accounts;
    private int paddingWidth = 0;

    public ExchangeAccountAdapter(List<WalletAccount> accounts, int paddingWidth) {
        this.accounts = accounts;
        this.paddingWidth = paddingWidth;
    }

    @Override
    public int findIndex(Object selected) {
        return 0;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ITEM) {
            // create a new view
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.recyclerview_item_fee_lvl, parent, false);
            v.findViewById(R.id.categorytextView).setVisibility(View.GONE);
            ImageView imageView = (ImageView) v.findViewById(R.id.rectangle);
            imageView.setImageResource(R.drawable.recyclerview_item_bottom_rectangle_selector);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) imageView.getLayoutParams();
            layoutParams.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            layoutParams.height = parent.getResources().getDimensionPixelSize(R.dimen.recycler_item_rectangle_height);
            imageView.setLayoutParams(layoutParams);
            return new ViewHolder(v);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_padding_sender,
                    parent, false);
            RecyclerView.LayoutParams layoutParams = (RecyclerView.LayoutParams) view.getLayoutParams();
            layoutParams.width = paddingWidth;
            view.setLayoutParams(layoutParams);
            return new ViewHolder(view);
        }
    }

    @Override
    public int getItemCount() {
        return accounts.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public TextView categoryTextView;
        public TextView itemTextView;
        public TextView valueTextView;

        public ViewHolder(View v) {
            super(v);
            categoryTextView = (TextView) v.findViewById(R.id.categorytextView);
            itemTextView = (TextView) v.findViewById(R.id.itemTextView);
            valueTextView = (TextView) v.findViewById(R.id.valueTextView);
        }
    }
}
