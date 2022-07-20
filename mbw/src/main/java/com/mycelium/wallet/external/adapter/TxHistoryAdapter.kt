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
import com.mycelium.wallet.external.changelly2.ExchangeFragment
import kotlinx.android.synthetic.main.item_changelly2_history.view.*

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
        h.status.text = item.status.capitalize()
        h.date.text = item.date
        h.amountFrom.text = "${item.amountFrom} ${item.currencyFrom.toUpperCase()}"
        h.amountTo.text = "${item.amountTo} ${item.currencyTo.toUpperCase()}"

        Glide.with(h.iconFrom).clear(h.iconFrom)
        Glide.with(h.iconFrom)
                .load(ExchangeFragment.iconPath(item.currencyFrom))
                .apply(RequestOptions().transforms(CircleCrop()))
                .into(h.iconFrom)

        Glide.with(h.iconTo).clear(h.iconTo)
        Glide.with(h.iconTo)
                .load(ExchangeFragment.iconPath(item.currencyTo))
                .apply(RequestOptions().transforms(CircleCrop()))
                .into(h.iconTo)

        h.itemView.setOnClickListener {
            clickListener?.invoke(getItem(holder.absoluteAdapterPosition))
        }
    }

    class ViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
        val status = view.status
        val date = view.date
        val amountFrom = view.amountFrom
        val amountTo = view.amountTo
        val iconFrom = view.iconFrom
        val iconTo = view.iconTo
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