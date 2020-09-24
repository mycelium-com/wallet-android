package com.mycelium.wallet.activity.fio.mapaccount

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.wallet.activity.fio.mapaccount.adapter.AccountMappingAdapter.Companion.TYPE_ACCOUNT
import kotlin.math.min


class AccountListDivider(private val divider: Drawable) : RecyclerView.ItemDecoration() {
    override fun onDraw(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDraw(canvas, parent, state)

        val childCount = min(state.itemCount, parent.childCount)
        for (i in 1 until childCount - 1) {
            val childPrevHolder = parent.getChildViewHolder(parent.getChildAt(i - 1))
            val child = parent.getChildAt(i)
            val childHolder = parent.getChildViewHolder(child)
            if ((childPrevHolder.itemViewType == TYPE_ACCOUNT && childHolder.itemViewType != TYPE_ACCOUNT) ||
                    (childHolder.itemViewType == TYPE_ACCOUNT && childPrevHolder.itemViewType != TYPE_ACCOUNT)) {
                divider.setBounds(0, child.top, parent.width, child.top + divider.intrinsicHeight)
                divider.draw(canvas)
            }
        }
    }

}