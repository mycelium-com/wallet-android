package com.mycelium.wallet.activity.send.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.trc20.TRC20Account
import com.mycelium.wapi.wallet.tron.TronAccount

class SendCoinsFactory(val account: WalletAccount<*>) : ViewModelProvider.Factory {
    val instance = WalletApplication.getInstance()
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when (account) {
            is ColuAccount ->
                SendColuViewModel(instance) as T

            is SingleAddressAccount, is HDAccount ->
                SendBtcViewModel(instance) as T

            is EthAccount, is ERC20Account ->
                SendEthViewModel(instance) as T


            is TronAccount, is TRC20Account ->
                SendTronViewModel(instance) as T

            else -> throw NotImplementedError()
        }
}
