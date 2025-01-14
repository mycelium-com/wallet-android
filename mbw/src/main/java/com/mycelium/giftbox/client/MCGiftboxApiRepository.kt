package com.mycelium.giftbox.client

import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mrd.bitlib.model.AddressType
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.doRequest
import com.mycelium.bequant.remote.doRequestModify
import com.mycelium.generated.giftbox.database.GiftboxCard
import com.mycelium.generated.giftbox.database.GiftboxDB
import com.mycelium.generated.giftbox.database.GiftboxProduct
import com.mycelium.giftbox.GiftboxPreference
import com.mycelium.giftbox.categories
import com.mycelium.giftbox.client.model.MCCreateOrderRequest
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCOrderStatusRequest
import com.mycelium.giftbox.client.model.MCOrderStatusResponse
import com.mycelium.giftbox.client.model.MCPrice
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.model.MCProductResponse
import com.mycelium.giftbox.client.model.OrderList
import com.mycelium.giftbox.client.model.Products
import com.mycelium.giftbox.countries
import com.mycelium.giftbox.dateAdapter
import com.mycelium.giftbox.getProducts
import com.mycelium.giftbox.listBigDecimalAdapter
import com.mycelium.giftbox.model.Card
import com.mycelium.giftbox.save
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.genericdb.Adapters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import retrofit2.Response
import java.math.BigDecimal
import java.util.*
import kotlin.text.orEmpty

class MCGiftboxApiRepository {

    private var lastOrderId = updateOrderId()

    private val signatureProvider = object : SignatureProvider {
        override fun address(): String = clientUserIdFromMasterSeed.toAddress(
            MbwManager.getInstance(WalletApplication.getInstance()).network,
            AddressType.P2PKH
        ).toString()

        override fun signature(data: String): String =
            MbwManager.getInstance(WalletApplication.getInstance())
                .masterSeedManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher())
                .getPrivateKeyForWebsite(
                    GiftboxConstants.MC_WEBSITE,
                    AesKeyCipher.defaultKeyCipher()
                )
                .signMessage(data).base64Signature
    }
    private val api = McGiftboxApi.create(signatureProvider)

    private val giftbxDB = GiftboxDB.invoke(
        AndroidSqliteDriver(GiftboxDB.Schema, WalletApplication.getInstance(), "giftbox.db"),
        GiftboxCard.Adapter(dateAdapter),
        GiftboxProduct.Adapter(
            Adapters.listAdapter, Adapters.listAdapter,
            Adapters.bigDecimalAdapter, Adapters.bigDecimalAdapter,
            listBigDecimalAdapter
        )
    )

    private val clientUserIdFromMasterSeed by lazy {
        MbwManager.getInstance(WalletApplication.getInstance())
            .masterSeedManager.getIdentityAccountKeyManager(AesKeyCipher.defaultKeyCipher())
            .getPrivateKeyForWebsite(GiftboxConstants.MC_WEBSITE, AesKeyCipher.defaultKeyCipher())
            .publicKey
    }
    val userId get() = clientUserIdFromMasterSeed.toString()

    fun updateOrderId(): String {
        lastOrderId = UUID.randomUUID().toString()
        return lastOrderId
    }

    fun getPrice(
        scope: CoroutineScope,
        code: String,
//        quantity: Int,
        amount: BigDecimal,
        currencyId: String,
        success: (MCPrice?) -> Unit,
        error: (Int, String) -> Unit,
        finally: () -> Unit
    ) {
        doRequest(scope, {
            api.getPrice(
                MCCreateOrderRequest(
                    userId,
                    code,
                    amount,
                    "BTC",
                    currencyId
                )
            )
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

    var fetchJob: Job? = null
    fun fetchProducts(scope: CoroutineScope) {
        if (fetchJob == null) {
            fetchJob = scope.launch(Dispatchers.IO) {
                var offset = 0
                val size = 500
                val brands = mutableListOf<MCProductInfo>()
                do {
                    val response = api.products(offset, size).apply {
                        brands.addAll(body()?.items.orEmpty())
                    }
                    offset += size
                } while ((response.body()?.items?.size ?: 0) > 0)
                giftbxDB.transaction {
                    giftbxDB.giftboxProductQueries.deleteAll()
                    brands.forEach { it.save(giftbxDB) }
                }
                GiftboxPreference.productFetched()
                fetchJob = null
            }
        }
    }


    fun getProducts(
        scope: CoroutineScope,
        search: String? = null,
        country: CountryModel? = null,
        category: String? = null,
        offset: Int = 0,
        limit: Int = 100,
        success: (Products?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ): Job =
        doRequestModify<MCProductResponse, Products>(scope, {
//            giftbxDB.giftboxProductQueries.deleteAll()
//            GiftboxPreference.setLastProductFetch(0)
            if (GiftboxPreference.needFetchProducts()) {
                fetchProducts(scope)
                api.products(offset, limit).apply {
                    giftbxDB.transaction {
                        body()?.items?.forEach { it.save(giftbxDB) }
                    }
                }
            } else {
                val items = giftbxDB.getProducts(offset, limit, search, category, country)
                Response<MCProductResponse>.success(MCProductResponse(-1, items))
            }
        }, successBlock = success, errorBlock = error, finallyBlock = finally,
            responseModifier = {
                val categories = giftbxDB.categories()
                val countries = giftbxDB.countries()
                Products(it?.items.orEmpty(), categories, countries)
            })


    fun createOrder(
        scope: CoroutineScope,
        code: String,
        quantity: Int,
        amount: BigDecimal,
        amountCurrency: String,
        cryptoCurrency: String,
        success: (MCOrderResponse?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ) {
        updateOrderId()
        doRequest(scope, {
            api.createOrder(
                MCCreateOrderRequest(userId, code, amount, cryptoCurrency, amountCurrency)
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
                .apply {
                    if (this.isSuccessful) {
                        updateCards(this.body()?.list)
//                    if (offset == 0L) {
//                        fetchAllOrders(scope, limit, this.body()?.size ?: 0)
//                    }
                    }
                }
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
            api.orderStatus(
                MCOrderStatusRequest(
                    userId,
                    orderId
                )
            )
        }, successBlock = success, errorBlock = error, finallyBlock = finally)
    }

//    private fun fetchAllOrders(scope: CoroutineScope, offset: Long, count: Long) {
//        for (i in offset..count step 100) {
//            getOrders(scope, i, 100, success = {
//            })
//        }
//    }

    private fun updateCards(orders: List<MCOrderResponse>?) {
        orders?.filter {
            (it.cardUrl?.isNotEmpty() == true) or (it.cardCode?.isNotEmpty() == true)
        }?.forEach { order ->
            giftbxDB.giftboxCardQueries.updateCard(
                order.product?.id,
                order.product?.name,
                order.product?.cardImageUrl,
                order.product?.currency,
                order.faceValue.toString(),
                order.product?.expiryData,
                order.createdDate,
                order.orderId,
                order.cardCode.orEmpty(),
                order.cardUrl.orEmpty(),
                ""
            )
            if (giftbxDB.giftboxCardQueries.isCardUpdated().executeAsOne() == 0L) {
                giftbxDB.giftboxCardQueries.insertCard(
                    order.orderId,
                    order.product?.id,
                    order.product?.name,
                    order.product?.cardImageUrl,
                    order.product?.currency,
                    order.faceValue.toString(),
                    order.product?.expiryData,
                    order.cardCode.orEmpty(),
                    order.cardUrl.orEmpty(),
                    "",
                    order.createdDate
                )
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