package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_store.view.*
import java.math.BigDecimal


class StoresAdapter : ListAdapter<ProductInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((ProductInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_store, parent, false))


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        Glide.with(holder.itemView.image)
                .load(item?.cardImageUrl)
                .into(holder.itemView.image)

        holder.itemView.title.text = item.name
        holder.itemView.description.text = item.categories?.joinToString()
        holder.itemView.additional.text = if (item.denominationType == ProductInfo.DenominationType.open) {
            "from ${item.minimumValue.stripTrailingZeros().toPlainString()} ${item.currencyCode}" +
                    if (item.maximumValue != BigDecimal.ZERO) {
                        " to ${item.maximumValue.stripTrailingZeros().toPlainString()} ${item.currencyCode}"
                    } else {
                        ""
                    }
        } else {
            item.availableDenominations?.joinToString { "${it.stripTrailingZeros().toPlainString()} ${item.currencyCode}" }
        }

        holder.itemView.setOnClickListener {
            itemClickListener?.invoke(getItem(holder.adapterPosition))
        }
    }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<ProductInfo>() {
        override fun areItemsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                oldItem.code == newItem.code


        override fun areContentsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.cardImageUrl }, { it.name }, { it.description })
    }
}