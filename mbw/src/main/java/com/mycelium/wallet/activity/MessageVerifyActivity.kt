package com.mycelium.wallet.activity

import android.os.Bundle
import android.text.Editable
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
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
import com.mycelium.wallet.databinding.ActivityMessageVerifyBinding
import java.lang.Exception
import java.util.regex.Pattern

class MessageVerifyActivity : AppCompatActivity() {
    private val messagePattern = Pattern.compile(
        """
    -----BEGIN BITCOIN SIGNED MESSAGE-----(?s)
    ?(.*?)
    ?-----(BEGIN SIGNATURE|BEGIN BITCOIN SIGNATURE)-----(?-s)
    ?(Version: (.*?))?
    ?(Address: )?(.*?)
    ?
    ?(.*?)
    ?-----(END BITCOIN SIGNATURE|END BITCOIN SIGNED MESSAGE)-----
    """.trimIndent()
    )

    var pasteView: Button? = null
    var checkResult = false

    private lateinit var binding: ActivityMessageVerifyBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ActivityMessageVerifyBinding.inflate(layoutInflater).apply {
            binding = this
        }.root)
        binding.signedMessage.hint =
            String.format(MessageSigningActivity.BTC_TEMPLATE, "Message", "Address", "Signature")
        binding.btPaste.setOnClickListener {
            onPasteClick()
        }
        binding.signedMessage.doAfterTextChanged {
            textChanged(it)
        }
    }

    override fun onResume() {
        super.onResume()
        binding.btPaste.post { pasteView!!.isEnabled = !Utils.getClipboardString(this).isEmpty() }
    }

    fun onPasteClick() {
        val clipboard = Utils.getClipboardString(this)
        binding.signedMessage.setText(clipboard)
    }

    fun textChanged(editable: Editable?) {
        checkResult = false
        var address: BitcoinAddress? = null
        val msgWithSign = binding.signedMessage.text.toString()
        val matcher = messagePattern.matcher(msgWithSign)
        if (matcher.find()) {
            address = BitcoinAddress.fromString(matcher.group(6))
            val msg = matcher.group(1)
            val data = HashUtils.doubleSha256(X509Utils.formatMessageForSigning(msg))
            try {
                val sig = matcher.group(7)
                val signedMessage = SignedMessage.validate(address, msg, sig)
                checkResult = signedMessage.publicKey.verifyDerEncodedSignature(
                    data,
                    signedMessage.derEncodedSignature
                )
            } catch (e: Exception) {
                Log.e("MessageVerifyActivity", "WrongSignatureException", e)
            }
        }
        binding.verifyResult.visibility = View.VISIBLE
        binding.verifyResult.text =
            if (checkResult) getString(R.string.message_verified_to_be_from, address)
            else getString(R.string.message_failed_to_verify)
        binding.verifyResult.setTextColor(resources.getColor(if (checkResult) R.color.status_green else R.color.status_red))
    }
}