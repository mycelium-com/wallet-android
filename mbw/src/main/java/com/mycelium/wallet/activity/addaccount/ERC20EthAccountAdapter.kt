package com.mycelium.wallet.activity.addaccount

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.checked_item.view.*


class ERC20EthAccountAdapter(context: Context, resource: Int) : ArrayAdapter<String>(context, resource) {
    var selected = 0
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).apply {
                checkedText.isChecked = position == selected
                this.setOnClickListener {
                    selected = position
                    checkedText.isChecked = true
                    notifyDataSetChanged()
                }
            }
}