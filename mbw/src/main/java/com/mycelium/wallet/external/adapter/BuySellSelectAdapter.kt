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
import com.mycelium.wallet.databinding.BuySellServiceRowBinding

data class BuySellSelectItem(val title: String, val description: String,
                             val imageDrawable: Drawable? = null, val image: String? = null
                             , val listener: (() -> Unit)? = null)


class BuySellSelectAdapter : ListAdapter<BuySellSelectItem, RecyclerView.ViewHolder>(DiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.buy_sell_service_row, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder as ViewHolder
        viewHolder.binding.tvServiceName.text = item.title
        viewHolder.binding.tvServiceDescription.text = item.description
        if (item.imageDrawable != null) {
            viewHolder.binding.ivIcon.setImageDrawable(item.imageDrawable)
            viewHolder.binding.ivIcon.visibility = VISIBLE
        } else if (item.image != null) {
            Glide.with(viewHolder.binding.ivIcon)
                    .load(item.image)
                    .into(viewHolder.binding.ivIcon)
            viewHolder.binding.ivIcon.visibility = VISIBLE
        } else {
            viewHolder.binding.ivIcon.visibility = INVISIBLE
        }
        viewHolder.itemView.setOnClickListener {
            getItem(viewHolder.absoluteAdapterPosition).listener?.invoke()
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = BuySellServiceRowBinding.bind(itemView)
    }

    class DiffCallback : DiffUtil.ItemCallback<BuySellSelectItem>() {
        override fun areItemsTheSame(oldItem: BuySellSelectItem, newItem: BuySellSelectItem): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: BuySellSelectItem, newItem: BuySellSelectItem): Boolean =
                oldItem.title == newItem.title
                        && oldItem.description == newItem.description
                        && oldItem.image == newItem.image
    }
}