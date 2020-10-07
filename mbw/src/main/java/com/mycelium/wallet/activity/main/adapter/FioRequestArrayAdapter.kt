package com.mycelium.wallet.activity.main.adapter

import android.app.Activity
import android.util.Log
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
import com.mycelium.wapi.wallet.coins.COINS
import com.mycelium.wapi.wallet.coins.CryptoCurrency
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.fio.FioGroup
import fiofoundation.io.fiosdk.models.fionetworkprovider.FIORequestContent
import java.math.BigDecimal
import java.math.BigInteger
import java.util.*


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
        val children = getChild(groupPosition, childPosition) as FIORequestContent
        val group = getGroup(groupPosition)

        val fioRequestView = convertView
                ?: activity.layoutInflater.inflate(R.layout.fio_request_row, null)!!
        val content = children.deserializedContent


        val isIncoming = true //content?.payeeTokenPublicAddress

        val color = if (isIncoming) R.color.green else R.color.red
        val direction = fioRequestView.findViewById<TextView>(R.id.tvDirection)
        direction?.text = if (isIncoming) "From:" else "To:"
        val address = fioRequestView.findViewById<TextView>(R.id.tvAddress)
        address?.text = children.payeeFioAddress

        val ivStatus = fioRequestView.findViewById<ImageView>(R.id.ivStatus)

        when (group.status) {
            FioGroup.Type.SENT -> {
//                ivStatus?.setBackgroundResource(if (isError) R.drawable.ic_request_good_to_go else R.drawable.ic_request_error)
            }
            FioGroup.Type.PENDING -> {
//                ivStatus?.setBackgroundResource(if (isIncoming) R.drawable.ic_request_arrow_down else R.drawable.ic_request_arrow_up)
            }
        }

        val memo = fioRequestView.findViewById<TextView>(R.id.tvTransactionLabel)
        memo?.text = content?.memo
        val amount = fioRequestView.findViewById<TextView>(R.id.tvAmount)
        val requestedCurrency = COINS.values.firstOrNull { it.symbol.equals(content?.chainCode ?: "", true) }
                ?: return fioRequestView
        val amountValue = Value.valueOf(requestedCurrency, strToBigInteger(requestedCurrency, content!!.amount))
        amount?.text = amountValue.toStringWithUnit()
        amount?.setTextColor(ContextCompat.getColor(activity, color))
        val convert = convert(amountValue, Utils.getTypeByName(CurrencyCode.USD.shortString)!!)
        val tvFiatAmount = fioRequestView.findViewById<TextView>(R.id.tvFiatAmount)
        tvFiatAmount?.text = convert?.toStringWithUnit()

        return fioRequestView
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

        val group = getGroup(groupPosition) as FioGroup

        if (convertView == null) {
            val inflater = activity.layoutInflater
            convertView = inflater.inflate(R.layout.fio_request_listrow_group, null)

            convertView?.setOnClickListener{
                val expandableListView = convertView?.getParent() as ExpandableListView
                if (!expandableListView.isGroupExpanded(groupPosition)) {
                    expandableListView.expandGroup(groupPosition)
                } else {
                    expandableListView.collapseGroup(groupPosition)
                }
            }
        }
        val arrow = convertView?.findViewById<CheckBox>(R.id.cbArrow)
        val checkedTextView = convertView?.findViewById(R.id.textView1) as TextView
        checkedTextView.text = group.status.toString()
        arrow?.isChecked = isExpanded
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

    private fun strToBigInteger(coinType: CryptoCurrency, amountStr: String): BigInteger =
            BigDecimal(amountStr).movePointRight(coinType.unitExponent).toBigIntegerExact()
}
