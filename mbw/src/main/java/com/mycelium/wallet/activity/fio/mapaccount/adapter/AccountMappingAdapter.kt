package com.mycelium.wallet.activity.fio.mapaccount.adapter

import android.graphics.drawable.Drawable
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.GroupViewHolder
import com.mycelium.wallet.activity.fio.mapaccount.adapter.viewholder.SubGroupViewHolder
import com.mycelium.wallet.databinding.ItemFioAccountMappingAccountBinding
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import java.util.*

open class Item(val type: Int)

class ItemGroup(val title: String = "", val accountCount: Int = 0, var isClosed: Boolean = true) : Item(AccountMappingAdapter.TYPE_GROUP)

class ItemAccount(val accountId: UUID,
                  val label: String,
                  val summary: String,
                  val icon: Drawable?,
                  val coinType: CryptoCurrency,
                  var isEnabled: Boolean = false) : Item(AccountMappingAdapter.TYPE_ACCOUNT)

class ItemSubGroup(val title: String = "") : Item(AccountMappingAdapter.TYPE_SUB_GROUP)
class ItemDivider(val bottomMargin: Int = 0) : Item(AccountMappingAdapter.TYPE_DIVIDER)

class AccountMappingAdapter : ListAdapter<Item, RecyclerView.ViewHolder>(DiffCallback()) {
    companion object {
        const val TYPE_GROUP = 0
        const val TYPE_ACCOUNT = 1
        const val TYPE_SUB_GROUP = 2
        const val TYPE_DIVIDER = 4
    }

    var selectChangeListener: ((ItemAccount) -> Unit)? = null
    var groupClickListener: ((String) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, typeView: Int): RecyclerView.ViewHolder =
            when (typeView) {
                TYPE_GROUP -> GroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fio_account_mapping_group_wrap, parent, false))
                TYPE_ACCOUNT -> AccountViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fio_account_mapping_account, parent, false))
                TYPE_SUB_GROUP -> SubGroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fio_account_mapping_sub_group, parent, false))
                TYPE_DIVIDER -> DividerViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_fio_list_divider, parent, false))
                else -> TODO("Not Implemented")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (getItemViewType(position)) {
            TYPE_GROUP -> {
                val item = getItem(position) as ItemGroup
                (holder as GroupViewHolder).run {
                    expandIcon.rotation = if (item.isClosed) 180f else 0f
                    title.text = "${item.title} (${item.accountCount})"
                    itemView.setOnClickListener {
                        item.isClosed = !item.isClosed
                        expandIcon.rotation = if (item.isClosed) 180f else 0f
                        groupClickListener?.invoke(item.title)
                    }
                }
            }
            TYPE_SUB_GROUP -> {
                val item = getItem(position) as ItemSubGroup
                (holder as SubGroupViewHolder).title.text = item.title
            }
            TYPE_ACCOUNT -> {
                val item = getItem(position) as ItemAccount
                holder as AccountViewHolder
                holder.binding.title.text = Html.fromHtml(item.label)
                holder.binding.summary.text = item.summary
                holder.binding.checkbox.setOnCheckedChangeListener(null)
                holder.binding.checkbox.isChecked = item.isEnabled
                item.icon?.let {
                    holder.binding.icon.setImageDrawable(it)
                }
                holder.binding.checkbox.setOnCheckedChangeListener { _, isChecked ->
                    (getItem(holder.adapterPosition) as ItemAccount).isEnabled = isChecked
                    selectChangeListener?.invoke(getItem(holder.adapterPosition) as ItemAccount)
                }
                holder.itemView.setOnClickListener {
                    holder.binding.checkbox.isChecked = !holder.binding.checkbox.isChecked
                }
            }
            TYPE_DIVIDER -> {
                (getItem(position) as ItemDivider).run {
                    holder.itemView.setPadding(0, 0, 0, bottomMargin)
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemFioAccountMappingAccountBinding.bind(itemView)
    }
    class DividerViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<Item>() {
        override fun areItemsTheSame(oldItem: Item, newItem: Item): Boolean =
                oldItem.type == newItem.type && oldItem == newItem

        override fun areContentsTheSame(oldItem: Item, newItem: Item): Boolean =
                when (oldItem.type) {
                    TYPE_GROUP -> {
                        oldItem as ItemGroup
                        newItem as ItemGroup
                        oldItem.title == newItem.title &&
                                oldItem.accountCount == newItem.accountCount &&
                                oldItem.isClosed == newItem.isClosed
                    }
                    TYPE_ACCOUNT -> {
                        oldItem as ItemAccount
                        newItem as ItemAccount
                        oldItem.accountId == newItem.accountId &&
                                oldItem.label == newItem.label &&
                                oldItem.isEnabled == newItem.isEnabled
                    }
                    TYPE_SUB_GROUP -> {
                        oldItem as ItemSubGroup
                        newItem as ItemSubGroup
                        oldItem.title == newItem.title
                    }
                    TYPE_DIVIDER -> true
                    else -> false
                }
    }
}