package com.mycelium.giftbox.client

import com.mycelium.bequant.remote.doRequest
import com.mycelium.giftbox.client.models.*
import kotlinx.coroutines.CoroutineScope

class GiftboxApiRepository {
    val api = GiftboxApi.create()

    fun getPrice(
        scope: CoroutineScope,
        clientUserId: String,
        clientOrderId: String,
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
                clientUserId,
                clientOrderId,
                code,
                quantity,
                amount,
                currencyId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getProduct(
        scope: CoroutineScope,
        clientUserId: String,
        clientOrderId: String,
        productId: String,
        success: (ProductResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.product(
                clientUserId,
                clientOrderId,
                productId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getProducts(
        scope: CoroutineScope,
        search: String? = null,
        country: String? = null,
        category: String? = null,
        product_id: String? = null,
        offset: Long = 0,
        limit: Long = 100,
        clientUserId: String,
        clientOrderId: String?,
        success: (ProductsResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.products(
                search,
                country,
                category,
                offset,
                limit,
                clientUserId,
                clientOrderId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun createOrder(
        scope: CoroutineScope,
        clientUserId: String,
        clientOrderId: String,
        code: String,
        quantity: Int,
        amount: Int,
        currencyId: String,
        success: (CreateOrderResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.createOrder(
                clientUserId,
                clientOrderId,
                code,
                quantity,
                amount,
                currencyId
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun checkoutProduct(
        scope: CoroutineScope,
        clientUserId: String,
        clientOrderId: String,
        code: String,
        quantity: Int,
        amount: Int,
        success: (CheckoutProductResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.checkoutProduct(
                clientUserId,
                clientOrderId,
                code,
                quantity,
                amount
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getOrders(
        scope: CoroutineScope,
        clientUserId: String,
        offset: Long = 0,
        limit: Long = 100,
        success: (GetOrdersResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.orders(
                clientUserId,
                offset,
                limit
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }
}