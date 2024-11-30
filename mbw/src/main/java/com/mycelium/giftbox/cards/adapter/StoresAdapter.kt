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
import com.mycelium.giftbox.cardValues
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.models.ProductInfo
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemGiftboxStoreBinding
import java.math.BigDecimal
import java.util.Locale


class StoresAdapter : ListAdapter<MCProductInfo, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((MCProductInfo) -> Unit)? = null

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
            holder as CardViewHolder
            Glide.with(holder.binding.image)
                    .load(item?.logoUrl)
                    .apply(RequestOptions()
                            .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
                    .into(holder.binding.image)

            holder.binding.title.text = item.name
            holder.binding.description.text = item.categories?.map {
                it.replace("-", " ").capitalize(Locale.ROOT)
            }?.joinToString()

            holder.binding.additional.text = item?.cardValues()
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

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemGiftboxStoreBinding.bind(itemView)
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<MCProductInfo>() {
        override fun areItemsTheSame(oldItem: MCProductInfo, newItem: MCProductInfo): Boolean =
                oldItem.id == newItem.id


        override fun areContentsTheSame(oldItem: MCProductInfo, newItem: MCProductInfo): Boolean =
                equalsValuesBy(oldItem, newItem,
                        { it.cardImageUrl }, { it.name }/*, { it.description }*/)
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1

        val LOADING_ITEM = MCProductInfo()
    }
}