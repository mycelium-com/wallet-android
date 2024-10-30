package com.mycelium.wallet.activity.receive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.receive.ReceiveBchViewModel
import com.mycelium.wallet.activity.receive.ReceiveBtcViewModel
import com.mycelium.wallet.activity.receive.ReceiveERC20ViewModel
import com.mycelium.wallet.activity.receive.ReceiveEthViewModel
import com.mycelium.wallet.activity.receive.ReceiveFIOViewModel
import com.mycelium.wallet.activity.receive.ReceiveGenericCoinsViewModel
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount
import com.mycelium.wapi.wallet.fio.FioAccount

class ReceiveCoinsFactory(val account: WalletAccount<*>) : ViewModelProvider.Factory {
    val instance = WalletApplication.getInstance()
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        when (account) {
            is SingleAddressBCHAccount, is Bip44BCHAccount -> ReceiveBchViewModel(instance) as T
            is SingleAddressAccount, is HDAccount -> ReceiveBtcViewModel(instance) as T
            is BitcoinVaultHdAccount -> ReceiveBtcViewModel(instance) as T
            is EthAccount -> ReceiveEthViewModel(instance) as T
            is ERC20Account -> ReceiveERC20ViewModel(instance) as T
            is FioAccount -> ReceiveFIOViewModel(instance) as T
            else -> ReceiveGenericCoinsViewModel(instance) as T
        }
}