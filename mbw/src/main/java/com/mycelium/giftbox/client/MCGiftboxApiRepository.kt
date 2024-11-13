package com.mycelium.giftbox.client

import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.doRequest
import com.mycelium.generated.giftbox.database.GiftboxCard
import com.mycelium.generated.giftbox.database.GiftboxDB
import com.mycelium.giftbox.client.models.*
import com.mycelium.giftbox.dateAdapter
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mycelium.giftbox.client.model.MCCreateOrderRequest
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCOrderStatusRequest
import com.mycelium.giftbox.client.model.MCOrderStatusResponse
import com.mycelium.giftbox.client.model.MCPrice
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.model.OrderList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import retrofit2.Response
import java.util.*

class MCGiftboxApiRepository {

    val walletAddress = "111113333"
    val walletSignature = "222222222234"

    private var lastOrderId = updateOrderId()

    private val api = McGiftboxApi.create()
    private val giftbxDB = GiftboxDB.invoke(
        AndroidSqliteDriver(GiftboxDB.Schema, WalletApplication.getInstance(), "giftbox.db"),
        GiftboxCard.Adapter(dateAdapter)
    )

    private val clientUserIdFromMasterSeed by lazy {
        MbwManager.getInstance(WalletApplication.getInstance())
            .masterSeedManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher())
            .getPrivateKeyForWebsite(GiftboxConstants.MC_WEBSITE, AesKeyCipher.defaultKeyCipher())
            .publicKey
    }
    val userId get() = "12354812386"

    private fun updateOrderId(): String {
        lastOrderId = UUID.randomUUID().toString()
        return lastOrderId
    }

    fun getPrice(
        scope: CoroutineScope,
        code: String,
//        quantity: Int,
        amount: Int,
        currencyId: String,
        success: (MCPrice?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.getPrice(MCCreateOrderRequest(
                userId,
                code,
                amount.toString(),
                "BTC",
                currencyId,
                walletAddress,
                walletSignature
                ))
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

//    fun getProduct(
//        scope: CoroutineScope,
//        productId: String,
//        success: (ProductResponse?) -> Unit,
//        error: (Int, String) -> Unit,
//        finally: () -> Unit = {}
//    ): Job {
//        return doRequest(scope, {
//            api.product(
//                clientUserIdFromMasterSeed,
//                lastOrderId,
//                productId
//            )
//        }, successBlock = success, errorBlock = error, finallyBlock = finally)
//    }

    fun getProducts(
        scope: CoroutineScope,
        search: String? = null,
        country: List<CountryModel>? = null,
        category: String? = null,
        offset: Long = 0,
        limit: Long = 100,
        success: (List<MCProductInfo>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ): Job =
        doRequest(scope, {
            api.products()
        }, successBlock = success, errorBlock = error, finallyBlock = finally)


    fun createOrder(
        scope: CoroutineScope,
        code: String,
        quantity: Int,
        amount: Int,
        amountCurrency: String,
        cryptoCurrency: String,
        success: (MCOrderResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        updateOrderId()
        doRequest(scope, {
            api.createOrder(
                MCCreateOrderRequest(
                    userId,
                    code,
                    amount.toString(),
                    cryptoCurrency,
                    amountCurrency,
                    walletAddress,
                    walletSignature
                )
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }


    fun getOrders(
        scope: CoroutineScope,
        offset: Long = 0,
        limit: Long = 100,
        success: (OrderList?) -> Unit,
        error: ((Int, String) -> Unit)? = null,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            api.getOrders(userId)
//                .apply {
//                if (this.isSuccessful) {
//                    updateCards(this.body()?.items)
//                    if (offset == 0L) {
//                        fetchAllOrders(scope, limit, this.body()?.size ?: 0)
//                    }
//                }
//            }
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun getOrder(
        scope: CoroutineScope,
        orderId: String,
        success: (MCOrderStatusResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            api.orderStatus(MCOrderStatusRequest(
                userId,
                orderId,
                walletAddress,
                walletSignature
            ))
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

//    private fun fetchAllOrders(scope: CoroutineScope, offset: Long, count: Long) {
//        for (i in offset..count step 100) {
//            getOrders(scope, i, 100, success = {
//            })
//        }
//    }

    private fun updateCards(orders: List<Order>?) {
        orders?.forEach { order ->
            order.items?.forEach {
                giftbxDB.giftboxCardQueries.updateCard(
                    order.productCode, order.productName, order.productImg,
                    order.currencyCode, it.amount, it.expiryDate, order.timestamp,
                    order.clientOrderId ?: "", it.code ?: "",
                    it.deliveryUrl ?: "", it.pin ?: ""
                )
                if (giftbxDB.giftboxCardQueries.isCardUpdated().executeAsOne() == 0L) {
                    giftbxDB.giftboxCardQueries.insertCard(
                        order.clientOrderId ?: "",
                        order.productCode, order.productName, order.productImg, order.currencyCode,
                        it.amount, it.expiryDate, it.code ?: "", it.deliveryUrl ?: "",
                        it.pin ?: "", order.timestamp
                    )
                }
            }
        }
    }

    fun getCards(
        scope: CoroutineScope,
        success: (List<Card>?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        doRequest(scope, {
            Response.success(giftbxDB.giftboxCardQueries.selectCards(mapper = { clientOrderId: String,
                                                                                productCode: String?,
                                                                                productName: String?,
                                                                                productImg: String?,
                                                                                currencyCode: String?,
                                                                                amount: String?,
                                                                                expiryDate: String?,
                                                                                code: String,
                                                                                deliveryUrl: String,
                                                                                pin: String,
                                                                                timestamp: Date?,
                                                                                redeemed: Boolean ->
                Card(
                    clientOrderId,
                    productCode,
                    productName,
                    productImg,
                    currencyCode,
                    amount,
                    expiryDate,
                    code,
                    deliveryUrl,
                    pin,
                    timestamp,
                    redeemed
                )
            }).executeAsList())
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

    fun redeem(
        card: Card, scope: CoroutineScope,
        success: (Boolean?) -> Unit
    ) {
        doRequest(scope, {
            giftbxDB.giftboxCardQueries.redeemCard(
                card.clientOrderId,
                card.code,
                card.deliveryUrl,
                card.pin
            )
            Response.success(true)
        }, successBlock = success)
    }

    fun unredeem(
        card: Card, scope: CoroutineScope,
        success: (Boolean?) -> Unit
    ) {
        doRequest(scope, {
            giftbxDB.giftboxCardQueries.unredeemCard(
                card.clientOrderId,
                card.code,
                card.deliveryUrl,
                card.pin
            )
            Response.success(true)
        }, successBlock = success)
    }

    fun remove(
        card: Card, scope: CoroutineScope,
        success: (Boolean?) -> Unit
    ) {
        doRequest(scope, {
            giftbxDB.giftboxCardQueries.deleteCard(
                card.clientOrderId,
                card.code,
                card.deliveryUrl,
                card.pin
            )
            Response.success(true)
        }, successBlock = success)
    }
}