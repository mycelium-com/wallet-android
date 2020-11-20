package com.mycelium.bequant.receive.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_bequant_select_account.view.*


class AccountGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val label = itemView.label
    val value = itemView.value
}