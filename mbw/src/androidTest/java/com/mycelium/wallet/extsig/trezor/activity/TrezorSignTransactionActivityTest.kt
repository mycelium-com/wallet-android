package com.mycelium.wallet.extsig.trezor.activity

import android.content.Context
import android.content.Intent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivityForResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.mrd.bitlib.crypto.Bip39
import com.mrd.bitlib.crypto.BipDerivationType
import com.mrd.bitlib.crypto.HdKeyNode
import com.mrd.bitlib.model.NetworkParameters
import com.mrd.bitlib.model.hdpath.HdKeyPath
import com.mycelium.wallet.CommonKeys
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SignTransactionActivity
import com.mycelium.wallet.extsig.common.ExternalSignatureDeviceManager
import com.mycelium.wallet.extsig.common.activity.ExtSigSignTransactionActivity
import com.mycelium.wapi.wallet.btc.BtcAddress
import com.mycelium.wapi.wallet.btc.BtcTransaction
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext
import com.satoshilabs.trezor.lib.ExternalSignatureDevice
import com.satoshilabs.trezor.lib.Trezor
import com.squareup.otto.Bus
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class TrezorSignTransactionActivityTest {
    @Mock
    lateinit var trezor: Trezor
    lateinit var hdRootNodes: Map<BipDerivationType, HdKeyNode>

    lateinit var trezorManager: FakeTrezorManager
    lateinit var mbwManager: MbwManager

    @Before
    fun setup() {
        val masterSeed = Bip39.generateSeedFromWordList(CommonKeys.WORD_LIST, "")
        hdRootNodes = BipDerivationType.entries.associateWith {
            HdKeyNode.fromSeed(masterSeed.bip32Seed, it)
        }
        val appContext = getInstrumentation().targetContext.applicationContext
        mbwManager = MbwManager.getInstance(appContext)
        trezorManager = FakeTrezorManager(appContext, mbwManager.network, MbwManager.getEventBus())
        MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testSign() {
        val hdPath = HdKeyPath.BIP44.getCoinTypeBitcoin(true).getAccount(0).getChain(true)
        val hdNodes = listOf(trezorManager.getAccountPubKeyNode(hdPath, BipDerivationType.BIP44))
            .filterNotNull()
        val uuid =
            trezorManager.createOnTheFlyAccount(hdNodes, mbwManager.getWalletManager(true), 0)
        val account = mbwManager.getWalletManager(true).getAccount(uuid)!!
        val address = account.receiveAddress as BtcAddress
        val transaction = BtcTransaction(
            account.coinType, listOf(address to account.coinType.value(0)),
            account.coinType.value(0)
        )
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            TrezorSignTransactionActivity::class.java
        )
            .putExtra(SendCoinsActivity.ACCOUNT, uuid)
            .putExtra(SendCoinsActivity.IS_COLD_STORAGE, true)
            .putExtra(SignTransactionActivity.TRANSACTION, transaction)

        launchActivityForResult<TrezorSignTransactionActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            scenario.onActivity { activity ->
                // Replace the dependency with a fake or mock
                initScanManager(activity)
            }
        }
    }


    fun initScanManager(activity: TrezorSignTransactionActivity) {
        ExtSigSignTransactionActivity::class.java.getDeclaredField("extSigManager").apply {
            isAccessible = true
            set(activity, trezorManager)
        }
    }

    inner class FakeTrezorManager(
        context: Context,
        network: NetworkParameters,
        eventBus: Bus
    ) : ExternalSignatureDeviceManager(context, network, eventBus) {
        override fun createDevice(): ExternalSignatureDevice =
            trezor

        override fun onBeforeScan(): Boolean = true

        override fun getBIP44AccountType(): Int =
            HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_TREZOR

        override fun getAccountPubKeyNode(
            keyPath: HdKeyPath,
            derivationType: BipDerivationType
        ): HdKeyNode? =
            hdRootNodes[derivationType]?.createChildNode(keyPath)
    }
}