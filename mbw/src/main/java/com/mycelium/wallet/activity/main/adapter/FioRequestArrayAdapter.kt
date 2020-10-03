package com.mycelium.wallet.activity.main.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseExpandableListAdapter
import android.widget.CheckedTextView
import android.widget.TextView
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.fio.FioGroup
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent

class FioRequestArrayAdapter(var activity: Activity,
                             private val groups: MutableList<FioGroup>) : BaseExpandableListAdapter() {

    override fun getChild(groupPosition: Int, childPosition: Int): Any {
        return groups[groupPosition].children[childPosition]
    }

    override fun getChildId(groupPosition: Int, childPosition: Int): Long {
        return 0
    }

    override fun getChildView(groupPosition: Int, childPosition: Int,
                              isLastChild: Boolean, convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        val children = getChild(groupPosition, childPosition) as FIORequestContent
        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.fio_request_row, null)
        }
        val content = children.deserializedContent

        val address = convertView?.findViewById<TextView>(R.id.tvAddress)
        address?.text = String.format("From: %s", children.payeeFioAddress)
        val memo = convertView?.findViewById<TextView>(R.id.tvTransactionLabel)
        memo?.text = content?.memo
        val amount = convertView?.findViewById<TextView>(R.id.tvAmount)
        amount?.text = content?.amount
        val tvFiatAmount = convertView?.findViewById<TextView>(R.id.tvFiatAmount)
        tvFiatAmount?.text = content?.amount
        return convertView!!
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return groups[groupPosition].children.size
    }

    override fun getGroup(groupPosition: Int): Any {
        return groups[groupPosition]
    }

    override fun getGroupCount(): Int {
        return groups.size
    }

    override fun onGroupCollapsed(groupPosition: Int) {
        super.onGroupCollapsed(groupPosition)
    }

    override fun onGroupExpanded(groupPosition: Int) {
        super.onGroupExpanded(groupPosition)
    }

    override fun getGroupId(groupPosition: Int): Long {
        return 0
    }

    override fun getGroupView(groupPosition: Int, isExpanded: Boolean,
                              convertView: View?, parent: ViewGroup): View {
        var convertView = convertView
        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.fio_request_listrow_group, null)
        }
        val group = getGroup(groupPosition) as FioGroup
        val checkedTextView = convertView as CheckedTextView
        checkedTextView.text = group.status.toString()
        checkedTextView.isChecked = isExpanded
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }
}