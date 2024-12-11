package com.mycelium.bequant.kyc.steps.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemBequantStepBinding

class ItemStep(val step: Int, val stepName: String, val stepState: StepState)

class StepAdapter : ListAdapter<ItemStep, RecyclerView.ViewHolder>(ItemListDiffCallback()) {
    var clickListener: ((Int) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_step, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder as ViewHolder
        if (position == 0) {
            viewHolder.binding.stepView.showConnectorLine = false
        }
        viewHolder.binding.stepView.title = item.stepName
        viewHolder.binding.stepView.number = item.step
        viewHolder.binding.stepView.state = item.stepState
        viewHolder.binding.stepView.update()
        when (item.stepState) {
            StepState.COMPLETE_EDITABLE, StepState.ERROR ->
                viewHolder.itemView.setOnClickListener {
                    clickListener?.invoke(getItem(viewHolder.adapterPosition).step)
                }

            else -> {}
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemBequantStepBinding.bind(itemView)
    }

    class ItemListDiffCallback : DiffUtil.ItemCallback<ItemStep>() {
        override fun areItemsTheSame(oldItem: ItemStep, newItem: ItemStep): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: ItemStep, newItem: ItemStep): Boolean =
                oldItem.step == newItem.step
                        && oldItem.stepName == newItem.stepName
                        && oldItem.stepState == newItem.stepState
    }
}