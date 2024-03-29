package com.mycelium.wallet

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mycelium.wallet.external.changelly.ChangellyAPIService.Companion.retrofit
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class TokensTest {

    @Test
    fun testTokenList() {
//        Log.e("!!!", WalletConfiguration.TOKENS.joinToString { it.symbol + " " + it.name })

        Log.e("!!!", "size = " + WalletConfiguration.TOKENS.size)
        Log.e("!!!", WalletConfiguration.TOKENS.joinToString { it.symbol + " = " + it.prodAddress })
    }
}