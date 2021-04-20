package com.mycelium.wallet.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import com.mrd.bitlib.crypto.InMemoryPrivateKey
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.HDSigningActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.AesKeyCipher
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.btcvault.AbstractBtcvAccount
import kotlin.concurrent.thread

class MessageSigningActivity : Activity() {
    private var base64Signature: String? = null
    private var messageText: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.sign_message)
        val encoded = intent.getStringExtra(PRIVATE_KEY)
        val address = intent.getSerializableExtra(ADDRESS) as Address
        val network = MbwManager.getInstance(this).network
        val privateKey = InMemoryPrivateKey(encoded, network)
        setContentView(R.layout.message_signing)
        val signButton = findViewById<View>(R.id.btSign)
        val copyButton = findViewById<View>(R.id.btCopyToClipboard)
        val shareButton = findViewById<View>(R.id.btShare)
        val signature = findViewById<TextView>(R.id.signature)
        val messageToSign = findViewById<EditText>(R.id.etMessageToSign)
        copyButton.visibility = View.GONE
        shareButton.visibility = View.GONE
        signButton.setOnClickListener {
            signButton.isEnabled = false
            messageToSign.isEnabled = false
            messageToSign.hint = ""
            val pd = ProgressDialog(this)
            pd.setTitle(getString(R.string.signing_inprogress))
            pd.setCancelable(false)
            pd.show()
            thread {
                messageText = messageToSign.text.toString()
                val signedMessage = privateKey.signMessage(messageText!!)
                base64Signature = signedMessage.base64Signature
                runOnUiThread {
                    pd.dismiss()
                    signature.text = base64Signature
                    signButton.visibility = View.GONE
                    copyButton.visibility = View.VISIBLE
                    shareButton.visibility = View.VISIBLE
                }
            }
        }
        copyButton.setOnClickListener {
            Utils.setClipboardString(base64Signature, this@MessageSigningActivity)
            Toaster(this@MessageSigningActivity).toast(R.string.sig_copied, false)
        }
        shareButton.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            val body = String.format(TEMPLATE, messageText, address, base64Signature)
            sharingIntent.putExtra(Intent.EXTRA_TEXT, body)
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.signed_message_subject))
            startActivity(Intent.createChooser(sharingIntent, getString(R.string.signed_message_share)))
        }
    }

    companion object {
        const val PRIVATE_KEY = "privateKey"
        const val ADDRESS = "address"
        const val TEMPLATE = """-----BEGIN BITCOIN SIGNED MESSAGE-----
%s
-----BEGIN BITCOIN SIGNATURE-----
Version: Bitcoin-qt (1.0)
Address: %s

%s
-----END BITCOIN SIGNATURE-----"""

        @JvmStatic
        fun callMe(currentActivity: Context, focusedAccount: WalletAccount<*>) {
            try {
                if (focusedAccount is HDAccount || focusedAccount is AbstractBtcvAccount) {
                    val intent = Intent(currentActivity, HDSigningActivity::class.java)
                            .putExtra("account", focusedAccount.id)
                    currentActivity.startActivity(intent)
                } else {
                    val key = focusedAccount.getPrivateKey(AesKeyCipher.defaultKeyCipher())
                    callMe(currentActivity, key, focusedAccount.receiveAddress)
                }
            } catch (invalidKeyCipher: InvalidKeyCipher) {
                invalidKeyCipher.printStackTrace()
            }
        }

        @JvmStatic
        fun callMe(currentActivity: Context, privateKey: InMemoryPrivateKey, address: Address?) {
            val privKey = privateKey.getBase58EncodedPrivateKey(MbwManager.getInstance(currentActivity).network)
            val intent = Intent(currentActivity, MessageSigningActivity::class.java)
                    .putExtra(PRIVATE_KEY, privKey)
                    .putExtra(ADDRESS, address)
            currentActivity.startActivity(intent)
        }
    }
}