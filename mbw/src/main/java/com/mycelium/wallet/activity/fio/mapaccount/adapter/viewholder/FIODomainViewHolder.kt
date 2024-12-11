package com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemFioNameBinding


class FIODomainViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = ItemFioNameBinding.bind(itemView)
    val title = binding.fioName
    val expireDate = binding.expireDate

    init {
        title.setCompoundDrawablesRelativeWithIntrinsicBounds(
                title.resources.getDrawable(R.drawable.ic_fio_domain),
                null, null, null)
    }
}