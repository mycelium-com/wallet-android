package com.mycelium.wallet.activity

import android.widget.TableLayout
import android.widget.TableRow
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.core.view.children
import androidx.fragment.app.Fragment
import com.mycelium.wallet.R

open class GenericDetailsFragment : Fragment() {
    protected open fun alignTables(view: TableLayout) {
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
}