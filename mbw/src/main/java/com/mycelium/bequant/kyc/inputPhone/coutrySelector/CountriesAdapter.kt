package com.mycelium.bequant.kyc.inputPhone.coutrySelector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.country_item.view.*

class CountriesAdapter(val itemClickListener: ItemClickListener) : ListAdapter<CountryModel, RecyclerView.ViewHolder>(CountryDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.country_item, parent, false))


    class ViewHolder(val itemView: View) : RecyclerView.ViewHolder(itemView)

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.tvAcronym.text = item.acronym
        holder.itemView.tvCountryName.text = item.name
        holder.itemView.tvCountryCode.text = "+${item.code}"
        holder.itemView.setOnClickListener {
            itemClickListener.onItemClick(item)
        }
    }

    class CountryDiffCallback : DiffUtil.ItemCallback<CountryModel>() {
        override fun areItemsTheSame(oldItem: CountryModel, newItem: CountryModel): Boolean =
                oldItem == newItem

        override fun areContentsTheSame(oldItem: CountryModel, newItem: CountryModel): Boolean =
                oldItem.acronym == newItem.acronym
    }

    interface ItemClickListener {
        fun onItemClick(countryModel: CountryModel)
    }

}