package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.listview_item_with_radiobutton.view.*

val ALL_COUNTRIES = CountryModel("All Countries", "", "", 0)

class SelectCountriesAdapter : ListAdapter<CountryModel, RecyclerView.ViewHolder>(DiffCallback()) {
    var selected: CountryModel = ALL_COUNTRIES
    var selectedChangeListener: ((CountryModel) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.listview_item_with_radiobutton, parent, false))

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        holder.itemView.tv_currency_name.text = item.name
        holder.itemView.tv_currency_short.text = item.acronym3
        holder.itemView.checkbox_currency.isChecked = selected == item
        holder.itemView.setOnClickListener {
            toggleChecked(getItem(holder.adapterPosition))
        }
    }

    fun toggleChecked(countryModel: CountryModel) {
        selected = countryModel
        selectedChangeListener?.invoke(selected)
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