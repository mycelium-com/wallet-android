package com.mycelium.giftbox.purchase.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_giftbox_select_account.view.*


class AccountItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val label = itemView.label
    val coinType = itemView.coinType
    val value2 = itemView.value2
    val value = itemView.value
}