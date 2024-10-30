package com.mycelium.wallet

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TokensTest {

    @Test
    fun testTokenList() {
//        Log.e("!!!", WalletConfiguration.TOKENS.joinToString { it.symbol + " " + it.name })

        Log.e("!!!", "size = " + WalletConfiguration.TOKENS.size)
        Log.e("!!!", WalletConfiguration.TOKENS.joinToString { it.symbol + " = " + it.prodAddress })
    }
}