package com.mycelium.bequant.kyc.steps.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.remote.model.KYCDocument
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_kyc_document.view.*

enum class LoadStatus {
    INIT, LOADING, FAILED, LOADED
}

class Document(val image: Bitmap, val name: String,
               var docType:KYCDocument,
               var url: String?,
               var size: Long = 0, var progress: Int = 0,
               var loadStatus: LoadStatus? = LoadStatus.INIT)


class DocumentAdapter() : ListAdapter<Document, RecyclerView.ViewHolder>(DocumentDiffCallback()) {
    var removeListner: ((Document) -> Unit)? = null
    var viewListener: ((Document) -> Unit)? = null
    var reloadListener: ((Document) -> Unit)? = null
    var listChangeListener: ((List<Document>) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_kyc_document, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.itemView.image.setImageBitmap(item.image)
        viewHolder.itemView.name.text = item.name
        viewHolder.itemView.uploadProgress.progress = item.progress
        if (item.loadStatus == LoadStatus.FAILED) {
            viewHolder.itemView.reload.visibility = VISIBLE
            viewHolder.itemView.name.text = "Upload failed"
            viewHolder.itemView.size.text = "Try again"
            viewHolder.itemView.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_red))
            viewHolder.itemView.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_red))
            viewHolder.itemView.uploadProgress.visibility = VISIBLE
            viewHolder.itemView.uploadProgress.progress = 100
            viewHolder.itemView.uploadProgress.progressDrawable = viewHolder.itemView.resources.getDrawable(R.drawable.bequantprogress_failed)
        } else if (item.loadStatus == LoadStatus.LOADING) {
            viewHolder.itemView.reload.visibility = GONE
            viewHolder.itemView.size.text = viewHolder.itemView.resources.getString(R.string.loading)
            viewHolder.itemView.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.white))
            viewHolder.itemView.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_gray_6))
            viewHolder.itemView.uploadProgress.visibility = VISIBLE
            viewHolder.itemView.uploadProgress.progressDrawable = viewHolder.itemView.resources.getDrawable(R.drawable.bequantprogress)
        } else {
            viewHolder.itemView.reload.visibility = GONE
            viewHolder.itemView.size.text = "%.1f MB".format(item.size / 1000000f)
            viewHolder.itemView.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.white))
            viewHolder.itemView.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_gray_6))
            viewHolder.itemView.uploadProgress.visibility = View.GONE
        }
        viewHolder.itemView.reload.setOnClickListener {
            reloadListener?.invoke(item)
        }
        viewHolder.itemView.remove.setOnClickListener {
            submitList(currentList - item)
            removeListner?.invoke(item)
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<Document>, currentList: MutableList<Document>) {
        super.onCurrentListChanged(previousList, currentList)
        listChangeListener?.invoke(currentList)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem.name == newItem.name
                        && oldItem.size == newItem.size
                        && oldItem.progress == newItem.progress
                        && oldItem.loadStatus == newItem.loadStatus
//                        && oldItem.image == newItem.image
    }
}