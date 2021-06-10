package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.getDateString
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*
import kotlinx.android.synthetic.main.item_giftbox_purchaced_group.view.*

abstract class PurchasedItem(val type: Int)
data class PurchasedOrderItem(val order: Order, val redeemed: Boolean = false) : PurchasedItem(PurchasedAdapter.TYPE_CARD)
data class PurchasedGroupItem(val title: String, val isOpen = true) : PurchasedItem(PurchasedAdapter.TYPE_GROUP)
object PurchasedLoadingItem : PurchasedItem(PurchasedAdapter.TYPE_LOADING)

class PurchasedAdapter : ListAdapter<PurchasedItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Order) -> Unit)? = null
    var itemShareListener: ((Order) -> Unit)? = null
    var itemRedeemListener: ((Order) -> Unit)? = null
    var itemDeleteListener: ((Order) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_CARD -> CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false))
                TYPE_GROUP -> GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced_group, parent, false))
                TYPE_LOADING -> LoadingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_loading, parent, false))
                else -> TODO("not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItem(position).type) {
            TYPE_CARD -> {
                val purchasedItem = getItem(position) as PurchasedOrderItem
                val item = purchasedItem.order
                holder.itemView.title.text = item.productName
                holder.itemView.description.text = item.amount
                holder.itemView.additional.text = when (item.status) {
                    Status.pROCESSING -> {
                        holder.itemView.additionalLabel.visibility = View.GONE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                        "Processing"
                    }
                    Status.eRROR -> {
                        holder.itemView.additionalLabel.visibility = View.GONE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                        "Failed"
                    }
                    else -> {
                        holder.itemView.additionalLabel.visibility = View.VISIBLE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        item.timestamp?.getDateString(holder.itemView.resources)
                    }
                }
                holder.itemView.more.setOnClickListener { view ->
                    PopupMenu(view.context, view).run {
                        menuInflater.inflate(R.menu.giftbox_purchased_list, menu)
                        setOnMenuItemClickListener { menuItem ->
                            when (menuItem.itemId) {
                                R.id.share -> {
                                    itemShareListener?.invoke((getItem(holder.adapterPosition) as PurchasedOrderItem).order)
                                }
                                R.id.delete -> {
                                    itemDeleteListener?.invoke((getItem(holder.adapterPosition) as PurchasedOrderItem).order)
                                }
                                R.id.redeem -> {
                                    itemRedeemListener?.invoke((getItem(holder.adapterPosition) as PurchasedOrderItem).order)
                                }
                            }
                            true
                        }
                        show()
                    }
                }
                holder.itemView.isEnabled = !purchasedItem.redeemed
                Glide.with(holder.itemView.image)
                        .load(item.productImg)
                        .into(holder.itemView.image)
                holder.itemView.setOnClickListener {
                    itemClickListener?.invoke((getItem(holder.adapterPosition) as PurchasedOrderItem).order)
                }
            }
            TYPE_GROUP -> {
                holder.itemView.groupTitle.text = (getItem(position) as PurchasedGroupItem).title
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<PurchasedItem>() {
        override fun areItemsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                oldItem.type == newItem.type && oldItem == newItem


        override fun areContentsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                when (oldItem.type) {
                    TYPE_CARD ->
                        equalsValuesBy(oldItem as PurchasedOrderItem, newItem as PurchasedOrderItem,
                                { it.order.productImg }, { it.order.productName },
                                { it.order.amount }, { it.order.timestamp })
                    TYPE_GROUP -> equalsValuesBy(oldItem as PurchasedGroupItem, newItem as PurchasedGroupItem,
                            { it.title })
                    else -> true
                }
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1
        const val TYPE_GROUP = 2
    }
}