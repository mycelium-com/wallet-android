package com.mycelium.wallet.activity.modern.adapter.holder;

import androidx.recyclerview.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton;


public class GroupTitleViewHolder extends RecyclerView.ViewHolder {
    public TextView tvTitle;
    public TextView tvAccountsCount;
    public ToggleableCurrencyButton tvBalance;
    public ImageView expandIcon;

    public GroupTitleViewHolder(View itemView) {
        super(itemView);
        tvTitle = itemView.findViewById(R.id.tvTitle);
        tvBalance = itemView.findViewById(R.id.tvBalance);
        tvAccountsCount = itemView.findViewById(R.id.tvAccountsCount);
        expandIcon = itemView.findViewById(R.id.expand);
    }
}