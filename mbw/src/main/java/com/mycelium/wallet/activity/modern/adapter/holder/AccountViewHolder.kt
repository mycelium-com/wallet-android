package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.databinding.RecordRowBinding

class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val binding = RecordRowBinding.bind(itemView)
    val llAddress: View = binding.llAddress
    val tvLabel: TextView = binding.tvLabel
    val tvWhatIsIt: TextView = binding.tvWhatIsIt
    val icon: ImageView = binding.ivIcon
    val tvAddress: TextView = binding.tvAddress
    val tvAccountType: TextView = binding.tvAccountType
    val tvBalance: TextView = binding.tvBalance
    val backupMissing: TextView = binding.tvBackupMissingWarning
    val tvProgress: TextView = binding.tvProgress
    val layoutProgressTxRetreived: View = binding.progressTxRetreived
    val tvProgressLayout: View = binding.tvProgressLayout
    val ivWhatIsSync: ImageView = binding.ivWhatIsSync
    val progressBar: ProgressBar = binding.progressBar
    val lastSyncState: View = binding.lastSyncState
    val tvTraderKey: View = binding.tvTraderKey
}