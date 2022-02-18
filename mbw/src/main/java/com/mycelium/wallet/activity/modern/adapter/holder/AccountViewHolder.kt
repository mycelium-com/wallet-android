package com.mycelium.wallet.activity.modern.adapter.holder

import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.record_row.view.*

class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    val llAddress: View = itemView.llAddress
    val tvLabel: TextView = itemView.tvLabel
    val tvWhatIsIt: TextView = itemView.tvWhatIsIt
    val icon: ImageView = itemView.ivIcon
    val tvAddress: TextView = itemView.tvAddress
    val tvAccountType: TextView = itemView.tvAccountType
    val tvBalance: TextView = itemView.tvBalance
    val backupMissing: TextView = itemView.tvBackupMissingWarning
    val tvProgress: TextView = itemView.tvProgress
    val layoutProgressTxRetreived: View = itemView.progressTxRetreived
    val tvProgressLayout: View = itemView.tvProgressLayout
    val ivWhatIsSync: ImageView = itemView.ivWhatIsSync
    val progressBar: ProgressBar = itemView.progress_bar
    val lastSyncState: View = itemView.lastSyncState
    val tvTraderKey: View = itemView.tvTraderKey
}