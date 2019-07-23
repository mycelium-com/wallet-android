package com.mycelium.wallet.activity


import android.support.test.espresso.Espresso
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.IdlingPolicies
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.action.ViewActions.scrollTo
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.hamcrest.core.IsInstanceOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class StartupActivityTestCreateAccount {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(StartupActivity::class.java)


    @Before
    fun resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(60, TimeUnit.SECONDS)
        IdlingPolicies.setIdlingResourceTimeout(26, TimeUnit.SECONDS)

        //clean account
//        val instance = MbwManager.getInstance(WalletApplication.getInstance())
//        instance.getWalletManager(false).deleteAccount(instance.selectedAccount.id)
    }

    @Test
    fun startupActivityTest2() {

        //simple wait
        try {
            Thread.sleep((10000).toLong())
        } catch (e: InterruptedException) {

        }

        val button = onView(
                allOf(withId(android.R.id.button1), withText(R.string.master_seed_create_new_button),
                        childAtPosition(
                                childAtPosition(
                                        withClassName(`is`("android.widget.ScrollView")),
                                        0),
                                0)))
        button.perform(scrollTo(), click())

        val textView = onView(
                allOf(withText("SegWit compatible (P2SH)"),
                        childAtPosition(
                                allOf(withId(R.id.llAddress),
                                        childAtPosition(
                                                IsInstanceOf.instanceOf(android.widget.FrameLayout::class.java),
                                                0)),
                                2),
                        isDisplayed()))
        textView.check(matches(withText("SegWit compatible (P2SH)")))

        val textView2 = onView(
                allOf(withId(R.id.tvBalance), withText("0 tBTC"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.llBalance),
                                        0),
                                0),
                        isDisplayed()))
        textView2.check(matches(withText("0 tBTC")))

        val button2 = onView(
                allOf(withId(R.id.btnFirst),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.backup_missing_layout),
                                        1),
                                0),
                        isDisplayed()))
        button2.check(matches(isDisplayed()))

        val button3 = onView(
                allOf(withId(R.id.btnSecond),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.backup_missing_layout),
                                        1),
                                1),
                        isDisplayed()))
        button3.check(matches(isDisplayed()))
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
