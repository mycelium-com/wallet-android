package com.mycelium.wallet.external.adapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.buy_sell_service_row.view.*

data class BuySellSelectItem(val title: String, val description: String,
                             val imageDrawable: Drawable? = null, val image: String? = null
                             , val listener: (() -> Unit)? = null)


class BuySellSelectAdapter : ListAdapter<BuySellSelectItem, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.buy_sell_service_row, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.itemView.tvServiceName.text = item.title
        viewHolder.itemView.tvServiceDescription.text = item.description
        if (item.imageDrawable != null) {
            viewHolder.itemView.ivIcon.setImageDrawable(item.imageDrawable)
            viewHolder.itemView.ivIcon.visibility = VISIBLE
        } else if (item.image != null) {
            Glide.with(viewHolder.itemView.ivIcon)
                    .load(item.image)
                    .into(viewHolder.itemView.ivIcon)
            viewHolder.itemView.ivIcon.visibility = VISIBLE
        } else {
            viewHolder.itemView.ivIcon.visibility = INVISIBLE
        }
        viewHolder.itemView.setOnClickListener {
            getItem(viewHolder.adapterPosition).listener?.invoke()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<BuySellSelectItem>() {
        override fun areItemsTheSame(oldItem: BuySellSelectItem, newItem: BuySellSelectItem): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: BuySellSelectItem, newItem: BuySellSelectItem): Boolean =
                oldItem.title == newItem.title
                        && oldItem.description == newItem.description
                        && oldItem.image == newItem.image
    }
}