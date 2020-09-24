package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_fio_name_details_account.view.*


class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val icon = itemView.icon
    val label = itemView.title
    val type = itemView.subtitle
    val balance = itemView.balance
}