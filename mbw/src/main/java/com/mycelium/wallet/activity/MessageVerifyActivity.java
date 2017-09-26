package com.mycelium.wallet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.EditText;

import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.R;

import java.io.UnsupportedEncodingException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageVerifyActivity extends Activity {
    private Pattern messagePattern = Pattern.compile("-----BEGIN BITCOIN SIGNED MESSAGE-----" +
            "(.*?)" +
            "-----BEGIN BITCOIN SIGNATURE-----" +
            "Version: (.*?)" +
            "Address: (.*?)" +
            "(.*?)" +
            "-----END BITCOIN SIGNATURE-----");
    private EditText signedMessageEditText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_verify);
        signedMessageEditText = (EditText) findViewById(R.id.signedMessage);
        signedMessageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String msgWithSign = signedMessageEditText.getText().toString();
                Matcher matcher = messagePattern.matcher(msgWithSign);
                String address = matcher.group(3);
                PublicKey publicKey = new PublicKey(address.getBytes());
                String msg = matcher.group(1);
                Sha256Hash data = null;
                try {
                    data = HashUtils.doubleSha256(msg.getBytes("UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    Log.e("", "encoding problem ", e);
                }
                String signatire = matcher.group(4);
                boolean checkResult = publicKey.verifyDerEncodedSignature(data, signatire.getBytes());

            }
        });
    }
}
