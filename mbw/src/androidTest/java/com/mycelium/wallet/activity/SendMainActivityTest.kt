package com.mycelium.wallet.activity

import android.app.Activity.RESULT_OK
import android.app.Instrumentation
import org.junit.Before
import org.junit.Rule
import android.content.Intent
import android.app.Instrumentation.ActivityResult
import androidx.test.InstrumentationRegistry
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import com.mycelium.wallet.activity.StringHandlerActivity.RESULT_PRIVATE_KEY
import com.mycelium.wallet.content.ResultType
import androidx.test.rule.ActivityTestRule
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mrd.bitlib.model.NetworkParameters
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.view.SelectableRecyclerView
import org.junit.After
import org.junit.Assert.*
import org.junit.Test


class SendMainActivityTest {
    @Rule @JvmField
    // disable auto activity launch (launchActivity flag = false) to be able to launch it with intent
    val sendMainActivityRule = ActivityTestRule(SendCoinsActivity::class.java, false, false)
    private var sut: SendCoinsActivity? = null
    private var receiversAddressesList: SelectableRecyclerView? = null
    private var activityMonitor: Instrumentation.ActivityMonitor? = null
    private val SCAN_RESULT_CODE = 2
    private val wifPrivkey = "926KDciwGhDW4Qda45QwRGeEvEVBoAWkMaizdoetUf7Lb2gtcC8"
    private val wifCompressedPrivkey = "cTDZUcUF7pvk7FWZtcHnUPZBRXvp7PhjzWsUN1ehxZVV8TkeA846"
    // SendMainActivity requires existing account on create
    private val accountId = MbwManager.getInstance(InstrumentationRegistry.getTargetContext()).getWalletManager(false).getAccountIds()[0]

    @Before
    fun setUp() {
        // manually launch SendMainActivity to pass Intent with accountId
        sut = sendMainActivityRule.launchActivity(Intent().putExtra("account", accountId))
        receiversAddressesList = sut!!.findViewById<SelectableRecyclerView>(R.id.receiversAddressList)
    }

    @Test
    fun whenScanWifPrivkeyThenNoMultiAddressView() {
        val privkey = InMemoryPrivateKey.fromBase58String(wifPrivkey, NetworkParameters.testNetwork)
        // mock up an ActivityResult
        val dataIntent = Intent()
                .putExtra(StringHandlerActivity.RESULT_TYPE_KEY, ResultType.PRIVATE_KEY)
                .putExtra(RESULT_PRIVATE_KEY, privkey)
        val activityResult = ActivityResult(RESULT_OK, dataIntent)

        // add monitor that catches ScanActivity and returns mock ActivityResult
        activityMonitor = getInstrumentation().addMonitor(ScanActivity::class.java.name, activityResult, true)
        sut!!.startActivityForResult(Intent(sut, ScanActivity::class.java), SCAN_RESULT_CODE)
        sut!!.runOnUiThread {
            // multi address view wasn't set up
            assertNull(receiversAddressesList!!.adapter)
        }
    }


    @Test
    fun whenScanWifCompressedPrivkeyThenMultiAddressViewWithAllSupportedAddresses() {
        val privkey = InMemoryPrivateKey.fromBase58String(wifCompressedPrivkey, NetworkParameters.testNetwork)
        val supportedAddressesNumber = privkey?.publicKey?.getAllSupportedAddresses(NetworkParameters.testNetwork)?.size
        // mock up an ActivityResult
        val dataIntent = Intent()
                .putExtra(StringHandlerActivity.RESULT_TYPE_KEY, ResultType.PRIVATE_KEY)
                .putExtra(RESULT_PRIVATE_KEY, privkey)
        val activityResult = ActivityResult(RESULT_OK, dataIntent)

        // add monitor that catches ScanActivity and returns mock ActivityResult
        activityMonitor = getInstrumentation().addMonitor(ScanActivity::class.java.name, activityResult, true)
        sut!!.startActivityForResult(Intent(sut, ScanActivity::class.java), SCAN_RESULT_CODE)
        sut!!.runOnUiThread {
            // multi address view was set up
            assertNotNull(receiversAddressesList!!.adapter)

            // receiversAddressesList contains all supported addresses
            assertEquals(receiversAddressesList!!.adapter!!.itemCount, supportedAddressesNumber)
        }
    }

    @After
    fun tearDown() {
        getInstrumentation().removeMonitor(activityMonitor)
    }
}