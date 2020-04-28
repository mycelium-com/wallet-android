package com.mycelium.bequant.receive.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.view.Denomination
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wapi.wallet.WalletAccount
import kotlinx.android.synthetic.main.layout_bequant_mycelium_wallet_accounts.view.*


class AccountPagerAdapter : ListAdapter<WalletAccount<*>, RecyclerView.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.layout_bequant_mycelium_wallet_accounts, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.itemView.accountLabel.text = item.label
        val value = item.accountBalance.confirmed
        viewHolder.itemView.accountBalance.text = value.toString(Denomination.UNIT)
        viewHolder.itemView.accountBalanceCurrency.text = value.currencySymbol
    }

    class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<WalletAccount<*>>() {
        override fun areItemsTheSame(oldItem: WalletAccount<*>, newItem: WalletAccount<*>): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: WalletAccount<*>, newItem: WalletAccount<*>): Boolean =
                oldItem.accountBalance.confirmed == newItem.accountBalance.confirmed
                        && oldItem.label == newItem.label
    }
}