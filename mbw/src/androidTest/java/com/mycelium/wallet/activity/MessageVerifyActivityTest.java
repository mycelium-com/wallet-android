package com.mycelium.wallet.activity;

import android.support.test.rule.ActivityTestRule;
import android.widget.EditText;

import com.mycelium.wallet.R;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;

public class MessageVerifyActivityTest {

    @Rule
    public ActivityTestRule<MessageVerifyActivity> messageVerifyRule
            = new ActivityTestRule<>(MessageVerifyActivity.class);

    private MessageVerifyActivity SUT;
    private EditText signedMessageEditText;

    @Before
    public void setUp() throws Exception {
        SUT = messageVerifyRule.getActivity();
        signedMessageEditText = SUT.findViewById(R.id.signedMessage);
    }

    @Test
    public void testResultWithMultiline() {
        final String input_correct_multiline = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "I am the owner of Instagram account \"bitcoin\".\n" +
                "The email it was registered with is prostoosloshnom@gmail.com\n" +
                "Account was hacked and deactivated,  I have no access to it.\n" +
                "Please restore access to my account  with the private key it associated with. \n" +
                "This message is signed with this private key.\n" +
                "Thanks. \n" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 18Sn6Vs65xCJRGshBoiaYX9mmmzXxnkoFq\n" +
                "\n" +
                "IMsV/oKkUwS5XPhQIjPY7X0tzd9NL/nUt8UaE8EUZi54SDNyrAk0ogzH8YsYzSkNsuujPrjtTBhXjO66fMA5374=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input_correct_multiline);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithMultiline2() {
        final String input_correct_multiline = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "Hello.\n" + "Ok" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 1NdeBBoqx8nyhRif3jULyLhECrpVersogN\n" +
                "\n" +
                "H1KinN1aIeXJpRyqUcwNovOkE1rVPE5ZELqbVrooBQZOamT/sUmciq0wGIcSJRj4SUIz9fFAFlFWmPvO5QAnbEM=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input_correct_multiline);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithSingleLine() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "thisisatest" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh\n" +
                "\n" +
                "IB066t2s+CpjlCJx00oERatuz/jaQRDvms9zx9kH2DW2dvUVoJsSIKCrxI419mJtpvyZoqm5eLNTbtBQWmqM324=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithSpaceNewlineSpace() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                " \n " +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh\n" +
                "\n" +
                "H2Kv4LAl5NARmpQ6EWY7vnT18fI8M84QK2sD54PaqYdOBVFaF6PnxfZaLyDF2akuOsP+KpebW4OVOGL8VU0daKw=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithLoremIpsum() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut " +
                "labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris " +
                "nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit " +
                "esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in" +
                " culpa qui officia deserunt mollit anim id est laborum." +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh\n" +
                "\n" +
                "H36ukDLATPxOUNizAkABOUvRoMqvWfd8FYWnRVH46Y9lSPvvrjYqhFGx2KWshOBICsOk6Qn+CmRoE79Sfk0E9xw=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithNothing() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh\n" +
                "\n" +
                "HyVnOv6mTYO2BoCynsOzBNA1VSPStnKj9rhS2VjzO6anRn6ahLjCZwcoHxTgGJNz+KBnzgNNlbeGovWnmcWfbww=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithRegexChars() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "..?.*.+[A-Z]+123[\\^abc][a-c&&[\\^b-c]]" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: 12nAh7so7FtgQ7PhoTgsqoNNFktn2d2FXh\n" +
                "\n" +
                "IME5kO4S7rFZabmV2/7ZlJqLt/BVWmjXel2gOtHubaVWMVyuaND9xkxtlBvUN7XAjdHShavbZ2d1OtmRNmfN+ug=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }

    @Test
    public void testResultWithSingeLineTestnet() {
        final String input = "-----BEGIN BITCOIN SIGNED MESSAGE-----\n" +
                // region user message
                "Hello" +
                // endregion user message
                "-----BEGIN BITCOIN SIGNATURE-----\n" +
                "Version: Bitcoin-qt (1.0)\n" +
                "Address: mg4u71KgaMToQ1GnV7eXkRifrdv9moNqoA\n" +
                "\n" +
                "IP1nHlu95JS4VYV92diIh+SYONiNbVlpUdtcZuaNQEL6RKQ5f9DnTj63+BBH+/rIQx6H4Fcc3sG+AHdUW7ym+mE=\n" +
                "-----END BITCOIN SIGNATURE-----";

        SUT.runOnUiThread(new Runnable() {
            public void run() {
                signedMessageEditText.setText(input);
                assertEquals(true, SUT.checkResult);
            }
        });
    }
}