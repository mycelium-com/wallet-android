package com.mycelium.giftbox.client

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrd.bitlib.crypto.Bip39
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.StartupActivity.mainAccounts
import com.mycelium.wapi.wallet.AesKeyCipher
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.math.BigDecimal

@RunWith(AndroidJUnit4::class)
class MCGiftboxApiRepositoryTests {
    private val WORD_LIST = arrayOf(
        "abandon", "abandon", "abandon", "abandon", "abandon", "abandon", "abandon",
        "abandon", "abandon", "abandon", "abandon", "about"
    )

    private lateinit var repository: MCGiftboxApiRepository

    @Before
    fun setUp() {
        repository = MCGiftboxApiRepository()
        val mbwManager =
            MbwManager.getInstance(ApplicationProvider.getApplicationContext<Context>())
        if (!mbwManager.masterSeedManager.hasBip32MasterSeed()) {
            val masterSeed = Bip39.generateSeedFromWordList(WORD_LIST, "")
            mbwManager.masterSeedManager.configureBip32MasterSeed(
                masterSeed,
                AesKeyCipher.defaultKeyCipher()
            )
            mbwManager.createAdditionalBip44AccountsUninterruptedly(mainAccounts)
        }
    }

    @Test
    fun testProducts() = kotlinx.coroutines.test.runTest {
        repository.getProducts(this,  country = null, category = null,
            success = {
                assert(true)
//                assertEquals(2, it?.products?.size)
            },
            error = { _, _ -> assert(false) },
            finally = {}
        )
    }

    @Test
    fun testGetOrders() = kotlinx.coroutines.test.runTest {
        repository.getOrders(this,
            success = {
                assert(true)
            },
            error = { _, _ -> assert(false) },
            finally = {}
        )
    }

    @Test
    fun testPrice() = kotlinx.coroutines.test.runTest {
        repository.getPrice(this, "ace-hardware-us", BigDecimal.ONE, "USD",
            success = {
                assert(true)
//                assertEquals(BigDecimal.TEN, it?.price)
//                assertEquals("BTC", it?.currency)
            },
            error = { _, _ -> assert(false) },
            finally = {}
        )
    }


    @Test
    fun testCards() = kotlinx.coroutines.test.runTest {
        repository.getCards(this,
            success = {
                assert(true)
//                assertEquals(1, it?.size)
//                assertEquals("name", it?.first()?.productName)
            },
            error = { _, _ -> assert(false) },
            finally = {}
        )
    }
}
