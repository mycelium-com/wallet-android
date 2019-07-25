package com.mycelium.wallet.activity


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.scrollTo
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.R
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.support.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import android.app.Activity

import android.os.Handler
import android.os.Looper
import android.support.test.InstrumentationRegistry.getInstrumentation
import android.support.test.espresso.*
import android.support.test.espresso.action.ViewActions
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.base.MainThread
import android.support.test.espresso.core.internal.deps.guava.collect.Iterables
import android.support.test.runner.lifecycle.Stage
import com.mycelium.wallet.activity.Utils.waitForAcitvity
import com.mycelium.wallet.activity.Utils.waitForTime
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wallet.activity.send.ManualAddressEntry
import com.mycelium.wallet.activity.send.SendInitializationActivity
import com.mycelium.wallet.activity.send.SendMainActivity
import org.junit.Before
import java.util.concurrent.TimeUnit


@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(StartupActivity::class.java)

    @Before
    fun resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(5, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(5, TimeUnit.MINUTES)
    }
    @Test
    fun startupActivityTest() {


        waitForTime(2000)

        onView(withText(R.string.master_seed_restore_backup_button)).perform(click())
        onView(withText("ОК")).perform(click())

        repeat(12) {

            onView(withText("O")).perform(click())
            onView(withText("I")).perform(click())
            onView(withText("L")).perform(click())

        }

        waitForAcitvity<ModernMain>()
        onView(withId(R.id.btSend)).perform(click())
        waitForAcitvity<SendInitializationActivity>()
        waitForTime(2000)
        waitForAcitvity<SendMainActivity>()

        onView(withId(R.id.btManualEntry)).perform(click())
        waitForAcitvity<ManualAddressEntry>()
        onView(withId(R.id.etAddress)).perform(ViewActions.typeText("2NEfsR6yeuF8tusetj5jhPz7LizY7frRfqu"))
        onView(withId(R.id.btOk)).perform(click())
        onView(withId(R.id.btEnterAmount)).perform(click())
        waitForAcitvity<GetAmountActivity>()
        onView(withText("0")).perform(click())
        onView(withText(".")).perform(click())
        onView(withText("0")).perform(click())
        onView(withText("0")).perform(click())
        onView(withText("1")).perform(click())
        onView(withId(R.id.btOk)).perform(click())
        waitForAcitvity<ManualAddressEntry>()
        onView(withText(R.id.btSend)).perform(click())


    }

}
