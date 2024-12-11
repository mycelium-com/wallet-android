package com.mycelium.bequant.sign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.mycelium.bequant.BequantConstants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.BequantConstants.ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import org.json.JSONObject
import com.mycelium.wallet.Constants.BAD_REQUEST_HTTP_CODE
import com.mycelium.wallet.databinding.ActivityBequantSignBinding

class SignActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityBequantSignBinding.inflate(layoutInflater)
        setSupportActionBar(binding.toolbar)
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
                        if (deepLink.host == "reg.bequant.io" && deepLink.path == "/account/email/confirm") {
                            loader(true)
                            Api.signRepository.accountEmailConfirm(lifecycleScope, intent.data?.getQueryParameter("token")
                                    ?: "",
                                    success = {
                                        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
                                    },
                                    error = { _, message ->
                                        try {
                                            var obj = JSONObject(message)
                                            var code = obj.getString("code")
                                            var message = obj.getString("message")

                                            // Handles the case when the user's email is already confirmed.
                                            // The current API returns HTTP 400 with the message 'user already confirmed' for it
                                            if (code == BAD_REQUEST_HTTP_CODE && message == "user already confirmed") {
                                                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
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

                        if (deepLink.host == "reg.bequant.io" && deepLink.path == "/account/password/set") {
                            LocalBroadcastManager
                                    .getInstance(this)
                                    .sendBroadcast(Intent(ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED)
                                            .putExtra("token", deepLink.getQueryParameter("token") ?: ""))
                        }
                    }
                }
    }
}