package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.RecordRowInvestmentBinding


class InvestmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val binding = RecordRowInvestmentBinding.bind(view)
    val balance = binding.tvBalance
    val activateLink = binding.activateLink
}