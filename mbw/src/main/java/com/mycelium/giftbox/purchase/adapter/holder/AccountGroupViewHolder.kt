package com.mycelium.giftbox.purchase.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_giftbox_select_account_group.view.*


class AccountGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val chevron = itemView.chevron
    val label = itemView.label
    val count = itemView.count
    val value = itemView.value
}