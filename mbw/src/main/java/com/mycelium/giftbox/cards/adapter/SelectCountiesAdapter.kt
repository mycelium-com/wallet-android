package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.listview_item_with_checkbox.view.*


class SelectCountiesAdapter : ListAdapter<CountryModel, RecyclerView.ViewHolder>(DiffCallback()) {
    val selected = mutableSetOf<CountryModel>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.listview_item_with_checkbox, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.tv_currency_name.text = item.name
        holder.itemView.tv_currency_short.text = item.acronym3
        holder.itemView.checkbox_currency.isChecked = selected.contains(item)
        holder.itemView.setOnClickListener {
            toggleChecked(getItem(holder.adapterPosition))
        }
    }

    fun toggleChecked(countryModel: CountryModel) {
        if (selected.contains(countryModel)) {
            selected.removeAll { it.code == 0 }
            selected.remove(countryModel)
        } else {
            selected.add(countryModel)
        }
        if (countryModel.code == 0) {
            if (selected.contains(countryModel)) {
                selected.addAll(currentList)
            } else {
                selected.clear()
            }
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)

    class DiffCallback : DiffUtil.ItemCallback<CountryModel>() {
        override fun areItemsTheSame(oldItem: CountryModel, newItem: CountryModel): Boolean =
                oldItem == newItem


        override fun areContentsTheSame(oldItem: CountryModel, newItem: CountryModel): Boolean =
                oldItem == newItem
    }
}