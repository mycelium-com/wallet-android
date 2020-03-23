package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.record_row_investment.view.*


class InvestmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val label = view.tvLabel
    val balance = view.tvBalance
}