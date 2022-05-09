package com.mycelium.wallet.external.changelly2.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mycelium.giftbox.purchase.adapter.AccountAdapter
import com.mycelium.wallet.activity.modern.model.accounts.AccountListItem
import com.mycelium.wallet.databinding.ItemGiftboxSelectAccountBinding
import com.mycelium.wallet.databinding.ItemGiftboxSelectAccountGroupBinding
import com.mycelium.wapi.wallet.coins.CryptoCurrency

class AddAccountModel(val coinType: CryptoCurrency) : AccountListItem {
    override fun getType(): AccountListItem.Type =
            AccountListItem.Type.ADD_ACCOUNT_TYPE
}

class GroupModel(val title: String) : AccountListItem {
    var isCollapsed = false
    override fun getType(): AccountListItem.Type =
            AccountListItem.Type.GROUP_TYPE
}

class SelectAccountAdapter : AccountAdapter() {

    var addAccountListener: ((CryptoCurrency) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder =
            when (viewType) {
                AccountListItem.Type.ADD_ACCOUNT_TYPE.typeId ->
                    AddAccountHolder(ItemGiftboxSelectAccountBinding.inflate(LayoutInflater.from(parent.context)))
                AccountListItem.Type.GROUP_TYPE.typeId ->
                    GroupHolder(ItemGiftboxSelectAccountGroupBinding.inflate(LayoutInflater.from(parent.context)))
                else -> super.onCreateViewHolder(parent, viewType)
            }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (getItemViewType(position)) {
            AccountListItem.Type.ADD_ACCOUNT_TYPE.typeId -> {
                (holder as AddAccountHolder).let {
                    val addAccount = item as AddAccountModel
                    it.binding.label.text = addAccount.coinType.name
                    it.binding.coinType.text = addAccount.coinType.symbol
                }
            }
            AccountListItem.Type.GROUP_TYPE.typeId -> {
                (holder as GroupHolder).let {
                    it.binding.label.text = (item as GroupModel).title
                }
            }
            else -> {
                super.onBindViewHolder(holder, position)
            }
        }
    }

    class AddAccountHolder(val binding: ItemGiftboxSelectAccountBinding) : RecyclerView.ViewHolder(binding.root)

    class GroupHolder(val binding: ItemGiftboxSelectAccountGroupBinding) : RecyclerView.ViewHolder(binding.root)
}