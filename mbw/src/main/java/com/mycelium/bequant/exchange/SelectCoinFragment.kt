package com.mycelium.bequant.exchange

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.mycelium.bequant.common.model.CoinListItem
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.view.DividerItemDecoration
import com.mycelium.wapi.wallet.coins.GenericAssetInfo
import com.mycelium.wapi.wallet.fiat.coins.FiatType
import kotlinx.android.synthetic.main.fragment_bequant_exchange_select_coin.*

class SelectCoinFragment : Fragment(R.layout.fragment_bequant_exchange_select_coin) {
    val adapter = CoinAdapter()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        list.addItemDecoration(DividerItemDecoration(resources.getDrawable(R.drawable.divider_bequant), VERTICAL)
                .apply { setFromItem(1) })
        list.adapter = adapter

        // fetch currencies list here with Constants.API_CURRENCIES url
        val responseJson = "[{\"id\":\"BTC\",\"fullName\":\"Bitcoin\",\"crypto\":true,\"payinEnabled\":true,\"payinPaymentId\":false,\"payinConfirmations\":1,\"payoutEnabled\":true,\"payoutIsPaymentId\":false,\"transferEnabled\":true,\"delisted\":false,\"payoutFee\":\"0.000400000000\"}," +
                "{\"id\":\"ETH\",\"fullName\":\"Ethereum\",\"crypto\":true,\"payinEnabled\":true,\"payinPaymentId\":false,\"payinConfirmations\":20,\"payoutEnabled\":true,\"payoutIsPaymentId\":false,\"transferEnabled\":true,\"delisted\":false,\"payoutFee\":\"0.003000000000\"}," +
                "{\"id\":\"USDT20\",\"fullName\":\"Tether ERC20\",\"crypto\":true,\"payinEnabled\":true,\"payinPaymentId\":false,\"payinConfirmations\":20,\"payoutEnabled\":true,\"payoutIsPaymentId\":false,\"transferEnabled\":true,\"delisted\":false,\"payoutFee\":\"1.000000000000\"}," +
                "{\"id\":\"GBPB\",\"fullName\":\"British Pound Sterling\",\"crypto\":false,\"payinEnabled\":false,\"payinPaymentId\":false,\"payinConfirmations\":2,\"payoutEnabled\":false,\"payoutIsPaymentId\":false,\"transferEnabled\":true,\"delisted\":false,\"payoutFee\":\"7.690000000000\"}]"
        val mapper = ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        val response = mapper.readValue<List<Currency>>(responseJson)
        // and create CoinListItem with them
        val coinsList = mutableListOf(CoinListItem(CoinAdapter.TYPE_SEARCH), CoinListItem(CoinAdapter.TYPE_SPACE))
        response.forEach {
            coinsList.add(CoinListItem(CoinAdapter.TYPE_ITEM, assetInfoById(it)))
        }
        adapter.submitList(coinsList)

    }

    private fun assetInfoById(currency: Currency): GenericAssetInfo? {
        return if (currency.crypto) {
            when (currency.id) {
                "BTC" -> Utils.getBtcCoinType()
                "ETH" -> Utils.getEthCoinType()
                else -> null
            }
        } else {
            FiatType(currency.id.substring(0, 3))
        }
    }

    class Currency {
        var id: String = ""
        var fullName: String = ""
        var crypto: Boolean = true
    }
}