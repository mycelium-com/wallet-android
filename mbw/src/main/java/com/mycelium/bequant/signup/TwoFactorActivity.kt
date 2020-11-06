package com.mycelium.bequant.signup

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.mycelium.bequant.Constants
import com.mycelium.bequant.Constants.ACTION_BEQUANT_TOTP_CONFIRMED
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_two_factor.*
import org.json.JSONObject
import com.mycelium.wallet.Constants.BAD_REQUEST_HTTP_CODE

class TwoFactorActivity : AppCompatActivity(R.layout.activity_two_factor) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
        }
        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent:Intent) {
        Firebase.dynamicLinks
                .getDynamicLink(intent)
                .addOnSuccessListener(this) { pendingDynamicLinkData ->
                    var deepLink: Uri? = null
                    if (pendingDynamicLinkData != null) {
                        deepLink = pendingDynamicLinkData.link
                    }

                    if (deepLink != null) {
                        if (deepLink.host == "reg.bequant.io" && deepLink.path == "/account/totp/confirm") {
                            loader(true)
                            Api.signRepository.accountTotpConfirm(lifecycleScope,
                                    token = intent.data?.getQueryParameter("token") ?: "",
                                    success = {
                                        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_TOTP_CONFIRMED))
                                    },
                                    error = { _, message ->
                                        try {
                                            var obj = JSONObject(message)
                                            var code = obj.getString("code")
                                            var message = obj.getString("message")

                                            // Handles the case when TOTP email is already confirmed.
                                            // The current API returns HTTP 400 for it
                                            if (code == BAD_REQUEST_HTTP_CODE) {
                                                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_TOTP_CONFIRMED))
                                            } else {
                                                ErrorHandler(this).handle(message)
                                            }
                                        } catch (ex: Exception) {
                                            ErrorHandler(this).handle(message)
                                        }
                                    },
                                    finally = {
                                        loader(false)
                                    })
                        }
                    }
                }
    }

}