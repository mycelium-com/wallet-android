package com.mycelium.wallet.activity

import android.os.Bundle
import android.util.TypedValue
import android.view.View
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wallet.activity.util.toString
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.coins.Value

open class GenericDetailsFragment : Fragment() {
    protected var mbwManager: MbwManager? = null
    protected var whiteColor = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mbwManager = MbwManager.getInstance(requireContext())
        whiteColor = resources.getColor(R.color.white)
    }

    protected fun alignTables(view: TableLayout) {
        // find the widest column in first table
        val maxWidth1 = requireActivity().findViewById<TableLayout>(R.id.main_table).children.filter { it is TableRow }.map {
            val tv = (it as TableRow).getChildAt(0)
            tv.measure(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
            tv.measuredWidth
        }.max() ?: 0

        // find the widest column in second table
        val maxWidth2 = view.children.filter { it is TableRow }.map {
            val tv = (it as TableRow).getChildAt(0)
            tv.measure(LinearLayoutCompat.LayoutParams.WRAP_CONTENT, LinearLayoutCompat.LayoutParams.WRAP_CONTENT)
            tv.measuredWidth
        }.max() ?: 0

        // remember elements of the first columns of two tables
        val tv1 = (requireActivity().findViewById<TableLayout>(R.id.main_table).getChildAt(0) as TableRow).getChildAt(0)
        val tv2 = (view.getChildAt(0) as TableRow).getChildAt(0)

        if (maxWidth1 > maxWidth2) {
            tv2.minimumWidth = maxWidth1
        } else {
            tv1.minimumWidth = maxWidth2
        }
    }

    protected fun getValue(value: Value, tag: Any?): View? {
        return TextView(requireContext()).apply {
            layoutParams = TransactionDetailsActivity.FPWC
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            text = value.toStringWithUnit(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.basedOnCoinType))
            setTextColor(whiteColor)
            this.tag = tag
            setOnLongClickListener {
                Utils.setClipboardString(value.toString(mbwManager!!.getDenomination(mbwManager!!.selectedAccount.basedOnCoinType)), requireContext())
                Toaster(requireContext()).toast(R.string.copied_to_clipboard, true)
                true
            }
        }
    }
}