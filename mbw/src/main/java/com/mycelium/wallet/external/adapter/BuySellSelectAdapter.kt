package com.mycelium.wallet.external.adapter

import android.graphics.drawable.Drawable
import android.graphics.drawable.PictureDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.svg.GlideApp
import com.mycelium.wallet.svg.GlideRequests
import com.mycelium.wallet.svg.SvgSoftwareLayerSetter
import kotlinx.android.synthetic.main.buy_sell_service_row.view.*

data class BuySellSelectItem(val title: String, val description: String,
                             val imageDrawable: Drawable? = null, val image: String? = null, val listener: (() -> Unit)? = null)


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
            val glideRequests: GlideRequests = GlideApp.with(viewHolder.itemView.ivIcon)
            val glideRequest = if (item.image.endsWith(".svg")) glideRequests.`as`(PictureDrawable::class.java).listener(SvgSoftwareLayerSetter()) else glideRequests.asBitmap()
            glideRequest.load(item.image).into(viewHolder.itemView.ivIcon)
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