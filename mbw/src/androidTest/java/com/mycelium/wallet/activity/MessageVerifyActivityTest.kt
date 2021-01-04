package com.mycelium.wallet.activity

import androidx.test.rule.ActivityTestRule
import android.widget.EditText
import com.mycelium.testhelper.SignatureTestVectors

import com.mycelium.wallet.R

import org.junit.Before
import org.junit.Rule
import org.junit.Test

import org.junit.Assert.assertTrue

class MessageVerifyActivityTest {
    @Rule @JvmField
    val messageVerifyRule = ActivityTestRule(MessageVerifyActivity::class.java)
    private var sut: MessageVerifyActivity? = null
    private var signedMessageEditText: EditText? = null

    @Before
    fun setUp() {
        sut = messageVerifyRule.activity
        signedMessageEditText = sut!!.findViewById(R.id.signedMessage)
    }

    @Test
    fun testVerifyMessages() {
        for (tv in SignatureTestVectors.bitcoinMessageTestVectors) {
            sut!!.runOnUiThread {
                signedMessageEditText!!.setText("""
-----BEGIN BITCOIN SIGNED MESSAGE-----
${tv.message}
-----BEGIN SIGNATURE-----
${tv.address}
${tv.signature}
-----END BITCOIN SIGNED MESSAGE-----
                """)
                assertTrue("Test Vector ${tv.name}:\n${tv.message}\nshould verify\n", sut!!.checkResult)
            }
        }
    }
}