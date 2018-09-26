package com.mycelium.wallet.activity

import android.support.test.rule.ActivityTestRule
import android.widget.EditText

import com.mycelium.wallet.R

import org.junit.Before
import org.junit.Rule
import org.junit.Test

import org.junit.Assert.assertTrue

class MessageVerifyActivityTest {
    @Rule @JvmField
    val messageVerifyRule = ActivityTestRule(MessageVerifyActivity::class.java)
    private val explictSpace = " " // android studio kills trailing spaces even in multiline strings!!
    private val bitcoinMessageTestVectors = arrayOf(
            // multiline
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                I am the owner of Instagram account "bitcoin".
                The email it was registered with is prostoosloshnom@gmail.com
                Account was hacked and deactivated,  I have no access to it.
                Please restore access to my account  with the private key it associated with.$explictSpace
                This message is signed with this private key.
                Thanks.$explictSpace
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 18Sn6Vs65xCJRGshBoiaYX9mmmzXxnkoFq

                IMsV/oKkUwS5XPhQIjPY7X0tzd9NL/nUt8UaE8EUZi54SDNyrAk0ogzH8YsYzSkNsuujPrjtTBhXjO66fMA5374=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            // multiline 2
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                Hello.
                Ok
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 1NdeBBoqx8nyhRif3jULyLhECrpVersogN

                H1KinN1aIeXJpRyqUcwNovOkE1rVPE5ZELqbVrooBQZOamT/sUmciq0wGIcSJRj4SUIz9fFAFlFWmPvO5QAnbEM=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            // single line
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                thisisatest
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh

                IB066t2s+CpjlCJx00oERatuz/jaQRDvms9zx9kH2DW2dvUVoJsSIKCrxI419mJtpvyZoqm5eLNTbtBQWmqM324=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            // space newline space
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                $explictSpace
                $explictSpace
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh

                H2Kv4LAl5NARmpQ6EWY7vnT18fI8M84QK2sD54PaqYdOBVFaF6PnxfZaLyDF2akuOsP+KpebW4OVOGL8VU0daKw=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh

                H36ukDLATPxOUNizAkABOUvRoMqvWfd8FYWnRVH46Y9lSPvvrjYqhFGx2KWshOBICsOk6Qn+CmRoE79Sfk0E9xw=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            // regexp
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                ..?.*.+[A-Z]+123[\^abc][a-c&&[\^b-c]]
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh

                IME5kO4S7rFZabmV2/7ZlJqLt/BVWmjXel2gOtHubaVWMVyuaND9xkxtlBvUN7XAjdHShavbZ2d1OtmRNmfN+ug=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                Hello
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: mg4u71KgaMToQ1GnV7eXkRifrdv9moNqoA

                IP1nHlu95JS4VYV92diIh+SYONiNbVlpUdtcZuaNQEL6RKQ5f9DnTj63+BBH+/rIQx6H4Fcc3sG+AHdUW7ym+mE=
                -----END BITCOIN SIGNATURE-----""".trimIndent(),
            // empty string
            """
                -----BEGIN BITCOIN SIGNED MESSAGE-----
                -----BEGIN BITCOIN SIGNATURE-----
                Version: Bitcoin-qt (1.0)
                Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh

                HyVnOv6mTYO2BoCynsOzBNA1VSPStnKj9rhS2VjzO6anRn6ahLjCZwcoHxTgGJNz+KBnzgNNlbeGovWnmcWfbww=
                -----END BITCOIN SIGNATURE-----""".trimIndent())
    private var sut: MessageVerifyActivity? = null
    private var signedMessageEditText: EditText? = null

    @Before
    fun setUp() {
        sut = messageVerifyRule.activity
        signedMessageEditText = sut!!.findViewById(R.id.signedMessage)
    }

    @Test
    fun testVerifyMessages() {
        for (testVector in bitcoinMessageTestVectors) {
            sut!!.runOnUiThread {
                signedMessageEditText!!.setText(testVector)
                assertTrue("$testVector\nshould verify\n", sut!!.checkResult)
            }
        }
    }
}