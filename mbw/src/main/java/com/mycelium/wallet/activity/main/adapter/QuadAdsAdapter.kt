package com.mycelium.wallet.activity.main.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.ItemQuadAdsBinding
import com.mycelium.wallet.external.partner.model.BuySellButton

class QuadAdsAdapter : ListAdapter<BuySellButton, RecyclerView.ViewHolder>(QuadDiffCallback()) {
    private var clickListener: ((BuySellButton) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
        ButtonHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_quad_ads, parent, false)
        )

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
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

    class ButtonHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemQuadAdsBinding.bind(itemView)
    }

    class QuadDiffCallback : DiffUtil.ItemCallback<BuySellButton>() {
        override fun areItemsTheSame(oldItem: BuySellButton, newItem: BuySellButton): Boolean =
            equalsValuesBy(oldItem, newItem, { it.link }, { it.link })

        override fun areContentsTheSame(oldItem: BuySellButton, newItem: BuySellButton): Boolean =
            equalsValuesBy(oldItem, newItem, { it.name }, { it.link }, { it.iconUrl })

    }
}