package com.mycelium.giftbox.client

import com.mycelium.bequant.remote.doRequest
import kotlinx.coroutines.CoroutineScope

class GiftboxApiRepository {
    private val api = GiftboxApi.create()

    fun products(scope: CoroutineScope,
                 search: String,
                 country: String,
                 category: String,
                 product_id: String,
                 offset: Long,
                 limit: Long,
                 clientUserId: String,
                 client_order_id: String,
                 success: (ProductsResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {

        doRequest(
                scope, {
            api.products(search,
                    country,
                    category,
                    product_id,
                    offset,
                    limit,
                    clientUserId,
                    client_order_id)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)

    }

    fun product(scope: CoroutineScope,
                clientUserId: String,
                client_order_id: String,
                productId: String,
                success: (ProductResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {

        doRequest(
                scope, {
            api.product(
                    clientUserId,
                    client_order_id,
                    productId)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)

    }

    fun price(scope: CoroutineScope,
              clientUserId: String,
              client_order_id: String,
              code: String,
              quantity: Int,
              amount: Int,
              currencyId: String,
              success: (PriceResponse?) -> Unit, error: (Int, String) -> Unit, finally: () -> Unit) {

        doRequest(
                scope, {
            api.price(
                    clientUserId,
                    client_order_id,
                    code,
                    quantity,
                    amount,
                    currencyId)
        }, successBlock = success, errorBlock = error, finallyBlock = finally)

    }
}