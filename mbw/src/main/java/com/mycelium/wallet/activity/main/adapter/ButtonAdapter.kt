package com.mycelium.wallet.activity.main.adapter

import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.main.model.ActionButton

class ButtonAdapter : ListAdapter<ActionButton, RecyclerView.ViewHolder>(ButtonDiffCallback()) {

    var clickListener: ((ActionButton) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            if (viewType == BUTTON) {
                ButtonHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_action_button, parent, false))
            } else {
                SpaceHolder(LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_space, parent, false))
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == BUTTON) {
            val actionButton = getItem(position)
            val button = (holder as ButtonHolder).button
            button.text = actionButton!!.text
            if (actionButton.icon != 0) {
                button.setCompoundDrawablesWithIntrinsicBounds(actionButton.icon, 0, 0, 0)
            } else if (actionButton.iconUrl != null) {
                val size = button.resources.getDimensionPixelSize(R.dimen.button_ads_icon_size)
                Glide.with(button.context)
                        .load(actionButton.iconUrl)
                        .into(object : SimpleTarget<Drawable?>(size, size) {
                            override fun onResourceReady(resource: Drawable, transition: Transition<in Drawable?>?) {
                                button.setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null)
                            }
                        })
            } else {
            }
            button.setTextColor(if (actionButton.textColor != 0) actionButton.textColor else button.resources.getColor(R.color.fio_white_alpha_0_8 ))
            button.setOnClickListener {
                clickListener?.invoke(actionButton)
            }
        } else {
            val paint = Paint()
            val textSize = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 17f, holder.itemView.resources.displayMetrics)
            paint.textSize = textSize
            val text = if (position == 0) {
                getItem(1)?.text
            } else {
                getItem(itemCount - 2)?.text
            }
            val width = paint.measureText(text).toInt()
            val paddings = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 58f, holder.itemView.resources.displayMetrics).toInt()
            val layoutParams = holder.itemView.layoutParams
            layoutParams.width = (holder.itemView.resources.displayMetrics.widthPixels - width - paddings) / 2
        }
    }

    override fun getItemViewType(position: Int): Int =
            if (getItem(position) == null) {
                SPACE
            } else {
                BUTTON
            }

    class ButtonHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val button: Button = itemView.findViewById(R.id.btn_action)
    }

    private inner class SpaceHolder(itemView: View?) : RecyclerView.ViewHolder(itemView!!)
    companion object {
        private const val SPACE = 10
        private const val BUTTON = 1
    }

    class ButtonDiffCallback : DiffUtil.ItemCallback<ActionButton>() {
        override fun areItemsTheSame(oldItem: ActionButton, newItem: ActionButton): Boolean =
            equalsValuesBy(oldItem, newItem, { it.id }, { it.text }, { it.iconUrl })

        override fun areContentsTheSame(oldItem: ActionButton, newItem: ActionButton): Boolean =
            equalsValuesBy(oldItem, newItem, { it.text }, { it.iconUrl }, { it.icon }, { it.textColor })
    }
}