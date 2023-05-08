package com.mycelium.giftbox.cards.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import kotlinx.android.synthetic.main.listview_item_with_radiobutton.view.*

val ALL_COUNTRIES = CountryModel(WalletApplication.getInstance().getString(R.string.all_countries), "", "", 0)
val RUSSIA =  CountryModel(WalletApplication.getInstance().getString(R.string.russia), "", "RUS", 0, null, "Coming soon", false)

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
        holder.itemView.description.isVisible = item.description != null
        holder.itemView.description.text = item.description
        holder.itemView.checkbox_currency.isEnabled = item.enabled
        if (item.enabled) {
            holder.itemView.setOnClickListener {
                toggleChecked(getItem(holder.adapterPosition))
            }
        } else {
            holder.itemView.setOnClickListener(null)
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