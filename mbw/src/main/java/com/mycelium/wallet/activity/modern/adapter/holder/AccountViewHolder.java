package com.mycelium.wallet.activity.modern.adapter.holder;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mycelium.wallet.R;


public class AccountViewHolder extends RecyclerView.ViewHolder {
    public View llAddress;
    public TextView tvLabel;
    public TextView tvWhatIsIt;
    public ImageView icon;
    public TextView tvAddress;
    public TextView tvAccountType;
    public TextView tvBalance;
    public TextView backupMissing;
    public TextView tvProgress;
    public View tvProgressLayout;
    public ImageView ivWhatIsSync;
    public ProgressBar progressBar;

    public View tvTraderKey;

    public AccountViewHolder(View itemView) {
        super(itemView);
        llAddress = itemView.findViewById(R.id.llAddress);
        tvLabel = itemView.findViewById(R.id.tvLabel);
        tvWhatIsIt = itemView.findViewById(R.id.tvWhatIsIt);
        icon = itemView.findViewById(R.id.ivIcon);
        tvAddress = itemView.findViewById(R.id.tvAddress);
        tvAccountType = itemView.findViewById(R.id.tvAccountType);
        tvBalance = itemView.findViewById(R.id.tvBalance);
        backupMissing = itemView.findViewById(R.id.tvBackupMissingWarning);
        tvTraderKey = itemView.findViewById(R.id.tvTraderKey);
        tvProgress = itemView.findViewById(R.id.tvProgress);
        tvProgressLayout = itemView.findViewById(R.id.tvProgressLayout);
        ivWhatIsSync = itemView.findViewById(R.id.ivWhatIsSync);
        progressBar = itemView.findViewById(R.id.progress_bar);
    }
}