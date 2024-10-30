package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.getDateString
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemGiftboxPurchacedBinding
import com.mycelium.wallet.databinding.ItemGiftboxPurchacedGroupBinding

abstract class CardListItem(val type: Int)
data class CardItem(val card: Card, val redeemed: Boolean = false) : CardListItem(OrderAdapter.TYPE_CARD)
data class GroupItem(val title: String, val isOpened: Boolean = true) : CardListItem(OrderAdapter.TYPE_GROUP)

class CardAdapter : ListAdapter<CardListItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Card) -> Unit)? = null
    var itemShareListener: ((Card) -> Unit)? = null
    var itemRedeemListener: ((Card) -> Unit)? = null
    var itemUnredeemListener: ((Card) -> Unit)? = null
    var itemDeleteListener: ((Card) -> Unit)? = null
    var groupListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_CARD -> CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))
                TYPE_GROUP -> GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced_group, parent, false))
                else -> TODO("not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bindingAdapterPosition = holder.bindingAdapterPosition
        if (bindingAdapterPosition == RecyclerView.NO_POSITION)
            return
        when (getItem(bindingAdapterPosition).type) {
            TYPE_CARD -> {
                val purchasedItem = getItem(bindingAdapterPosition) as CardItem
                val item = purchasedItem.card
                holder as CardViewHolder
                holder.binding.title.text = item.productName
                holder.binding.description.text = "${item.amount} ${item.currencyCode}"
                holder.binding.additional.text = item.timestamp?.getDateString(holder.itemView.resources)
                holder.binding.redeemLayer.visibility = if (purchasedItem.redeemed) View.VISIBLE else View.GONE
                Glide.with(holder.binding.image)
                        .load(item.productImg)
                        .apply(RequestOptions()
                                .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner)), ))
                        .into(holder.binding.image)
                holder.itemView.setOnClickListener {
                    itemClickListener?.invoke((getItem(bindingAdapterPosition) as CardItem).card)
                }
                holder.binding.more.setOnClickListener { view ->
                    PopupMenu(view.context, view).apply {
                        menuInflater.inflate(R.menu.giftbox_purchased_list, menu)
                        menu.findItem(R.id.redeem).isVisible = !purchasedItem.redeemed
                        menu.findItem(R.id.unredeem).isVisible = purchasedItem.redeemed
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.share -> {
                                    itemShareListener?.invoke((getItem(holder.absoluteAdapterPosition) as CardItem).card)
                                }
                                R.id.delete -> {
                                    itemDeleteListener?.invoke((getItem(holder.absoluteAdapterPosition) as CardItem).card)
                                }
                                R.id.redeem -> {
                                    itemRedeemListener?.invoke((getItem(holder.absoluteAdapterPosition) as CardItem).card)
                                }
                                R.id.unredeem -> {
                                    itemUnredeemListener?.invoke((getItem(holder.absoluteAdapterPosition) as CardItem).card)
                                }
                            }
                            true
                        }
                    }.show()
                }
            }
            TYPE_GROUP -> {
                val item = getItem(bindingAdapterPosition) as GroupItem
                holder as GroupViewHolder
                holder.binding.groupTitle.text = item.title
                holder.binding.expand.rotation = if (item.isOpened) 180f else 0f
                holder.itemView.setOnClickListener {
                    groupListener?.invoke((getItem(bindingAdapterPosition) as GroupItem).title)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemGiftboxPurchacedBinding.bind(itemView)
    }
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemGiftboxPurchacedGroupBinding.bind(itemView)
    }

    class DiffCallback : DiffUtil.ItemCallback<CardListItem>() {
        override fun areItemsTheSame(oldItem: CardListItem, newItem: CardListItem): Boolean =
                oldItem.type == newItem.type &&
                        when (oldItem.type) {
                            TYPE_CARD ->
                                equalsValuesBy(oldItem as CardItem, newItem as CardItem,
                                        { it.card.clientOrderId }, { it.card.pin },
                                        { it.card.deliveryUrl }, { it.card.code }, { it.redeemed })
                            TYPE_GROUP -> equalsValuesBy(oldItem as GroupItem, newItem as GroupItem,
                                    { it.title })
                            else -> true
                        }


        override fun areContentsTheSame(oldItem: CardListItem, newItem: CardListItem): Boolean =
                when (oldItem.type) {
                    TYPE_CARD ->
                        equalsValuesBy(oldItem as CardItem, newItem as CardItem,
                                { it.card.productImg }, { it.card.productName },
                                { it.card.amount }, { it.card.timestamp })
                    TYPE_GROUP -> equalsValuesBy(oldItem as GroupItem, newItem as GroupItem,
                            { it.title }, { it.isOpened })
                    else -> true
                }
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_GROUP = 2
    }
}