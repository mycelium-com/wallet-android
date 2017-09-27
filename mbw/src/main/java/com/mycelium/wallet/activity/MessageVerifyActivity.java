package com.mycelium.wallet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.crypto.WrongSignatureException;
import com.mrd.bitlib.lambdaworks.crypto.Base64;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.ByteWriter;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.SecureKeyValueStore;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageVerifyActivity extends Activity {
    private static final byte[] HEADER;
    private static final byte[] SIGNING_HEADER;

    static {
        try {
            HEADER = "Bitcoin Signed Message:\n".getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        SIGNING_HEADER = standardSigningHeader();
    }

    private Pattern messagePattern = Pattern.compile("-----BEGIN BITCOIN SIGNED MESSAGE-----\n?" +
            "(.*?)\n?" +
            "-----BEGIN BITCOIN SIGNATURE-----\n?" +
            "(Version: )?(.*?)\n?" +
            "(Address: )?(.*?)\n?\n?" +
            "(.*?)\n?" +
            "-----END BITCOIN SIGNATURE-----");
    private EditText signedMessageEditText;
    private TextView verifyResultView;

    SecureKeyValueStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_verify);
        store = MbwManager.getInstance(MessageVerifyActivity.this).getWalletManager(false).getSecureStorage();
        signedMessageEditText = (EditText) findViewById(R.id.signedMessage);
        verifyResultView = (TextView) findViewById(R.id.verifyResult);
        findViewById(R.id.btPaste).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String clipboard = Utils.getClipboardString(MessageVerifyActivity.this);
                signedMessageEditText.setText(clipboard);
            }
        });
        signedMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                boolean checkResult = false;
                String msgWithSign = signedMessageEditText.getText().toString();
                Matcher matcher = messagePattern.matcher(msgWithSign);
                if (matcher.find()) {
                    Address address = Address.fromString(matcher.group(4));
                    String msg = matcher.group(1);
                    Sha256Hash data = HashUtils.doubleSha256(formatMessageForSigning(msg));
                    try {
                        SignedMessage signedMessage = SignedMessage.validate(address, msg, matcher.group(6));
                        checkResult = signedMessage.getPublicKey().verifyDerEncodedSignature(data, signedMessage.getDerEncodedSignature());
                    } catch (WrongSignatureException e) {
                        Log.e("MessageVerifyActivity", "WrongSignatureException", e);
                    }

                }
                verifyResultView.setText(checkResult ? "verified" : "failed");
            }
        });
    }

    static byte[] formatMessageForSigning(String message) {
        byte[] messageBytes;
        try {
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        ByteWriter writer = new ByteWriter(messageBytes.length + SIGNING_HEADER.length + 1);
        writer.putBytes(SIGNING_HEADER);
        writer.putCompactInt(message.length());
        writer.putBytes(messageBytes);
        return writer.toBytes();
    }

    private static byte[] standardSigningHeader() {
        ByteArrayOutputStream bos1 = new ByteArrayOutputStream();
        bos1.write(HEADER.length);
        try {
            bos1.write(HEADER);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return bos1.toByteArray();
    }

}
