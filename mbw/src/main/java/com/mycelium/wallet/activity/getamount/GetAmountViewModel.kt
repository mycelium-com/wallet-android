package com.mycelium.wallet.activity.getamount

import android.app.Application
import androidx.lifecycle.*
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.WalletApplication
import com.mycelium.wallet.activity.util.get
import com.mycelium.wallet.activity.util.toStringWithUnit
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.coins.AssetInfo
import com.mycelium.wapi.wallet.coins.Value
import com.mycelium.wapi.wallet.erc20.ERC20Account
import com.mycelium.wapi.wallet.eth.EthAccount


class GetAmountViewModel(application: Application) : AndroidViewModel(application) {
    val mbwManager = MbwManager.getInstance(application)
    var account = MutableLiveData<WalletAccount<Address>?>()
    val maxSpendableAmount = MutableLiveData<Value>()
    val amount = MutableLiveData<Value>()
    val amountValidation = MutableLiveData<AmountValidation>()
    val currentCurrency = MutableLiveData<AssetInfo>()

    val maxAmount: LiveData<String> =
            Transformations.switchMap(MediatorLiveData<Pair<Value?, AssetInfo?>>().apply {
                addSource(maxSpendableAmount) {
                    value = Pair(it, currentCurrency.value)
                }
                addSource(currentCurrency) {
                    value = Pair(maxSpendableAmount.value, it)
                }
            }) {
                if (it.first != null && it.second != null) {
                    MutableLiveData(WalletApplication.getInstance().resources.getString(R.string.max_btc,
                            (convert(it.first!!, it.second!!) ?: Value.zeroValue(it.second!!))
                            .toStringWithUnit(mbwManager.getDenomination(account.value!!.coinType))))
                } else {
                    MutableLiveData("")
                }
            }

    val currencyCurrencyText: LiveData<String> =
            Transformations.switchMap(currentCurrency) {
                mbwManager.currencySwitcher.setCurrency(account.value!!.coinType, it)
                MutableLiveData(mbwManager.currencySwitcher.getCurrentCurrencyIncludingDenomination(account.value!!.coinType))
            }

    val howMaxSpendableCalculated: LiveData<Boolean> =
            Transformations.switchMap(maxAmount) {
                MutableLiveData(account.value!!.coinType == Utils.getBtcCoinType() && it.isNotEmpty())
            }

    val parentAccount: LiveData<EthAccount?> =
        Transformations.switchMap(account) {
            MutableLiveData((it as? ERC20Account)?.ethAcc)
        }

    val notEnoughBalanceToPayForFee: LiveData<Boolean> =
        Transformations.switchMap(amountValidation) {
            MutableLiveData(it == AmountValidation.NotEnoughFunds && account.value!!.accountBalance.spendable.moreThan(amount.value!!))
        }

    fun convert(value: Value, assetInfo: AssetInfo): Value? =
            mbwManager.exchangeRateManager.get(mbwManager.getWalletManager(false), value, assetInfo)

}
