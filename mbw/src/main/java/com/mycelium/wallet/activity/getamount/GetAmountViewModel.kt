package com.mycelium.wallet.activity.getamount

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Transformations
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.util.get
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value


class GetAmountViewModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    var account: WalletAccount<Address>? = null

    val maxSpendableAmount = MutableLiveData<Value>()
    val amount = MutableLiveData<Value>()

    val maxAmount: LiveData<String> =
            Transformations.switchMap(maxSpendableAmount) {
                val maxSpendable = convert(it, mbwManager.currencySwitcher.getCurrentCurrency(account!!.coinType))
                MutableLiveData<String>(getApplication<Application>().resources.getString(R.string.max_btc, maxSpendable
                        ?: Value.zeroValue(mbwManager.currencySwitcher.getCurrentCurrency(account!!.coinType)!!).toStringWithUnit(mbwManager.getDenomination(account!!.coinType))))
            }

    val howMaxSpendableCalculated: LiveData<Boolean> =
            Transformations.switchMap(maxAmount) {
                MutableLiveData<Boolean>(account?.coinType == Utils.getBtcCoinType() && it.isNotEmpty())
            }

    fun convert(value: Value?, assetInfo: AssetInfo?): Value? =
            mbwManager.exchangeRateManager.get(mbwManager.getWalletManager(false), value!!, assetInfo!!)

}