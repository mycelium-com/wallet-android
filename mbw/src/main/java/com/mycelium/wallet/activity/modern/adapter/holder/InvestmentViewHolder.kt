package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.record_row_investment.view.*


class InvestmentViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val balance = view.tvBalance
    val activateLink = view.activateLink
}