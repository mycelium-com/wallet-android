package com.mycelium.wallet.activity.addaccount

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import com.mycelium.wallet.R


class ERC20EthAccountAdapter(context: Context, resource: Int) : ArrayAdapter<String>(context, resource) {
    var selected = 0
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            super.getView(position, convertView, parent).apply {
                val checkedText = this.findViewById<CheckedTextView>(R.id.checkedText)
                checkedText.isChecked = position == selected
                this.setOnClickListener {
                    selected = position
                    checkedText.isChecked = true
                    notifyDataSetChanged()
                }
            }
}