package com.mycelium.wallet.activity.send.event;

import androidx.recyclerview.widget.RecyclerView;

public interface SelectListener {
    void onSelect(RecyclerView.Adapter adapter, int position);
}
