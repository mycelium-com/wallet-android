package com.mycelium.bequant.sign

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.firebase.dynamiclinks.ktx.dynamicLinks
import com.google.firebase.ktx.Firebase
import com.mycelium.bequant.Constants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.Constants.ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_bequant_sign.*
import org.json.JSONObject


class SignActivity : AppCompatActivity(R.layout.activity_bequant_sign) {
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
                        if (deepLink.host == "reg.bequant.io" && deepLink.path == "/account/email/confirm") {
                            loader(true)
                            Api.signRepository.accountEmailConfirm(lifecycleScope, intent.data?.getQueryParameter("token")
                                    ?: "",
                                    success = {
                                        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
                                    },
                                    error = { _, message ->
                                        var obj = JSONObject(message)
                                        var code = obj.getString("code")
                                        var message = obj.getString("message")

                                        if (code == "400" && message == "user already confirmed") {
                                            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
                                        } else {
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