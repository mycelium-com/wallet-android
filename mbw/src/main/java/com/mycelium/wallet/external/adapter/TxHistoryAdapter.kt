package com.mycelium.wallet.external.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CircleCrop
import com.bumptech.glide.request.RequestOptions
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemChangelly2HistoryBinding
import com.mycelium.wallet.external.changelly2.ExchangeFragment

data class TxItem(val id: String,
                  val amountFrom: String,
                  val amountTo: String,
                  val currencyFrom: String,
                  val currencyTo: String,
                  val date: String,
                  val status: String)

class TxHistoryAdapter : ListAdapter<TxItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var clickListener: ((TxItem) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_changelly2_history, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val h = holder as ViewHolder
        val item = getItem(position)
        h.binding.status.text = item.status.capitalize()
        h.binding.date.text = item.date
        h.binding.amountFrom.text = "${item.amountFrom} ${item.currencyFrom.toUpperCase()}"
        h.binding.amountTo.text = "${item.amountTo} ${item.currencyTo.toUpperCase()}"

        Glide.with(h.binding.iconFrom).clear(h.binding.iconFrom)
        Glide.with(h.binding.iconFrom)
                .load(ExchangeFragment.iconPath(item.currencyFrom))
                .apply(RequestOptions().transforms(CircleCrop()))
                .into(h.binding.iconFrom)

        Glide.with(h.binding.iconTo).clear(h.binding.iconTo)
        Glide.with(h.binding.iconTo)
                .load(ExchangeFragment.iconPath(item.currencyTo))
                .apply(RequestOptions().transforms(CircleCrop()))
                .into(h.binding.iconTo)

        h.itemView.setOnClickListener {
            clickListener?.invoke(getItem(holder.absoluteAdapterPosition))
        }
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val binding = ItemChangelly2HistoryBinding.bind(view)
    }

    class DiffCallback : DiffUtil.ItemCallback<TxItem>() {
        override fun areItemsTheSame(oldItem: TxItem, newItem: TxItem): Boolean =
                oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: TxItem, newItem: TxItem): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.status }, { it.amountFrom }, { it.amountTo }, { it.date },
                        { it.currencyFrom }, { it.currencyTo })

    }
}