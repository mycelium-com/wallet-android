package com.mycelium.wallet.activity.addaccount

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.mycelium.bequant.common.equalsValuesBy
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.TokenItemBinding
import com.mycelium.wapi.wallet.erc20.coins.ERC20Token
import java.io.IOException
import java.io.InputStream

class ERC20TokenWrap(val token: ERC20Token, var isChecked: Boolean)

class ERC20TokenAdapter(val alreadyAdded: List<ERC20Token>)
    : ListAdapter<ERC20TokenWrap, ERC20TokenAdapter.TokenViewHolder>(DiffCallback()) {

    var selectListener: ((ERC20Token) -> Unit)? = null
    fun getSelectedList(): List<ERC20Token> = currentList.filter { it.isChecked }.map { it.token }

    fun submit(tokens: List<ERC20Token>) = submitList(tokens.map { ERC20TokenWrap(it, false) })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder =
            TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_item, parent, false))

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        val token = getItem(position).token
        holder.binding.tvName.text = token.name + if (alreadyAdded.contains(token)) " " + holder.binding.tvName.resources.getString(R.string.account_added_part) else ""

        var icon: Drawable? = null
        val symbol = token.symbol
        try {
            // get input stream
            val ims: InputStream = holder.binding.ivIcon.resources.assets.open("token-logos/" + symbol?.toLowerCase() + "_logo.png")
            // load image as Drawable
            icon = Drawable.createFromStream(ims, null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        if (icon == null) {
            holder.binding.ivIcon.visibility = View.INVISIBLE
        } else {
            holder.binding.ivIcon.setImageDrawable(icon)
            holder.binding.ivIcon.visibility = View.VISIBLE
        }
        holder.binding.tvName.isChecked = getItem(position).isChecked || alreadyAdded.contains(token)
        holder.binding.root.isEnabled = !alreadyAdded.contains(token)
        holder.binding.root.setOnClickListener {
            val item = getItem(holder.absoluteAdapterPosition)
            item.isChecked = !item.isChecked
            holder.binding.tvName.isChecked = item.isChecked
            selectListener?.invoke(getItem(holder.absoluteAdapterPosition).token)
        }
    }

    class TokenViewHolder(view: View) : ViewHolder(view) {
        val binding = TokenItemBinding.bind(view)
    }

    class DiffCallback : DiffUtil.ItemCallback<ERC20TokenWrap>() {
        override fun areItemsTheSame(oldItem: ERC20TokenWrap, newItem: ERC20TokenWrap): Boolean =
                oldItem.token.contractAddress == newItem.token.contractAddress

        override fun areContentsTheSame(oldItem: ERC20TokenWrap, newItem: ERC20TokenWrap): Boolean =
                equalsValuesBy(oldItem, newItem, { it.token.name }, { it.isChecked })
    }

}