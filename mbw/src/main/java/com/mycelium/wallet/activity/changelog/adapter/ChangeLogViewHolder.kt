package com.mycelium.wallet.activity.changelog.adapter

import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.changelog.ChangeLogUiModel
import com.mycelium.wallet.databinding.AdapterItemChangeLogChangeBinding
import com.mycelium.wallet.databinding.AdapterItemChangeLogLatestReleaseBinding
import com.mycelium.wallet.databinding.AdapterItemChangeLogReleaseBinding

internal sealed class ChangeLogViewHolder<Binding, Data>(
    protected val binding: Binding
) : RecyclerView.ViewHolder(binding.root) where Binding : ViewBinding, Data : ChangeLogUiModel {

    abstract fun bind(data: Data)

    class LatestReleaseViewHolder(
        binding: AdapterItemChangeLogLatestReleaseBinding
    ) : ChangeLogViewHolder<AdapterItemChangeLogLatestReleaseBinding, ChangeLogUiModel.LatestRelease>(binding) {

        override fun bind(data: ChangeLogUiModel.LatestRelease) {
            binding.versionTextView.apply {
                text = resources.getString(R.string.changelog_version_placeholder, data.version)
            }
        }
    }

    class ReleaseViewHolder(
        binding: AdapterItemChangeLogReleaseBinding
    ) : ChangeLogViewHolder<AdapterItemChangeLogReleaseBinding, ChangeLogUiModel.Release>(binding) {

        override fun bind(data: ChangeLogUiModel.Release) {
            binding.root.apply {
                text = resources.getString(R.string.changelog_version_placeholder, data.version)
            }
        }
    }

    class ChangeViewHolder(
        binding: AdapterItemChangeLogChangeBinding
    ) : ChangeLogViewHolder<AdapterItemChangeLogChangeBinding, ChangeLogUiModel.Change>(binding) {

        override fun bind(data: ChangeLogUiModel.Change) {
            binding.titleTextView.text = data.change
        }
    }
}
