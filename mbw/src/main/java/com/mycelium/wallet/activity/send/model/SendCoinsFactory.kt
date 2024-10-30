package com.mycelium.wallet.activity.send.model

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount

class SendCoinsFactory(val account: WalletAccount<*>) : ViewModelProvider.Factory {
    val instance = WalletApplication.getInstance()
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when (account) {
            is ColuAccount ->
                SendColuViewModel(instance) as T

            is SingleAddressAccount, is HDAccount, is BitcoinVaultHdAccount ->
                SendBtcViewModel(instance) as T

            is EthAccount, is ERC20Account ->
                SendEthViewModel(instance) as T

            is FioAccount ->
                SendFioViewModel(instance) as T

            else -> throw NotImplementedError()
        }
}
