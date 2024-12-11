package com.mycelium.wallet.activity.main.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wallet.databinding.FioRequestRowBinding
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioGroup
import com.mycelium.wapi.wallet.fio.FioRequestStatus
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import fiofoundation.io.fiosdk.models.fionetworkprovider.SentFIORequestContent
import java.text.SimpleDateFormat

open class FioRequestAdapterItem(val type: Int)
data class Group(val group: FioGroup, val expanded: Boolean) : FioRequestAdapterItem(FioRequestAdapter.TYPE_GROUP)
data class FioRequest(val group: FioGroup, val request: FIORequestContent) : FioRequestAdapterItem(FioRequestAdapter.TYPE_ITEM)

class FioRequestAdapter : ListAdapter<FioRequestAdapterItem, RecyclerView.ViewHolder>(ItemListDiffCallback()) {

    var itemClickListener: ((FIORequestContent, FioGroup) -> Unit)? = null
    var groupClickListener: ((FioGroup) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                TYPE_GROUP -> FioGroupViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fio_request_listrow_group, parent, false))
                TYPE_ITEM -> FioRequestViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.fio_request_row, parent, false))
                else -> TODO("Not yet implemented")
            }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (item.type) {
            TYPE_GROUP -> {
                val group = item as Group
                val convertView = holder.itemView

                convertView.setOnClickListener {
                    groupClickListener?.invoke(group.group)
                }
                val arrow = convertView.findViewById<CheckBox>(R.id.cbArrow)
                val checkedTextView = convertView.findViewById(R.id.textView1) as TextView
                checkedTextView.text = """${group.group.status} (${group.group.children.size})"""
                arrow?.isChecked = group.expanded
            }
            TYPE_ITEM -> {
                val fioRequest = item as FioRequest
                val group = fioRequest.group
                val fioRequestContent = fioRequest.request
                holder as FioRequestViewHolder
                val content = fioRequestContent.deserializedContent
                holder.itemView.setOnClickListener {
                    itemClickListener?.invoke(fioRequestContent, group)
                }

                holder.binding.tvDirection.text = when (group.status) {
                    FioGroup.Type.SENT -> "To:"
                    FioGroup.Type.PENDING -> "From:"
                }
                holder.binding.tvAddress.text = when (group.status) {
                    FioGroup.Type.SENT -> fioRequestContent.payerFioAddress
                    FioGroup.Type.PENDING -> fioRequestContent.payeeFioAddress
                }

                var hasStatus = false;
                if (group.status == FioGroup.Type.SENT) {
                    val status = FioRequestStatus.getStatus((fioRequestContent as SentFIORequestContent).status)
                    if (status != FioRequestStatus.NONE) {
                        hasStatus = true
                        val color = ContextCompat.getColor(holder.binding.tvStatus.context,
                                when (status) {
                                    FioRequestStatus.REQUESTED -> R.color.fio_yellow
                                    FioRequestStatus.REJECTED -> R.color.red
                                    FioRequestStatus.SENT_TO_BLOCKCHAIN -> R.color.green
                                    FioRequestStatus.NONE -> R.color.green
                                })
                        holder.binding.tvStatus.text = when (status) {
                            FioRequestStatus.REQUESTED -> "Requested"
                            FioRequestStatus.REJECTED -> "Rejected"
                            FioRequestStatus.SENT_TO_BLOCKCHAIN -> "Received"
                            FioRequestStatus.NONE -> ""
                        }
                        holder.binding.tvStatus.setTextColor(color)
                        holder.binding.tvAmount.setTextColor(color)
                        holder.binding.ivStatus.setBackgroundResource(
                                when (status) {
                                    FioRequestStatus.REQUESTED -> R.drawable.ic_request_pending
                                    FioRequestStatus.REJECTED -> R.drawable.ic_request_error
                                    FioRequestStatus.SENT_TO_BLOCKCHAIN -> R.drawable.ic_request_good_to_go
                                    FioRequestStatus.NONE -> R.drawable.ic_request_item_circle_gray
                                })
                        holder.binding.ivStatus.setImageResource(
                                when (status) {
                                    FioRequestStatus.REQUESTED -> R.drawable.ic_history
                                    FioRequestStatus.REJECTED -> R.drawable.ic_close
                                    FioRequestStatus.SENT_TO_BLOCKCHAIN -> R.drawable.ic_fio_paid
                                    FioRequestStatus.NONE -> R.drawable.ic_history
                                })
                    }
                } else {
                    holder.binding.tvAmount.setTextColor(ContextCompat.getColor(holder.binding.tvStatus.context, R.color.white))
                    holder.binding.tvStatus.text = ""
                    holder.binding.tvStatus.setTextColor(ContextCompat.getColor(holder.binding.tvStatus.context, R.color.white))
                    holder.binding.ivStatus.setBackgroundResource(R.drawable.ic_request_item_circle_gray)
                    holder.binding.ivStatus.setImageResource(R.drawable.ic_history)
                }


                val date = inDate.parse(fioRequestContent.timeStamp)
                holder.binding.tvDate.text = """${outDate.format(date)}${if (hasStatus) ", " else ""}"""

                val mbwManager = MbwManager.getInstance(holder.binding.root.context)
                val requestedCurrency = (COINS.values + mbwManager.getWalletManager(false).getAssetTypes()).firstOrNull {
                    it.symbol.equals(content?.tokenCode ?: "", true)
                            && if(mbwManager.network.isTestnet) it.name.contains("test", true) else true
                }
                if(requestedCurrency != null) {
                    val amountValue = Value.valueOf(requestedCurrency, Util.strToBigInteger(requestedCurrency, content!!.amount))
                    holder.binding.tvAmount.text = amountValue.toStringWithUnit()
                    val convert = mbwManager.exchangeRateManager.get(amountValue, mbwManager.getFiatCurrency(requestedCurrency))
                    holder.binding.tvFiatAmount.text = convert?.toStringWithUnit()
                } else {
                    holder.binding.tvAmount.text = "${content?.amount} ${content?.tokenCode}"
                    holder.binding.tvFiatAmount?.text = ""
                }
                if (content?.memo?.isNotEmpty() == true) {
                    holder.binding.tvMemo.text = content.memo
                    holder.binding.tvMemo.visibility = View.VISIBLE
                } else {
                    holder.binding.tvMemo.visibility = View.GONE
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int = getItem(position).type

    class FioRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = FioRequestRowBinding.bind(itemView)
    }
    class FioGroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ItemListDiffCallback : DiffUtil.ItemCallback<FioRequestAdapterItem>() {
        override fun areItemsTheSame(oldItem: FioRequestAdapterItem, newItem: FioRequestAdapterItem): Boolean =
                oldItem.type == newItem.type && when (oldItem.type) {
                    TYPE_GROUP -> {
                        equalsValuesBy(oldItem as Group, newItem as Group,
                                { it.group.status })
                    }
                    TYPE_ITEM -> {
                        equalsValuesBy(oldItem as FioRequest, newItem as FioRequest,
                                { it.request.fioRequestId })
                    }
                    else -> TODO("Not yet implemented")
                }

        override fun areContentsTheSame(oldItem: FioRequestAdapterItem, newItem: FioRequestAdapterItem): Boolean =
                when (oldItem.type) {
                    TYPE_GROUP -> {
                        equalsValuesBy(oldItem as Group, newItem as Group,
                                { it.group.status }, { it.expanded }, { it.group.children.size })
                    }
                    TYPE_ITEM -> {
                        equalsValuesBy(oldItem as FioRequest, newItem as FioRequest,
                                { it.request.fioRequestId }, { it.request.content })
                    }
                    else -> TODO("Not yet implemented")
                }
    }

    companion object {
        const val TYPE_GROUP = 1
        const val TYPE_ITEM = 2

        private val inDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss")
        private val outDate = SimpleDateFormat("dd/MM/yyyy")
    }
}