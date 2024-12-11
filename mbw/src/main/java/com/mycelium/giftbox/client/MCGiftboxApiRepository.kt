package com.mycelium.giftbox.client

import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountryModel
import com.mycelium.bequant.remote.doRequest
import com.mycelium.generated.giftbox.database.GiftboxCard
import com.mycelium.generated.giftbox.database.GiftboxDB
import com.mycelium.giftbox.dateAdapter
import com.mycelium.giftbox.model.Card
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.AesKeyCipher
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.mrd.bitlib.model.AddressType
import com.mycelium.bequant.kyc.inputPhone.coutrySelector.CountriesSource
import com.mycelium.bequant.remote.doRequestModify
import com.mycelium.giftbox.client.model.MCCreateOrderRequest
import com.mycelium.giftbox.client.model.MCOrderResponse
import com.mycelium.giftbox.client.model.MCOrderStatusRequest
import com.mycelium.giftbox.client.model.MCOrderStatusResponse
import com.mycelium.giftbox.client.model.MCPrice
import com.mycelium.giftbox.client.model.MCProductInfo
import com.mycelium.giftbox.client.model.OrderList
import com.mycelium.giftbox.client.model.Products
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import retrofit2.Response
import java.math.BigDecimal
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.orEmpty
import kotlin.math.abs
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
        GiftboxCard.Adapter(dateAdapter)
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

    var cacheProducts: Pair<Long, Response<List<MCProductInfo>>>? = null

    fun getProducts(
        scope: CoroutineScope,
        search: String? = null,
        country: List<CountryModel>? = null,
        category: String? = null,
        offset: Long = 0,
        limit: Long = 100,
        success: (Products?) -> Unit,
        error: (Int, String) -> Unit,
        finally: (() -> Unit)? = null
    ): Job =
        doRequestModify<List<MCProductInfo>, Products>(scope, {
            if (System.currentTimeMillis() - (cacheProducts?.first
                    ?: 0) < TimeUnit.MINUTES.toMillis(5)
                && cacheProducts?.second != null
            ) {
                cacheProducts?.second
            } else {
                api.products().apply {
                    cacheProducts = System.currentTimeMillis() to this
                }
            }!!
        }, successBlock = success, errorBlock = error, finallyBlock = finally,
            responseModifier = {
                val products = it?.filter {
                    if (search?.isNotEmpty() == true)
                        it.name?.contains(search, true) == true else true
                }?.filter {
                    if (country?.isNotEmpty() == true) {
                        it.countries?.intersect(country.map { it.acronym })?.isNotEmpty() == true
                    } else true
                }?.filter {
                    if (category?.isNotEmpty() == true) it.categories?.contains(category) == true else true
                }
                val categories = it?.flatMap { it.categories.orEmpty() }
                    ?.toSet().orEmpty().filter { it.isNotEmpty() }

                val countries = it?.flatMap { it.countries.orEmpty() }?.toSet()?.mapNotNull {
                    CountriesSource.countryModels.find { model -> model.acronym.equals(it, true) }
                }
                Products(products, categories, countries)
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
                MCCreateOrderRequest(
                    userId,
                    code,
                    amount,
                    cryptoCurrency,
                    amountCurrency
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