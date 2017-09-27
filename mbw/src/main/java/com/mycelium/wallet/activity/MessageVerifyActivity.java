package com.mycelium.wallet.activity;

import android.app.Activity;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.mrd.bitlib.crypto.PublicKey;
import com.mrd.bitlib.crypto.Signature;
import com.mrd.bitlib.crypto.SignedMessage;
import com.mrd.bitlib.crypto.WrongSignatureException;
import com.mrd.bitlib.lambdaworks.crypto.Base64;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.util.BitUtils;
import com.mrd.bitlib.util.HashUtils;
import com.mrd.bitlib.util.Sha256Hash;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.SecureKeyValueStore;

import java.math.BigInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;

import static butterknife.OnTextChanged.Callback.AFTER_TEXT_CHANGED;

public class MessageVerifyActivity extends Activity {

    private Pattern messagePattern = Pattern.compile("-----BEGIN BITCOIN SIGNED MESSAGE-----\n?" +
            "(.*?)\n?" +
            "-----(BEGIN SIGNATURE|BEGIN BITCOIN SIGNATURE)-----\n?" +
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

    SecureKeyValueStore store;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_verify);
        ButterKnife.bind(this);
        signedMessageEditText.setHint(String.format(MessageSigningActivity.TEMPLATE, "Message", "Address", "Signature"));
        store = MbwManager.getInstance(MessageVerifyActivity.this).getWalletManager(false).getSecureStorage();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String clipboard = Utils.getClipboardString(MessageVerifyActivity.this);
        pasteView.setEnabled(!clipboard.isEmpty());
    }

    @OnClick(R.id.btPaste)
    void onPasteClick() {
        String clipboard = Utils.getClipboardString(MessageVerifyActivity.this);
        signedMessageEditText.setText(clipboard);
    }

    @OnTextChanged(value = R.id.signedMessage, callback = AFTER_TEXT_CHANGED)
    void textChanged(Editable editable) {
        boolean checkResult = false;
        Address address = null;
        String msgWithSign = signedMessageEditText.getText().toString();
        Matcher matcher = messagePattern.matcher(msgWithSign);
        if (matcher.find()) {
            address = Address.fromString(matcher.group(6));
            String msg = matcher.group(1);
            Sha256Hash data = HashUtils.doubleSha256(org.bitcoinj.core.Utils.formatMessageForSigning(msg));
            try {
                SignedMessage signedMessage = validate(address, msg, matcher.group(7));
                checkResult = signedMessage.getPublicKey().verifyDerEncodedSignature(data, signedMessage.getDerEncodedSignature());
            } catch (Exception e) {
                Log.e("MessageVerifyActivity", "WrongSignatureException", e);
            }
        }
        verifyResultView.setVisibility(View.VISIBLE);
        verifyResultView.setText(checkResult ? "Message verified to be from " + address.toString() : "Message failed to verify! ");
        verifyResultView.setTextColor(getResources().getColor(checkResult ? R.color.status_green : R.color.status_red));
    }

    /**
     * we need use org.bitcoinj.core.Utils.formatMessageForSigning in other case we have not valid message for not latin character
     * code below copy paste from SignedMessage(with replace formatMessageForSigning)
     */

    public static SignedMessage validate(Address address, String message, String signatureBase64)
            throws WrongSignatureException {
        final byte[] signatureEncoded = Base64.decode(signatureBase64);
        if (signatureEncoded == null) {
            // Invalid or truncated base64
            throw new WrongSignatureException(String.format("given signature is not valid base64 %s", signatureBase64));
        }
        final Signature sig = decodeSignature(signatureEncoded);
        final RecoveryInfo info = recoverFromSignature(message, signatureEncoded, sig);
        SignedMessage.validateAddressMatches(address, info.publicKey);
        return SignedMessage.from(sig, info.publicKey, info.recId);
    }

    private static Signature decodeSignature(byte[] signatureEncoded) throws WrongSignatureException {
        // Parse the signature bytes into r/s and the selector value.
        if (signatureEncoded.length < 65)
            throw new WrongSignatureException("Signature truncated, expected 65 bytes and got " + signatureEncoded.length);
        BigInteger r = new BigInteger(1, BitUtils.copyOfRange(signatureEncoded, 1, 33));
        BigInteger s = new BigInteger(1, BitUtils.copyOfRange(signatureEncoded, 33, 65));
        return new Signature(r, s);
    }

    private static RecoveryInfo recoverFromSignature(String message, byte[] signatureEncoded, Signature sig)
            throws WrongSignatureException {
        int header = signatureEncoded[0] & 0xFF;
        // The header byte: 0x1B = first key with even y, 0x1C = first key with
        // odd y,
        // 0x1D = second key with even y, 0x1E = second key with odd y
        if (header < 27 || header > 34)
            throw new WrongSignatureException("Header byte out of range: " + header);

        byte[] messageBytes = org.bitcoinj.core.Utils.formatMessageForSigning(message);
        // Note that the C++ code doesn't actually seem to specify any character
        // encoding. Presumably it's whatever
        // JSON-SPIRIT hands back. Assume UTF-8 for now.
        Sha256Hash messageHash = HashUtils.doubleSha256(messageBytes);
        boolean compressed = false;
        if (header >= 31) {
            compressed = true;
            header -= 4;
        }
        int recId = header - 27;
        PublicKey ret = SignedMessage.recoverFromSignature(recId, sig, messageHash, compressed);
        if (ret == null) {
            throw new WrongSignatureException("Could not recover public key from signature");
        }
        return new RecoveryInfo(ret, recId);
    }

    public static class RecoveryInfo {
        PublicKey publicKey;
        int recId;

        private RecoveryInfo(PublicKey publicKey, int recId) {
            this.publicKey = publicKey;
            this.recId = recId;
        }
    }
}
