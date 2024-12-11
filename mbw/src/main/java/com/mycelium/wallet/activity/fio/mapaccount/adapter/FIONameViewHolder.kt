package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.ItemFioNameBinding


class FIONameViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioNameBinding.bind(itemView)
    val fioName = binding.fioName
    val expireDate = binding.expireDate
}