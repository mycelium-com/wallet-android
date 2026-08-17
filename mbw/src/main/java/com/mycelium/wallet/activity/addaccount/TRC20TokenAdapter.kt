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
import com.mycelium.wapi.wallet.Util
import com.mycelium.wapi.wallet.trc20.coins.TRC20Token
import java.io.IOException
import java.io.InputStream

class TRC20TokenWrap(val token: TRC20Token, var isChecked: Boolean)

class TRC20TokenAdapter(val alreadyAdded: List<TRC20Token>)
    : ListAdapter<TRC20TokenWrap, TRC20TokenAdapter.TokenViewHolder>(DiffCallback()) {

    var selectListener: ((TRC20Token) -> Unit)? = null
    fun getSelectedList(): List<TRC20Token> = currentList.filter { it.isChecked }.map { it.token }

    fun submit(tokens: List<TRC20Token>) = submitList(tokens.map { TRC20TokenWrap(it, false) })

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder =
        TokenViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.token_item, parent, false))

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        val token = getItem(position).token
        holder.binding.tvName.text = token.name + if (alreadyAdded.contains(token)) " " + holder.binding.tvName.resources.getString(R.string.account_added_part) else ""

        var icon: Drawable? = null
        try {
            val ims: InputStream = holder.binding.ivIcon.resources.assets.open("token-logos/" + Util.trimTestnetSymbolDecoration(token.symbol).lowercase() + "_logo.png")
            icon = Drawable.createFromStream(ims, null)
        } catch (e: IOException) {
            // no logo for this token
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

    class DiffCallback : DiffUtil.ItemCallback<TRC20TokenWrap>() {
        override fun areItemsTheSame(oldItem: TRC20TokenWrap, newItem: TRC20TokenWrap): Boolean =
            oldItem.token.contractAddress == newItem.token.contractAddress

        override fun areContentsTheSame(oldItem: TRC20TokenWrap, newItem: TRC20TokenWrap): Boolean =
            equalsValuesBy(oldItem, newItem, { it.token.name }, { it.isChecked })
    }
}
