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
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*
import kotlinx.android.synthetic.main.item_giftbox_purchaced_group.view.*

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
        when (getItem(position).type) {
            TYPE_CARD -> {
                val purchasedItem = getItem(position) as CardItem
                val item = purchasedItem.card
                holder.itemView.title.text = item.productName
                holder.itemView.description.text = "${item.amount} ${item.currencyCode}"
                holder.itemView.additional.text = item.timestamp?.getDateString(holder.itemView.resources)
                holder.itemView.redeemLayer.visibility = if (purchasedItem.redeemed) View.VISIBLE else View.GONE
                Glide.with(holder.itemView.image)
                        .load(item.productImg)
                        .apply(RequestOptions()
                                .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
                        .into(holder.itemView.image)
                holder.itemView.setOnClickListener {
                    itemClickListener?.invoke((getItem(holder.adapterPosition) as CardItem).card)
                }
                holder.itemView.more.setOnClickListener { view ->
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
                val item = getItem(position) as GroupItem
                holder.itemView.groupTitle.text = item.title
                holder.itemView.expand.rotation = if (item.isOpened) 180f else 0f
                holder.itemView.setOnClickListener {
                    groupListener?.invoke((getItem(holder.adapterPosition) as GroupItem).title)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

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