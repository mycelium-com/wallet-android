package com.mycelium.giftbox.purchase.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.CheckedTextView
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.coins.Value

class CustomSimpleAdapter(
    private val context: Context,
    val data: Map<Value, Boolean>
) : BaseAdapter() {
    override fun getCount(): Int = data.size

    override fun getItem(position: Int): Value = data.keys.toTypedArray()[position]

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val inflate = LayoutInflater.from(context)
            .inflate(R.layout.simple_list_item_single_choice, null)

        val textView = inflate.findViewById<CheckedTextView>(android.R.id.text1)
        val item = getItem(position)
        textView.text = item.toStringWithUnit()
        val isEnabled = data[item] ?: false
        textView.isEnabled = isEnabled
        if (!isEnabled) {
            textView.isFocusable = true
            textView.isFocusableInTouchMode = true
        }
        return inflate
    }
}