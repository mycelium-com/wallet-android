package com.mycelium.wallet.activity.modern.adapter

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import com.mycelium.wallet.external.mediaflow.model.Category
import kotlinx.android.synthetic.main.item_media_flow_filter.view.*


class MediaFlowFilterAdapter(private val filters: List<Category>) : RecyclerView.Adapter<MediaFlowFilterAdapter.Holder>() {

    val checked = mutableSetOf<String>()
    var checkListener: (() -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_media_flow_filter, parent, false))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        if (position == 0) {
            holder.itemView.checkbox.text = holder.itemView.resources.getString(R.string.select_all)
            holder.itemView.checkbox.setOnCheckedChangeListener(null)
            holder.itemView.checkbox.isChecked = checked.size == filters.size
            holder.itemView.checkbox.setOnCheckedChangeListener { _, b ->
                if (b) {
                    selectAll()
                } else {
                    deselectAll()
                }
                checkListener?.invoke()
            }
        } else {
            holder.itemView.checkbox.text = filters[position - 1].name
            holder.itemView.checkbox.setOnCheckedChangeListener(null)
            holder.itemView.checkbox.isChecked = checked.contains(filters[position - 1].name)
            holder.itemView.checkbox.setOnCheckedChangeListener { _, b ->
                if (b) {
                    checked.add(filters[holder.adapterPosition - 1].name)
                } else {
                    checked.remove(filters[holder.adapterPosition - 1].name)
                }
                holder.itemView.post { notifyItemChanged(0) }
                checkListener?.invoke()
            }
        }
    }

    private fun deselectAll() {
        checked.clear()
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return filters.size + 1
    }

    private fun selectAll() {
        checked.clear()
        filters.forEach {
            checked.add(it.name)
        }
        notifyDataSetChanged()
    }

    class Holder(itemView: View) : RecyclerView.ViewHolder(itemView)

}
