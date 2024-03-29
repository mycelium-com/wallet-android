package com.mycelium.wallet.external.changelly

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mycelium.wallet.external.changelly.ChangellyAPIService.Companion.retrofit
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class ChangellyHeaderInterceptorTest {

    @Test
    fun testChandllyDefaultMethod() = runBlocking {
        val api = retrofit.create(ChangellyAPIService::class.java)
        val response = api.getFixRate("eth", "btc")
        Assert.assertEquals(response.code(), 200)
    }

    @Test
    fun testChandllyExchangeAmountFix() = runBlocking {
        val api = retrofit.create(ChangellyAPIService::class.java)
        val response = api.exchangeAmountFix("eth", "btc", BigDecimal.ONE)
        Assert.assertEquals(response.code(), 200)
    }
}