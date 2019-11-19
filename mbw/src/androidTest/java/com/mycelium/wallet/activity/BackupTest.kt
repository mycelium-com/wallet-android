package com.mycelium.wallet.activity

import android.app.Activity
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingPolicies
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents.*
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit

@LargeTest
@RunWith(AndroidJUnit4::class)
class BackupTest {

    lateinit var activityScenario: ActivityScenario<StartupActivity>

    @get:Rule
    var activityTestRule = ActivityTestRule(StartupActivity::class.java, false, false)

    @Before
    fun resetTimeout() {
        IdlingPolicies.setMasterPolicyTimeout(20, TimeUnit.MINUTES)
        IdlingPolicies.setIdlingResourceTimeout(20, TimeUnit.MINUTES)

        init()

        activityScenario =
                ActivityScenario.launch(StartupActivity::class.java)

    }

    @After
    fun after() {
        release()
        activityScenario.close()

    }

    @Test
    fun testBackup() {

        var currentActivity: Activity? = null
        runBlocking {
            currentActivity = waitForActivity<StartupActivity>()
            //waiting for dialog until it pops up
            delay(300)
        }


        if (currentActivity is StartupActivity) {
            onView(withText(R.string.master_seed_restore_backup_button))
                    .perform(click())

            onView(withText(R.string.ok))
                    .perform(click())

            repeat(12) {
                onView(withText("O")).perform(click())
                onView(withText("I")).perform(click())
                onView(withText("L")).perform(click())
            }
        }
        intended(hasComponent(ModernMain::class.java.name))

    }
}

