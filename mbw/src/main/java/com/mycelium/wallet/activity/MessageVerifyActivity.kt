package com.mycelium.wallet.activity

import android.app.Activity
import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import butterknife.OnTextChanged
import com.mrd.bitlib.model.BitcoinAddress
import com.mrd.bitlib.crypto.SignedMessage
import com.mrd.bitlib.util.HashUtils
import com.mrd.bitlib.util.X509Utils
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import java.lang.Exception
import java.util.regex.Pattern

class MessageVerifyActivity : Activity() {
    private val messagePattern = Pattern.compile("""
    -----BEGIN BITCOIN SIGNED MESSAGE-----(?s)
    ?(.*?)
    ?-----(BEGIN SIGNATURE|BEGIN BITCOIN SIGNATURE)-----(?-s)
    ?(Version: (.*?))?
    ?(Address: )?(.*?)
    ?
    ?(.*?)
    ?-----(END BITCOIN SIGNATURE|END BITCOIN SIGNED MESSAGE)-----
    """.trimIndent())

    @JvmField
    @BindView(R.id.signedMessage)
    var signedMessageEditText: EditText? = null

    @JvmField
    @BindView(R.id.verifyResult)
    var verifyResultView: TextView? = null

    @JvmField
    @BindView(R.id.btPaste)
    var pasteView: Button? = null
    var checkResult = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_message_verify)
        ButterKnife.bind(this)
        signedMessageEditText!!.hint = String.format(MessageSigningActivity.BTC_TEMPLATE, "Message", "Address", "Signature")
    }

    override fun onResume() {
        super.onResume()
        pasteView!!.post { pasteView!!.isEnabled = !Utils.getClipboardString(this).isEmpty() }
    }

    @OnClick(R.id.btPaste)
    fun onPasteClick() {
        val clipboard = Utils.getClipboardString(this)
        signedMessageEditText!!.setText(clipboard)
    }

    @OnTextChanged(value = [R.id.signedMessage], callback = OnTextChanged.Callback.AFTER_TEXT_CHANGED)
    fun textChanged(editable: Editable?) {
        checkResult = false
        var address: BitcoinAddress? = null
        val msgWithSign = signedMessageEditText!!.text.toString()
        val matcher = messagePattern.matcher(msgWithSign)
        if (matcher.find()) {
            address = BitcoinAddress.fromString(matcher.group(6))
            val msg = matcher.group(1)
            val data = HashUtils.doubleSha256(X509Utils.formatMessageForSigning(msg))
            try {
                val sig = matcher.group(7)
                val signedMessage = SignedMessage.validate(address, msg, sig)
                checkResult = signedMessage.publicKey.verifyDerEncodedSignature(data, signedMessage.derEncodedSignature)
            } catch (e: Exception) {
                Log.e("MessageVerifyActivity", "WrongSignatureException", e)
            }
        }
        verifyResultView!!.visibility = View.VISIBLE
        verifyResultView!!.text = if (checkResult) "Message verified to be from " + address.toString() else "Message failed to verify! "
        verifyResultView!!.setTextColor(resources.getColor(if (checkResult) R.color.status_green else R.color.status_red))
    }
}