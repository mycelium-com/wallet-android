package com.mycelium.bequant.kyc.inputPhone.coutrySelector

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.CountryItemBinding

class CountriesAdapter(val itemClickListener: ItemClickListener) : ListAdapter<CountryModel, RecyclerView.ViewHolder>(CountryDiffCallback()) {
    var showPhoneCode = true
    var nationality = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.country_item, parent, false)).apply {
                binding.tvCountryCode.visibility = if (showPhoneCode) View.VISIBLE else View.GONE
                binding.tvAcronym.visibility = if (nationality) View.GONE else View.VISIBLE
            }


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = CountryItemBinding.bind(itemView)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder as ViewHolder
        if (nationality) {
            holder.binding.tvCountryName.text = item.nationality
        } else {
            holder.binding.tvAcronym.text = item.acronym
            holder.binding.tvCountryName.text = item.name
        }
        holder.binding.tvCountryCode.text = "+${item.code}"
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