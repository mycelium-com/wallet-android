package com.mycelium.wallet.activity

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents.*
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.send.SendCoinsActivity
import com.mycelium.wallet.activity.send.SendInitializationActivity
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class SendAssetsTest {
    private lateinit var activityScenario: ActivityScenario<ModernMain>
    private val addressSendTo =  "2NEfsR6yeuF8tusetj5jhPz7LizY7frRfqu"
    @get:Rule
    var activityTestRule = ActivityTestRule(ModernMain::class.java, false, false)

    @Before
    fun resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(20, TimeUnit.MINUTES)
        init()
        activityScenario = ActivityScenario.launch(ModernMain::class.java)
    }

    @After
    fun after(){
        release()
        activityScenario.close()
    }

    @Test
    fun testSendBTCNotConfirmed() {
        sendBtc()
        onView(withId(R.id.tvSending)).check(matches((isDisplayed())))
    }

    @Test
    fun testSendBTCConfirmed() {
        sendBtc()
        IdlingRegistry.getInstance().register(ViewVisibilityIdlingResource(getCurrentActivity()!!, R.id.tvSending, View.GONE))
        onView(withId(R.id.tvSending)).check(matches(not(isDisplayed())))
    }

    private fun sendBtc() {
        onView(withId(R.id.btSend)).perform(click())
        intended(hasComponent(SendInitializationActivity::class.java.name))
        waitWhileAcitvity<SendInitializationActivity>()
        intended(hasComponent(SendCoinsActivity::class.java.name))
        onView(withId(R.id.btManualEntry)).perform(click())
        intended(hasComponent(ManualAddressEntry::class.java.name))
        onView(withId(R.id.etRecipient)).perform(ViewActions.typeText(addressSendTo))
        onView(withId(R.id.btOk)).perform(click())
        onView(withId(R.id.btEnterAmount)).perform(click())
        intended(hasComponent(GetAmountActivity::class.java.name))
        customKeyboardType("0.001")
        onView(withId(R.id.btOk)).perform(click())
        intended(hasComponent(ManualAddressEntry::class.java.name))
        onView(withId(R.id.btSend)).perform(click())
    }
}

