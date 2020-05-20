package com.mycelium.wallet.activity.addaccount

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.CheckedTextView
import android.widget.ImageView
import com.mycelium.wallet.R
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import java.io.IOException
import java.io.InputStream


class ERC20TokenAdapter(context: Context,
                        val resource: Int,
                        val objects: List<ERC20Token>,
                        val alreadyAdded: List<ERC20Token>) : ArrayAdapter<ERC20Token>(context, resource, objects) {

    var selectListener: ((List<ERC20Token>) -> Unit)? = null
    val selectedList = mutableListOf<ERC20Token>()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
            (convertView ?: LayoutInflater.from(context).inflate(resource, parent, false))!!.apply {
                val tvName = this.findViewById<CheckedTextView>(R.id.tvName)
                val token = getItem(position)
                tvName?.text = token?.name + if(alreadyAdded.contains(token)) " " + context.getString(R.string.account_added_part) else ""

                var icon: Drawable? = null
                val symbol = token?.symbol
                try {
                    // get input stream
                    val ims: InputStream = context.resources.assets.open("token-logos/" + symbol?.toLowerCase() + "_logo.png")
                    // load image as Drawable
                    icon = Drawable.createFromStream(ims, null)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                val ivIcon = this.findViewById<ImageView>(R.id.ivIcon)
                if (icon == null) {
                    ivIcon?.visibility = View.INVISIBLE
                } else {
                    ivIcon?.setImageDrawable(icon)
                    ivIcon?.visibility = View.VISIBLE
                }
                tvName?.isChecked = selectedList.contains(token) || alreadyAdded.contains(token)
                this.isEnabled = !alreadyAdded.contains(token)
                this.setOnClickListener {
                    tvName.isChecked = !tvName.isChecked
                    if (selectedList.contains(token)) {
                        selectedList.remove(token)
                    } else {
                        selectedList.add(token)
                    }
                    selectListener?.invoke(selectedList)
                    notifyDataSetChanged()
                }
            }
}