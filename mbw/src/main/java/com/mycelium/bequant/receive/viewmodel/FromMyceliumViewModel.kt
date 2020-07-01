package com.mycelium.bequant.receive.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mycelium.bequant.BequantPreference
import com.mycelium.bequant.Constants
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.GenericTransaction
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.FeePerKbFee
import com.mycelium.wapi.wallet.coins.Value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class FromMyceliumViewModel : ViewModel() {
    val oneCoinFiatRate = MutableLiveData<String>()
    val castodialBalance = MutableLiveData<String>()
    val amount = MutableLiveData<String>()
    val amountFiat = MutableLiveData<String>()
    val address = MutableLiveData<String>()

    fun hasOneCoinFiatRate() = oneCoinFiatRate.value != null && oneCoinFiatRate.value?.isNotEmpty() == true

    val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
//    fun deposit(account: WalletAccount<*>, value: Value,
//                success: (GenericTransaction) -> Unit, error: (Exception) -> Unit, finally: () -> Unit) {
//
//        viewModelScope.launch(Dispatchers.Default) {
//            try {
//                val addresses = mbwManager.getWalletManager(false)
//                        .parseAddress(if (mbwManager.network.isProdnet) address.value!! else Constants.TEST_ADDRESS)
//                val address = addresses[0]
//                val fee = FeePerKbFee(Value.parse(Utils.getBtcCoinType(), "0.00000001"))
//                val tx = account.createTx(address, value, fee, null)
//                account.signTx(tx, AesKeyCipher.defaultKeyCipher())
//                success.invoke(tx)
//                BequantPreference.setMockCastodialBalance(BequantPreference.getMockCastodialBalance().plus(value))
//            } catch (ex: Exception) {
//                error(error)
//            }
//        }
//                .invokeOnCompletion {
//                    finally.invoke()
//                }
//    }

    fun getCryptocurrenciesSymbols(): List<String> {
        return mbwManager.getWalletManager(false).getCryptocurrenciesSymbols().map { it.removePrefix("t") }
    }
}