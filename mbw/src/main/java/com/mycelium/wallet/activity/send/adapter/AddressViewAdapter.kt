package com.mycelium.wallet.activity.send.adapter

import android.support.v7.widget.RecyclerView
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.model.AddressItem
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView

class AddressViewAdapter(private val dataSet: List<AddressItem>, private val paddingWidth: Int)
    : SelectableRecyclerView.Adapter<AddressViewAdapter.ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (viewType == SelectableRecyclerView.Adapter.VIEW_TYPE_ITEM) {
            // create a new view
            val v = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_address, parent, false)
            val imageView = v.findViewById<View>(R.id.rectangle) as ImageView
            val layoutParams = imageView.layoutParams as FrameLayout.LayoutParams
            layoutParams.gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            imageView.layoutParams = layoutParams
            return ViewHolder(v, this)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.list_item_padding_sender_address,
                    parent, false)

            val layoutParams = view.layoutParams as RecyclerView.LayoutParams
            layoutParams.width = paddingWidth
            view.layoutParams = layoutParams
            return ViewHolder(view, this)
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {

        if (getItemViewType(position) == SelectableRecyclerView.Adapter.VIEW_TYPE_ITEM) {
            // - get element from your dataSet at this position
            // - replace the contents of the view with that element
            val item = dataSet[position]
            holder.addressTextView?.text = item.address?.toMultiLineString() ?: ""
            holder.addressTypeTextView?.text = item.addressType
            holder.addressTypeLabelTextView?.text = item.addressTypeLabel

            // setting root background color and arrow
            val backgroundColor = when (selectedItem) {
                position -> {
                    holder.triangleImageView?.setImageResource(R.drawable.recyclerview_item_triangle_selected)
                    holder.itemView.context?.resources?.getColor(R.color.fee_recycler_item_selected)!!
                }
                else -> {
                    holder.triangleImageView?.setImageResource(0)
                    holder.itemView.context?.resources?.getColor(R.color.fee_recycler_item)!!
                }
            }

            holder.rootFrameLayout?.setBackgroundColor(backgroundColor)

        } else {
            val layoutParams = holder.itemView.layoutParams as RecyclerView.LayoutParams
            layoutParams.width = paddingWidth
            holder.itemView.layoutParams = layoutParams
        }
    }

    fun getItem(position: Int) = dataSet[position]

    override fun getItemCount() = dataSet.size

    override fun getItemViewType(position: Int) = dataSet[position].type

    override fun findIndex(selected: Any?) = dataSet.indexOf(selected)

    class ViewHolder(v: View, internal val adapter: AddressViewAdapter) : RecyclerView.ViewHolder(v) {
        // each data item is just a string in this case
        val addressTextView: TextView? = v.findViewById(R.id.address)
        var addressTypeTextView: TextView? = v.findViewById(R.id.addressType)
        var addressTypeLabelTextView: TextView? = v.findViewById(R.id.addressTypeLabel)
        var triangleImageView: ImageView? = v.findViewById(R.id.rectangle)
        var rootFrameLayout: FrameLayout? = v.findViewById(R.id.frameLayout)
    }
}