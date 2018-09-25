package com.mycelium.wallet.activity;

import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import com.mycelium.wallet.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class MessageVerifyActivityTest {

    @Rule
    public ActivityTestRule<MessageVerifyActivity> messageVerifyRule
            = new ActivityTestRule<>(MessageVerifyActivity.class);

    private List<BitcoinMessage> bitcoinMessageTestVectors; // all messages to be tested
    private MessageVerifyActivity SUT;
    private EditText signedMessageEditText;

    @Before
    public void setUp() throws Exception {
        createTestVectors();
        SUT = messageVerifyRule.getActivity();
        signedMessageEditText = SUT.findViewById(R.id.signedMessage);
    }

    private void createTestVectors() {
        bitcoinMessageTestVectors = new ArrayList<>();

        BitcoinMessage bmWithMultiline = new BitcoinMessage("I am the owner of Instagram account \"bitcoin\".\n" +
                "The email it was registered with is prostoosloshnom@gmail.com\n" +
                "Account was hacked and deactivated,  I have no access to it.\n" +
                "Please restore access to my account  with the private key it associated with. \n" +
                "This message is signed with this private key.\n" +
                "Thanks. ",
                "18Sn6Vs65xCJRGshBoiaYX9mmmzXxnkoFq",
                "IMsV/oKkUwS5XPhQIjPY7X0tzd9NL/nUt8UaE8EUZi54SDNyrAk0ogzH8YsYzSkNsuujPrjtTBhXjO66fMA5374=");

        BitcoinMessage bmWithMultiline2 = new BitcoinMessage("Hello.\n" + "Ok", "1NdeBBoqx8nyhRif3jULyLhECrpVersogN",
                "H1KinN1aIeXJpRyqUcwNovOkE1rVPE5ZELqbVrooBQZOamT/sUmciq0wGIcSJRj4SUIz9fFAFlFWmPvO5QAnbEM=");

        BitcoinMessage bmWithSingleLine = new BitcoinMessage("thisisatest", "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                "IB066t2s+CpjlCJx00oERatuz/jaQRDvms9zx9kH2DW2dvUVoJsSIKCrxI419mJtpvyZoqm5eLNTbtBQWmqM324=");

        BitcoinMessage bmWithSpaceNewlineSpace = new BitcoinMessage(" \n ", "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                "H2Kv4LAl5NARmpQ6EWY7vnT18fI8M84QK2sD54PaqYdOBVFaF6PnxfZaLyDF2akuOsP+KpebW4OVOGL8VU0daKw=");

        BitcoinMessage bmWithLoremIpsum = new BitcoinMessage("Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut " +
                "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit " +
                "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in" +
                " culpa qui officia deserunt mollit anim id est laborum.",
                "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                "H36ukDLATPxOUNizAkABOUvRoMqvWfd8FYWnRVH46Y9lSPvvrjYqhFGx2KWshOBICsOk6Qn+CmRoE79Sfk0E9xw=");

        BitcoinMessage bmWithRegexChars = new BitcoinMessage("..?.*.+[A-Z]+123[\\^abc][a-c&&[\\^b-c]]", "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                "IME5kO4S7rFZabmV2/7ZlJqLt/BVWmjXel2gOtHubaVWMVyuaND9xkxtlBvUN7XAjdHShavbZ2d1OtmRNmfN+ug=");

        BitcoinMessage bmWithSingeLineTestnet = new BitcoinMessage("Hello", "mg4u71KgaMToQ1GnV7eXkRifrdv9moNqoA",
                "IP1nHlu95JS4VYV92diIh+SYONiNbVlpUdtcZuaNQEL6RKQ5f9DnTj63+BBH+/rIQx6H4Fcc3sG+AHdUW7ym+mE=");

        BitcoinMessage bmWithNothing = new BitcoinMessage("", "12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh",
                "HyVnOv6mTYO2BoCynsOzBNA1VSPStnKj9rhS2VjzO6anRn6ahLjCZwcoHxTgGJNz+KBnzgNNlbeGovWnmcWfbww=");

        bitcoinMessageTestVectors.add(bmWithMultiline);
        bitcoinMessageTestVectors.add(bmWithMultiline2);
        bitcoinMessageTestVectors.add(bmWithSingleLine);
        bitcoinMessageTestVectors.add(bmWithSpaceNewlineSpace);
        bitcoinMessageTestVectors.add(bmWithLoremIpsum);
        bitcoinMessageTestVectors.add(bmWithRegexChars);
        bitcoinMessageTestVectors.add(bmWithSingeLineTestnet);
        bitcoinMessageTestVectors.add(bmWithNothing);
    }

    @Test
    public void testVerifyMessages() {
        for (BitcoinMessage testVector : bitcoinMessageTestVectors) {
            final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                    testVector.message +
                    "-----BEGIN BITCOIN SIGNATURE-----\n" +
                    "Version: Bitcoin-qt (1.0)\n" +
                    "Address: " + testVector.address + "\n" +
                    "\n" +
                    testVector.signature + "\n" +
                    "-----END BITCOIN SIGNATURE-----";

            SUT.runOnUiThread(new Runnable() {
                public void run() {
                    signedMessageEditText.setText(input);
                    assertTrue(input + "\nshould verify\n", SUT.checkResult);
                }
            });
        }
    }

    private class BitcoinMessage {
        String message, address, signature;

        BitcoinMessage(String message, String address, String signature) {
            this.message = message;
            this.address = address;
            this.signature = signature;
        }
    }
}