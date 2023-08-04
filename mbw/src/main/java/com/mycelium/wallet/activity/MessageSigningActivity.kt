package com.mycelium.wallet.activity

import android.app.Activity
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.Utils
import com.mycelium.wallet.activity.modern.HDSigningActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.KeyCipher.InvalidKeyCipher
import com.mycelium.wapi.wallet.WalletAccount
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount
import com.mycelium.wapi.wallet.btc.bip44.AddressesListProvider
import com.mycelium.wapi.wallet.btcvault.AbstractBtcvAccount
import com.mycelium.wapi.wallet.colu.ColuAccount
import com.mycelium.wapi.wallet.eth.EthAccount
import kotlinx.android.synthetic.main.message_signing.*
import java.util.*
import kotlin.concurrent.thread

class MessageSigningActivity : AppCompatActivity() {
    private var signature: String? = null
    private var messageText: String? = null
    private var address: Address? = null
    private var account: WalletAccount<*>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTitle(R.string.sign_message)
        account = MbwManager.getInstance(this).getWalletManager(false)
            .getAccount(intent.getSerializableExtra(ACCOUNT) as UUID)!!
        address = intent.getSerializableExtra(ADDRESS) as? Address ?: account!!.receiveAddress

        setContentView(R.layout.message_signing)
        btCopyToClipboard.visibility = View.GONE
        btShare.visibility = View.GONE
        btSign.setOnClickListener {
            btSign.isEnabled = false
            etMessageToSign.isEnabled = false
            etMessageToSign.hint = ""
            val pd = ProgressDialog(this)
            pd.setTitle(getString(R.string.signing_inprogress))
            pd.setCancelable(false)
            pd.show()
            thread {
                messageText = etMessageToSign.text.toString()
                signature = account!!.signMessage(messageText!!, address)
                runOnUiThread {
                    pd.dismiss()
                    tvSignature.text = signature
                    btSign.visibility = View.GONE
                    btCopyToClipboard.visibility = View.VISIBLE
                    btShare.visibility = View.VISIBLE
                }
            }
        }
        btCopyToClipboard.setOnClickListener {
            Utils.setClipboardString(signature, this@MessageSigningActivity)
            Toaster(this@MessageSigningActivity).toast(R.string.sig_copied, false)
        }
        btShare.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "text/plain"
            sharingIntent.putExtra(Intent.EXTRA_TEXT, getBody())
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, getSubject())
            startActivity(Intent.createChooser(sharingIntent, getChooserTitle()))
        }
    }

    companion object {
        const val PRIVATE_KEY = "privateKey"
        const val ADDRESS = "address"
        const val ACCOUNT = "account"
        const val BTC_TEMPLATE = """-----BEGIN BITCOIN SIGNED MESSAGE-----
%s
-----BEGIN BITCOIN SIGNATURE-----
Version: Bitcoin-qt (1.0)
Address: %s

%s
-----END BITCOIN SIGNATURE-----"""

        // Template comes from MyCrypto so it could be copied and easily verified there
        const val ETH_TEMPLATE = """{
  "address": "%s",
  "msg": "%s",
  "sig": "%s",
  "version": "2"
}"""

        @JvmStatic
        fun callMe(currentActivity: Context, focusedAccount: WalletAccount<*>) {
            try {
                when (focusedAccount) {
                    is AddressesListProvider<*> -> {
                        val intent = Intent(currentActivity, HDSigningActivity::class.java)
                            .putExtra("account", focusedAccount.id)
                        currentActivity.startActivity(intent)
                    }
                    else -> {
                        callMe(currentActivity, focusedAccount.id)
                    }
                }
            } catch (invalidKeyCipher: InvalidKeyCipher) {
                invalidKeyCipher.printStackTrace()
            }
        }

        @JvmStatic
        fun callMe(currentActivity: Context, address: Address?, accountId: UUID) {
            val intent = Intent(currentActivity, MessageSigningActivity::class.java)
                .putExtra(ADDRESS, address)
                .putExtra(ACCOUNT, accountId)
            currentActivity.startActivity(intent)
        }

        @JvmStatic
        fun callMe(currentActivity: Context, accountId: UUID) {
            val intent = Intent(currentActivity, MessageSigningActivity::class.java)
                .putExtra(ACCOUNT, accountId)
            currentActivity.startActivity(intent)
        }
    }

    private fun getBody() =
        when (account) {
            is EthAccount -> String.format(
                ETH_TEMPLATE,
                address,
                messageText,
                signature
            )
            else -> String.format(
                BTC_TEMPLATE,
                messageText,
                address,
                signature
            )
        }

    private fun getChooserTitle() =
        when (account) {
            is AbstractBtcAccount, is AbstractBtcvAccount, is ColuAccount -> getString(R.string.signed_message_share_btc)
            else -> getString(R.string.signed_message_share, account!!.coinType.symbol)
        }

    private fun getSubject() =
        when (account) {
            is AbstractBtcAccount, is AbstractBtcvAccount, is ColuAccount -> getString(R.string.signed_message_subject_btc)
            else -> getString(R.string.signed_message_subject, account!!.coinType.symbol)
        }
}
