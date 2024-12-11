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
import com.mycelium.wallet.databinding.ItemBequantKycDocumentBinding
import java.io.File

enum class LoadStatus {
    INIT, LOADING, FAILED, LOADED
}

class Document(val image: Bitmap, val name: String,
               var docType:KYCDocument,
               var url: String?,
               var progress: Int = 0,
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
        viewHolder as ViewHolder
        viewHolder.binding.image.setImageBitmap(item.image)
        viewHolder.binding.name.text = item.name
        viewHolder.binding.uploadProgress.progress = item.progress
        if (item.loadStatus == LoadStatus.FAILED) {
            viewHolder.binding.reload.visibility = VISIBLE
            viewHolder.binding.name.text = "Upload failed"
            viewHolder.binding.size.text = "Try again"
            viewHolder.binding.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_red))
            viewHolder.binding.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_red))
            viewHolder.binding.uploadProgress.visibility = VISIBLE
            viewHolder.binding.uploadProgress.progress = 100
            viewHolder.binding.uploadProgress.progressDrawable = viewHolder.itemView.resources.getDrawable(R.drawable.bequantprogress_failed)
        } else if (item.loadStatus == LoadStatus.LOADING) {
            viewHolder.binding.reload.visibility = GONE
            viewHolder.binding.size.text = viewHolder.itemView.resources.getString(R.string.loading)
            viewHolder.binding.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.white))
            viewHolder.binding.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_gray_6))
            viewHolder.binding.uploadProgress.visibility = VISIBLE
            viewHolder.binding.uploadProgress.progressDrawable = viewHolder.itemView.resources.getDrawable(R.drawable.bequantprogress)
        } else {
            viewHolder.binding.reload.visibility = GONE
            viewHolder.binding.size.text = "%.1f MB".format(File(item.url).length() / 1000000f)
            viewHolder.binding.name.setTextColor(viewHolder.itemView.resources.getColor(R.color.white))
            viewHolder.binding.size.setTextColor(viewHolder.itemView.resources.getColor(R.color.bequant_gray_6))
            viewHolder.binding.uploadProgress.visibility = View.GONE
        }
        viewHolder.binding.reload.setOnClickListener {
            reloadListener?.invoke(item)
        }
        viewHolder.binding.remove.setOnClickListener {
            submitList(currentList - item)
            removeListner?.invoke(item)
        }
    }

    override fun onCurrentListChanged(previousList: MutableList<Document>, currentList: MutableList<Document>) {
        super.onCurrentListChanged(previousList, currentList)
        listChangeListener?.invoke(currentList)
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemBequantKycDocumentBinding.bind(itemView)
    }
    class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem.name == newItem.name
                        && oldItem.progress == newItem.progress
                        && oldItem.loadStatus == newItem.loadStatus
//                        && oldItem.image == newItem.image
    }
}