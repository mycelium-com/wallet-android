package com.mycelium.wallet.activity.changelog.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel.LatestRelease
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel.Release
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel.Change
import com.mycelium.wallet.activity.changelog.adapter.ChangeLogViewHolder.LatestReleaseViewHolder
import com.mycelium.wallet.activity.changelog.adapter.ChangeLogViewHolder.ReleaseViewHolder
import com.mycelium.wallet.activity.changelog.adapter.ChangeLogViewHolder.ChangeViewHolder
import com.mycelium.wallet.databinding.AdapterItemChangeLogChangeBinding
import com.mycelium.wallet.databinding.AdapterItemChangeLogLatestReleaseBinding
import com.mycelium.wallet.databinding.AdapterItemChangeLogReleaseBinding

internal class ChangeLogListAdapter : ListAdapter<ChangeLogUiModel, ViewHolder>(ItemCallback) {

    override fun getItemViewType(position: Int): Int = getItem(position).type

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            LatestRelease.VIEW_TYPE -> AdapterItemChangeLogLatestReleaseBinding
                .inflate(inflater, parent, false).let(::LatestReleaseViewHolder)
            Release.VIEW_TYPE -> AdapterItemChangeLogReleaseBinding.inflate(inflater, parent, false)
                .let(::ReleaseViewHolder)
            Change.VIEW_TYPE ->
                AdapterItemChangeLogChangeBinding.inflate(inflater, parent, false).let(::ChangeViewHolder)
            else -> error("View type $viewType not supported")
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = getItem(holder.absoluteAdapterPosition)
        when (holder.itemViewType) {
            LatestRelease.VIEW_TYPE -> (holder as LatestReleaseViewHolder).bind(item as LatestRelease)
            Release.VIEW_TYPE -> (holder as ReleaseViewHolder).bind(item as Release)
            Change.VIEW_TYPE -> (holder as ChangeViewHolder).bind(item as Change)
            else -> error("View type ${holder.itemViewType} not supported")
        }
    }

    private object ItemCallback : DiffUtil.ItemCallback<ChangeLogUiModel>() {
        private val ChangeLogUiModel.comparableValue
            get() = when (this) {
                is Change -> change
                is LatestRelease -> version
                is Release -> version
            }

        override fun areItemsTheSame(oldItem: ChangeLogUiModel, newItem: ChangeLogUiModel): Boolean =
            oldItem.type == newItem.type && oldItem::class == newItem::class &&
                    oldItem.comparableValue == newItem.comparableValue

        override fun areContentsTheSame(oldItem: ChangeLogUiModel, newItem: ChangeLogUiModel): Boolean =
            oldItem == newItem
    }
}
