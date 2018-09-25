package com.mycelium.wallet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;
import static org.bitcoinj.core.Utils.formatMessageForSigning;

public class MessageVerifyActivity extends Activity {

    private Pattern messagePattern = Pattern.compile("-----BEGIN BITCOIN SIGNED MESSAGE-----(?s)\n?" +
            "(.*?)\n?" +
            "-----(BEGIN SIGNATURE|BEGIN BITCOIN SIGNATURE)-----(?-s)\n?" +
            "(Version: (.*?))?\n?" +
            "(Address: )?(.*?)\n?\n?" +
            "(.*?)\n?" +
            "-----(END BITCOIN SIGNATURE|END BITCOIN SIGNED MESSAGE)-----");

    @BindView(R.id.signedMessage)
    protected EditText signedMessageEditText;

    @BindView(R.id.verifyResult)
    protected TextView verifyResultView;

    @BindView(R.id.btPaste)
    protected Button pasteView;

    protected boolean checkResult = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_verify);
        ButterKnife.bind(this);
        signedMessageEditText.setHint(String.format(MessageSigningActivity.TEMPLATE, "Message", "Address", "Signature"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        String clipboard = Utils.getClipboardString(this);
        pasteView.setEnabled(!clipboard.isEmpty());
    }

    @OnClick(R.id.btPaste)
    void onPasteClick() {
        String clipboard = Utils.getClipboardString(this);
        signedMessageEditText.setText(clipboard);
    }

    @OnTextChanged(value = R.id.signedMessage, callback = AFTER_TEXT_CHANGED)
    void textChanged(Editable editable) {
        checkResult = false;
        Address address = null;
        String msgWithSign = signedMessageEditText.getText().toString();
        Matcher matcher = messagePattern.matcher(msgWithSign);
        if (matcher.find()) {
            address = Address.fromString(matcher.group(6));
            String msg = matcher.group(1);
            Sha256Hash data = HashUtils.doubleSha256(formatMessageForSigning(msg));
            try {
                SignedMessage signedMessage = SignedMessage.validate(address, msg, matcher.group(7));
                checkResult = signedMessage.getPublicKey().verifyDerEncodedSignature(data, signedMessage.getDerEncodedSignature());
            } catch (Exception e) {
                Log.e("MessageVerifyActivity", "WrongSignatureException", e);
            }
        }
        verifyResultView.setVisibility(View.VISIBLE);
        verifyResultView.setText(checkResult ? "Message verified to be from " + address.toString() : "Message failed to verify! ");
        verifyResultView.setTextColor(getResources().getColor(checkResult ? R.color.status_green : R.color.status_red));
    }

}
