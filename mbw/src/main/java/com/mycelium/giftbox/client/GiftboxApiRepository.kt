package com.mycelium.giftbox.client

import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.models.*
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import kotlinx.coroutines.CoroutineScope
import java.lang.IllegalArgumentException
import java.util.*

class GiftboxApiRepository {
    private lateinit var lastOrderId: String

    private val api = GiftboxApi.create()

    private val clientUserIdFromMasterSeed by lazy {
        val mbwManager = MbwManager.getInstance(WalletApplication.getInstance())
        mbwManager.masterSeedManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher())
            .getPrivateKeyForWebsite(Constants.ENDPOINT, AesKeyCipher.defaultKeyCipher())
            .publicKey.toString()
    }

    private fun updateOrderId() {
        lastOrderId = UUID.randomUUID().toString()
    }

    init {
        updateOrderId()
    }

    fun getPrice(
        scope: CoroutineScope,
        code: String,
        quantity: Int,
        amount: Int,
        currencyId: String,
        success: (PriceResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.price(
                clientUserIdFromMasterSeed,
                lastOrderId!!,
                amount,
                quantity,
                code,
                currencyId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getProduct(
        scope: CoroutineScope,
        productId: String,
        success: (ProductResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit = {}
    ) {
        doRequest(scope, {
            api.product(
                clientUserIdFromMasterSeed,
                Constants.CLIENT_ORDER_ID, //TODO check why client order id need for product
                productId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getProducts(
        scope: CoroutineScope,
        search: String? = null,
        country: List<CountryModel>? = null,
        category: String? = null,
        offset: Long = 0,
        limit: Long = 100,
        success: (ProductsResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        val countryString = country?.joinToString(",") { it.acronym }
        doRequest(scope, {
            api.products(
                clientUserIdFromMasterSeed,
                lastOrderId,
                category,
                search,
                if(countryString?.isNotEmpty() == true) countryString else null,
                offset,
                limit
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun createOrder(
        scope: CoroutineScope,
        code: String,
        quantity: Int,
        amount: Int,
        currencyId: String,
        success: (OrderResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        updateOrderId()
        doRequest(scope, {
            api.createOrder(
                CreateOrderRequest(
                    clientUserId = clientUserIdFromMasterSeed,
                    clientOrderId = lastOrderId,
                    code = code,
                    quantity = quantity.toString(),
                    amount = amount.toString(),
                    currencyId = currencyId
                )
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun checkoutProduct(
        scope: CoroutineScope,
        code: String,
        quantity: Int,
        amount: Int,
        currencyId: String,
        success: (CheckoutProductResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.checkoutProduct(
                clientUserIdFromMasterSeed,
                lastOrderId,
                code,
                quantity,
                amount,
                currencyId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getOrders(
        scope: CoroutineScope,
        offset: Long = 0,
        limit: Long = 100,
        success: (OrdersHistoryResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.orders(clientUserIdFromMasterSeed, offset, limit)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getOrder(
        scope: CoroutineScope,
        item: Order,
        success: (OrderResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.order(clientUserIdFromMasterSeed, item.clientOrderId!!)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }
}