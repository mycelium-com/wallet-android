package com.mycelium.wallet.activity.main.adapter

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.api.lib.CurrencyCode
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioGroup
import com.mycelium.wapi.wallet.fio.coins.FIOMain
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import java.math.BigInteger


class FioRequestArrayAdapter(var activity: Activity,
                             private val groups: List<FioGroup>,
                             val mbwManager: MbwManager) : BaseExpandableListAdapter() {

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
        val group = getGroup(groupPosition)

        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.fio_request_row, null)
        }
        val content = children.deserializedContent


        val isIncoming = true //content?.payeeTokenPublicAddress
        val isError = false

        val color = if (isIncoming) R.color.green else R.color.red
        val direction = convertView?.findViewById<TextView>(R.id.tvDirection)
        direction?.text = if (isIncoming) "From:" else "To:"
        val address = convertView?.findViewById<TextView>(R.id.tvAddress)
        address?.text = children.payeeFioAddress

        val ivStatus = convertView?.findViewById<ImageView>(R.id.ivStatus)

        when (group.status) {
            FioGroup.Type.SENT -> {
//                ivStatus?.setBackgroundResource(if (isError) R.drawable.ic_request_good_to_go else R.drawable.ic_request_error)
            }
            FioGroup.Type.PENDING -> {
//                ivStatus?.setBackgroundResource(if (isIncoming) R.drawable.ic_request_arrow_down else R.drawable.ic_request_arrow_up)
            }
        }

        val memo = convertView?.findViewById<TextView>(R.id.tvTransactionLabel)
        memo?.text = content?.memo
        val amount = convertView?.findViewById<TextView>(R.id.tvAmount)
        val btc = Value.valueOf(FIOMain, content?.amount?.toBigInteger()
                ?: BigInteger.ZERO)
        amount?.text = btc.toStringWithUnit()
        amount?.setTextColor(ContextCompat.getColor(activity, color))
        val convert = convert(btc, Utils.getTypeByName(CurrencyCode.USD.shortString)!!)
        val tvFiatAmount = convertView?.findViewById<TextView>(R.id.tvFiatAmount)
        tvFiatAmount?.text = convert?.toStringWithUnit()

        return convertView!!
    }

    override fun getChildrenCount(groupPosition: Int): Int {
        return groups[groupPosition].children.size
    }

    override fun getGroup(groupPosition: Int): FioGroup {
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
        val listView = parent as ExpandableListView
        listView.expandGroup(groupPosition);
        return convertView
    }

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun isChildSelectable(groupPosition: Int, childPosition: Int): Boolean {
        return true
    }


    private fun convert(value: Value, assetInfo: AssetInfo): Value? {
        return mbwManager.exchangeRateManager.get(value, assetInfo)
    }
}