package com.mycelium.wallet.activity.fio.domain.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_fio_name.view.*


class FIONameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val fioName = itemView.fioName
    val expireDate = itemView.expireDate
}