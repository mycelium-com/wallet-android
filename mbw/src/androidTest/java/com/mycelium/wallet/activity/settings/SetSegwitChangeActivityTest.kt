package com.mycelium.wallet.activity.settings

import android.app.Activity
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import android.view.View
import com.mycelium.wallet.R
import android.widget.RadioButton
import android.widget.RadioGroup
import com.mycelium.wallet.Constants
import com.mycelium.wapi.wallet.bip44.ChangeAddressMode

import org.junit.Before
import org.junit.Rule
import org.junit.Test

import org.junit.Assert.assertEquals

class SetSegwitChangeActivityTest {
    @Rule @JvmField
    val setSegwitChangeRule = ActivityTestRule(SetSegwitChangeActivity::class.java)
    private val sharedPrefs = InstrumentationRegistry.getTargetContext().getSharedPreferences(Constants.SETTINGS_NAME, Activity.MODE_PRIVATE)
    private var sut: SetSegwitChangeActivity? = null
    private var radioGroup: RadioGroup? = null

    @Before
    fun setUp() {
        sut = setSegwitChangeRule.activity
        radioGroup = sut!!.findViewById(R.id.radio_group)
    }

    @Test
    fun changeSettingsArePersistedInSharedPrefs() {
        enumValues<ChangeAddressMode>().filter { it != ChangeAddressMode.NONE }.forEach {
            sut!!.runOnUiThread {
                val changeMode = it.toString()
                (radioGroup!!.findViewWithTag<View>(changeMode) as? RadioButton)!!.performClick()
                val changeModeInPrefs =  sharedPrefs.getString(Constants.CHANGE_ADDRESS_MODE,
                        null)
                assertEquals(changeMode, changeModeInPrefs)
            }
        }
    }
}
