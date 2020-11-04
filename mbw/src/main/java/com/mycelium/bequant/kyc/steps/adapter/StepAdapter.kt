package com.mycelium.bequant.kyc.steps.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_step.view.*

class ItemStep(val step: Int, val stepName: String, val stepState: StepState)

class StepAdapter : ListAdapter<ItemStep, RecyclerView.ViewHolder>(ItemListDiffCallback()) {
    var clickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_step, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        if (position == 0) {
            viewHolder.itemView.stepView.showConnectorLine = false
        }
        viewHolder.itemView.stepView.title = item.stepName
        viewHolder.itemView.stepView.number = item.step
        viewHolder.itemView.stepView.state = item.stepState
        viewHolder.itemView.stepView.update()
        when (item.stepState) {
            StepState.COMPLETE_EDITABLE, StepState.ERROR ->
                viewHolder.itemView.setOnClickListener {
                    clickListener?.invoke(getItem(viewHolder.adapterPosition).step)
                }
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class ItemListDiffCallback : DiffUtil.ItemCallback<ItemStep>() {
        override fun areItemsTheSame(oldItem: ItemStep, newItem: ItemStep): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: ItemStep, newItem: ItemStep): Boolean =
                oldItem.step == newItem.step
                        && oldItem.stepName == newItem.stepName
                        && oldItem.stepState == newItem.stepState
    }
}