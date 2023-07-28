package com.mycelium.wallet.activity.main.adapter

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemQuadAdsBinding
import com.mycelium.wallet.external.partner.model.BuySellButton

class QuadAdsAdapter : ListAdapter<BuySellButton, RecyclerView.ViewHolder>(QuadDiffCallback()) {
    private var clickListener: ((BuySellButton) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == BUTTON) {
            ButtonHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_quad_ads, parent, false)
            )
        } else {
            SpaceHolder(
                LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_space, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == BUTTON) {
            val actionButton = getItem(position)
            val button = (holder as ButtonHolder)
            button.binding.text.text = actionButton!!.name
            Glide.with(button.itemView)
                .load(actionButton.iconUrl)
                .into(button.binding.image)
            button.itemView.setOnClickListener(View.OnClickListener {
                clickListener?.invoke(actionButton)
            })
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position) == null) {
            SPACE
        } else {
            BUTTON
        }
    }

    class ButtonHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemQuadAdsBinding.bind(itemView)
    }

    private inner class SpaceHolder(itemView: View?) : RecyclerView.ViewHolder(
        itemView!!
    )

    companion object {
        private const val SPACE = 10
        private const val BUTTON = 1
    }

    class QuadDiffCallback : DiffUtil.ItemCallback<BuySellButton>() {
        override fun areItemsTheSame(oldItem: BuySellButton, newItem: BuySellButton): Boolean {
            TODO("Not yet implemented")
        }

        override fun areContentsTheSame(oldItem: BuySellButton, newItem: BuySellButton): Boolean {
            TODO("Not yet implemented")
        }

    }
}