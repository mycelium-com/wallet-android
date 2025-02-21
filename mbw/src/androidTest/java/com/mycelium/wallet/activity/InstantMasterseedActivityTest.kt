package com.mycelium.wallet.activity

import android.content.Intent
import android.widget.ListView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ApplicationProvider
import androidx.test.core.app.launchActivity
import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.mycelium.wallet.CommonKeys
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.HdAccountSelectorActivity.Companion.COIN_TYPE
import com.mycelium.wallet.activity.InstantMasterseedActivity.Companion.PASSWORD
import com.mycelium.wallet.activity.InstantMasterseedActivity.Companion.WORDS
import org.hamcrest.CoreMatchers.anything
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InstantMasterseedActivityTest {

    @Before
    fun setup() {
    }

    @Test
    fun testAccountSelection() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            InstantMasterseedActivity::class.java
        )
            .putExtra(WORDS, CommonKeys.WORD_LIST)
            .putExtra(PASSWORD, "")
            .putExtra(COIN_TYPE, Utils.getBtcCoinType())

        launchActivity<InstantMasterseedActivity>(intent).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
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

            scenario.close()
        }
    }
}