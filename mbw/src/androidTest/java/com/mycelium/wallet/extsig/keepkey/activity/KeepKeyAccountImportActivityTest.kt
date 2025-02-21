package com.mycelium.wallet.extsig.keepkey.activity

import android.app.Activity.RESULT_OK
import android.content.Context
import android.widget.ListView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.launchActivityForResult
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.CommonKeys
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.HdAccountSelectorActivity
import com.mycelium.wallet.activity.awaitCondition
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.satoshilabs.trezor.lib.ExternalSignatureDevice
import com.satoshilabs.trezor.lib.KeepKey
import com.squareup.otto.Bus
import org.hamcrest.CoreMatchers.anything
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class KeepKeyAccountImportActivityTest {

    @Mock
    lateinit var keepKey: KeepKey

    lateinit var hdRootNodes: Map<BipDerivationType, HdKeyNode>

    @Before
    fun setup() {
        val masterSeed = Bip39.generateSeedFromWordList(CommonKeys.WORD_LIST, "")
        hdRootNodes = BipDerivationType.entries.associateWith {
            HdKeyNode.fromSeed(masterSeed.bip32Seed, it)
        }
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testAccountSelection() {
        launchActivityForResult<KeepKeyAccountImportActivity>().use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                // Replace the dependency with a fake or mock
                initScanManager(activity)
            }
            // click ok on dialog, btc selected
            onView(withId(android.R.id.button1)).perform(click())

            // click first account from loaded list
            awaitCondition(10000) {
                var count = 0
                onView(withId(R.id.lvAccounts)).check { view, _ ->
                    val listView = view as ListView
                    count = listView.adapter.count
                }
                count > 0
            }
            onData(anything())
                .atPosition(0)
                .perform(click())


            val resultCode = scenario.result.resultCode
            val resultData = scenario.result.resultData

            assert(resultCode == RESULT_OK)
            assert(resultData.hasExtra("account"))
            scenario.close()
        }
    }

    fun initScanManager(activity: HdAccountSelectorActivity<*>) {
        class FakeKeepKeyManager(
            context: Context,
            network: NetworkParameters,
            eventBus: Bus
        ) : ExternalSignatureDeviceManager(context, network, eventBus) {
            override fun createDevice(): ExternalSignatureDevice =
                keepKey

            override fun onBeforeScan(): Boolean = true

            override fun getBIP44AccountType(): Int =
                HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY

            override fun getAccountPubKeyNode(
                keyPath: HdKeyPath,
                derivationType: BipDerivationType
            ): HdKeyNode? =
                hdRootNodes[derivationType]?.createChildNode(keyPath)
        }

        val mbwManager = MbwManager.getInstance(activity)
        HdAccountSelectorActivity::class.java.getDeclaredField("masterseedScanManager").apply {
            isAccessible = true
            set(
                activity,
                FakeKeepKeyManager(activity, mbwManager.network, MbwManager.getEventBus())
            )
        }
    }
}