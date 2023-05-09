package com.mycelium.giftbox.cards.adapter

import android.content.res.ColorStateList
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.CenterCrop
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.request.RequestOptions
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.getDateString
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_giftbox_purchaced.view.*
import kotlinx.android.synthetic.main.item_giftbox_purchaced_group.view.*
import java.math.BigDecimal

abstract class PurchasedItem(val type: Int)
data class PurchasedOrderItem(val order: Order, val redeemed: Boolean = false) : PurchasedItem(OrderAdapter.TYPE_CARD)
data class PurchasedGroupItem(val title: String, val isOpened: Boolean = true) : PurchasedItem(OrderAdapter.TYPE_GROUP)
object PurchasedLoadingItem : PurchasedItem(OrderAdapter.TYPE_LOADING)

class OrderAdapter : ListAdapter<PurchasedItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((Order) -> Unit)? = null
    var groupListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_CARD -> CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false)).apply {
                    itemView.more.visibility = GONE
                    itemView.descriptionLabel.visibility = GONE
                }
                TYPE_GROUP -> GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced_group, parent, false))
                TYPE_LOADING -> LoadingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_loading, parent, false))
                else -> TODO("not implemented")
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val bindingAdapterPosition = holder.bindingAdapterPosition
        if (bindingAdapterPosition == RecyclerView.NO_POSITION)
            return
        when (getItem(bindingAdapterPosition).type) {
            TYPE_CARD -> {
                val purchasedItem = getItem(bindingAdapterPosition) as PurchasedOrderItem
                val item = purchasedItem.order
                holder.itemView.title.text = item.productName
                val amount = (item.amount?.toBigDecimal() ?: BigDecimal.ZERO) *
                        (item.quantity ?: BigDecimal.ZERO)
                holder.itemView.description.text = "${amount.stripTrailingZeros().toPlainString()} ${item.currencyCode}"
                holder.itemView.additional.text = when (item.status) {
                    Status.pROCESSING -> {
                        holder.itemView.additionalLabel.visibility = GONE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                        val color = holder.itemView.context.resources.getColor(R.color.giftbox_yellow)
                        holder.itemView.additional.setTextColor(color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            holder.itemView.additional.compoundDrawableTintList = ColorStateList.valueOf(color)
                        }
                        holder.itemView.context.getString(R.string.payment_in_progress)
                    }
                    Status.eRROR -> {
                        holder.itemView.additionalLabel.visibility = GONE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_failed, 0, 0, 0)
                        val color = holder.itemView.context.resources.getColor(R.color.fio_white_alpha_0_9)
                        holder.itemView.additional.setTextColor(color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            holder.itemView.additional.compoundDrawableTintList = ColorStateList.valueOf(color)
                        }
                        holder.itemView.context.getString(R.string.failed)
                    }
                    else -> {
                        holder.itemView.additionalLabel.visibility = View.VISIBLE
                        holder.itemView.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        holder.itemView.additional.setTextColor(holder.itemView.context.resources.getColor(R.color.giftbox_green))
                        item.timestamp?.getDateString(holder.itemView.resources)
                    }
                }
                holder.itemView.isEnabled = !purchasedItem.redeemed
                Glide.with(holder.itemView.image)
                        .load(item.productImg)
                        .apply(RequestOptions()
                                .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
                        .into(holder.itemView.image)
                if (!purchasedItem.redeemed) {
                    holder.itemView.setOnClickListener {
                        itemClickListener?.invoke((getItem(bindingAdapterPosition) as PurchasedOrderItem).order)
                    }
                } else {
                    holder.itemView.setOnClickListener(null)
                }
            }
            TYPE_GROUP -> {
                val item = getItem(bindingAdapterPosition) as PurchasedGroupItem
                holder.itemView.groupTitle.text = item.title
                holder.itemView.expand.rotation = if (item.isOpened) 180f else 0f
                holder.itemView.setOnClickListener {
                    groupListener?.invoke((getItem(bindingAdapterPosition) as PurchasedGroupItem).title)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<PurchasedItem>() {
        override fun areItemsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                oldItem.type == newItem.type &&
                        when (oldItem.type) {
                            TYPE_CARD ->
                                equalsValuesBy(oldItem as PurchasedOrderItem, newItem as PurchasedOrderItem,
                                        { it.order.clientOrderId })
                            TYPE_GROUP -> equalsValuesBy(oldItem as PurchasedGroupItem, newItem as PurchasedGroupItem,
                                    { it.title })
                            else -> true
                        }


        override fun areContentsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                when (oldItem.type) {
                    TYPE_CARD ->
                        equalsValuesBy(oldItem as PurchasedOrderItem, newItem as PurchasedOrderItem,
                                { it.order.productImg }, { it.order.productName },
                                { it.order.amount }, { it.order.timestamp })
                    TYPE_GROUP -> equalsValuesBy(oldItem as PurchasedGroupItem, newItem as PurchasedGroupItem,
                            { it.title }, { it.isOpened })
                    else -> true
                }
    }

    companion object {
        const val TYPE_CARD = 0
        const val TYPE_LOADING = 1
        const val TYPE_GROUP = 2
    }
}