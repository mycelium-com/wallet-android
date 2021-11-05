package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.ToggleableCurrencyButton

open class GroupTitleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
    val tvAccountsCount: TextView = itemView.findViewById(R.id.tvAccountsCount)
    val tvBalance: ToggleableCurrencyButton? = itemView.findViewById(R.id.tvBalance)
    val expandIcon: ImageView = itemView.findViewById(R.id.expand)
}