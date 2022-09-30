package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_store.view.*
import java.math.BigDecimal


class StoresAdapter : ListAdapter<ProductInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((ProductInfo) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_CARD -> CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_store, parent, false))
                TYPE_LOADING -> LoadingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_loading, parent, false))
                else -> TODO("not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bindingAdapterPosition = holder.bindingAdapterPosition
        if (bindingAdapterPosition == RecyclerView.NO_POSITION)
            return
        if (getItemViewType(position) == TYPE_CARD) {
            val item = getItem(bindingAdapterPosition)
            Glide.with(holder.itemView.image)
                    .load(item?.cardImageUrl)
                    .apply(RequestOptions()
                            .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
                    .into(holder.itemView.image)

            holder.itemView.title.text = item.name
            holder.itemView.description.text = item.categories
                    ?.joinToString { it.replace("-", " ").capitalize() }
            holder.itemView.additional.text = if (item?.denominationType == ProductInfo.DenominationType.fixed && item.availableDenominations?.size ?: 100 < 6) {
                item.availableDenominations?.joinToString { "${it.toPlainString()} ${item.currencyCode}" }
            } else {
                "from ${item.minimumValue.stripTrailingZeros().toPlainString()} ${item.currencyCode}" +
                        if (item.maximumValue != BigDecimal.ZERO) {
                            " to ${item.maximumValue.stripTrailingZeros().toPlainString()} ${item.currencyCode}"
                        } else {
                            ""
                        }
            }
            holder.itemView.setOnClickListener {
                itemClickListener?.invoke(getItem(bindingAdapterPosition))
            }
        }
    }

    override fun getItemViewType(position: Int): Int =
            when (getItem(position)) {
                LOADING_ITEM -> TYPE_LOADING
                else -> TYPE_CARD
            }

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<ProductInfo>() {
        override fun areItemsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                oldItem.code == newItem.code


        override fun areContentsTheSame(oldItem: ProductInfo, newItem: ProductInfo): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.cardImageUrl }, { it.name }, { it.description })
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1

        val LOADING_ITEM = ProductInfo()
    }
}