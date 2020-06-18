package com.mycelium.bequant.kyc.steps.adapter

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.item_bequant_kyc_document.view.*


class Document(val image: Bitmap, val name: String,
               var url: String?,
               var size: Long = 0, var progress: Int = 0)

class DocumentAdapter() : ListAdapter<Document, RecyclerView.ViewHolder>(DocumentDiffCallback()) {
    var removeListner: ((Document) -> Unit)? = null
    var viewListener: ((Document) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_bequant_kyc_document, parent, false))

    override fun onBindViewHolder(viewHolder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        viewHolder.itemView.image.setImageBitmap(item.image)
        viewHolder.itemView.name.text = item.name
        viewHolder.itemView.uploadProgress.progress = item.progress
        if (item.progress < 100) {
            viewHolder.itemView.size.text = viewHolder.itemView.resources.getString(R.string.loading)
            viewHolder.itemView.uploadProgress.visibility = View.VISIBLE
        } else {
            viewHolder.itemView.size.text = "%.1f MB".format(item.size / 1000000f)
            viewHolder.itemView.uploadProgress.visibility = View.GONE
        }
        viewHolder.itemView.remove.setOnClickListener {
            submitList(currentList - item)
            removeListner?.invoke(item)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    class DocumentDiffCallback : DiffUtil.ItemCallback<Document>() {
        override fun areItemsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: Document, newItem: Document): Boolean =
                oldItem.name == newItem.name
                        && oldItem.size == newItem.size
                        && oldItem.progress == newItem.progress
//                        && oldItem.image == newItem.image
    }
}