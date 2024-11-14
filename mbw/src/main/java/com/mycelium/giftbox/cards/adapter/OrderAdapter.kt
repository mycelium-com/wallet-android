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
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.models.Order
import com.mycelium.giftbox.client.models.Status
import com.mycelium.giftbox.getDateString
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemGiftboxPurchacedBinding
import com.mycelium.wallet.databinding.ItemGiftboxPurchacedGroupBinding
import java.math.BigDecimal

abstract class PurchasedItem(val type: Int)
data class PurchasedOrderItem(val order: MCOrderResponse, val redeemed: Boolean = false) : PurchasedItem(OrderAdapter.TYPE_CARD)
data class PurchasedGroupItem(val title: String, val isOpened: Boolean = true) : PurchasedItem(OrderAdapter.TYPE_GROUP)
object PurchasedLoadingItem : PurchasedItem(OrderAdapter.TYPE_LOADING)

class OrderAdapter : ListAdapter<PurchasedItem, RecyclerView.ViewHolder>(DiffCallback()) {

    var itemClickListener: ((MCOrderResponse) -> Unit)? = null
    var groupListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_CARD -> CardViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_giftbox_purchaced, parent, false)).apply {
                    binding.more.visibility = GONE
                    binding.descriptionLabel.visibility = GONE
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
                holder as CardViewHolder
                holder.binding.title.text = item.product?.name
                val amount = (item.faceValue ?: BigDecimal.ZERO) * item.quantity
                holder.binding.description.text = "${amount.stripTrailingZeros().toPlainString()} ${item.product?.currency}"
                holder.binding.additional.text = when (item.status) {
                     Status.PENDING -> {
                        holder.binding.additionalLabel.visibility = GONE
                        holder.binding.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_history, 0, 0, 0)
                        val color = holder.itemView.context.resources.getColor(R.color.giftbox_yellow)
                        holder.binding.additional.setTextColor(color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            holder.binding.additional.compoundDrawableTintList = ColorStateList.valueOf(color)
                        }
                        holder.itemView.context.getString(R.string.payment_in_progress)
                    }
                    Status.eRROR -> {
                        holder.binding.additionalLabel.visibility = GONE
                        holder.binding.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_failed, 0, 0, 0)
                        val color = holder.itemView.context.resources.getColor(R.color.fio_white_alpha_0_9)
                        holder.binding.additional.setTextColor(color)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            holder.binding.additional.compoundDrawableTintList = ColorStateList.valueOf(color)
                        }
                        holder.itemView.context.getString(R.string.failed)
                    }
                    else -> {
                        holder.binding.additionalLabel.visibility = View.VISIBLE
                        holder.binding.additional.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, 0, 0)
                        holder.binding.additional.setTextColor(holder.itemView.context.resources.getColor(R.color.giftbox_green))
                        item.createdDate?.getDateString(holder.itemView.resources)
                    }
                }
                holder.itemView.isEnabled = !purchasedItem.redeemed
                Glide.with(holder.binding.image)
                        .load(item.product?.logoUrl)
                        .apply(RequestOptions()
                                .transforms(CenterCrop(), RoundedCorners(holder.itemView.resources.getDimensionPixelSize(R.dimen.giftbox_small_corner))))
                        .into(holder.binding.image)
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
                holder as GroupViewHolder
                holder.binding.groupTitle.text = item.title
                holder.binding.expand.rotation = if (item.isOpened) 180f else 0f
                holder.itemView.setOnClickListener {
                    groupListener?.invoke((getItem(bindingAdapterPosition) as PurchasedGroupItem).title)
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
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<PurchasedItem>() {
        override fun areItemsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                oldItem.type == newItem.type &&
                        when (oldItem.type) {
                            TYPE_CARD ->
                                equalsValuesBy(oldItem as PurchasedOrderItem, newItem as PurchasedOrderItem,
                                        { it.order.orderId })
                            TYPE_GROUP -> equalsValuesBy(oldItem as PurchasedGroupItem, newItem as PurchasedGroupItem,
                                    { it.title })
                            else -> true
                        }


        override fun areContentsTheSame(oldItem: PurchasedItem, newItem: PurchasedItem): Boolean =
                when (oldItem.type) {
                    TYPE_CARD ->
                        equalsValuesBy(oldItem as PurchasedOrderItem, newItem as PurchasedOrderItem,
                                { it.order.product?.logoUrl }, { it.order.product?.name },
                                { it.order.faceValue }, { it.order.createdDate })
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