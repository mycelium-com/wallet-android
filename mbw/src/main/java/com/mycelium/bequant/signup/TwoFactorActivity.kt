package com.mycelium.bequant.signup

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.Constants.ACTION_BEQUANT_TOTP_CONFIRMED
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.loader
import com.mycelium.bequant.remote.repositories.Api
import com.mycelium.wallet.R
import kotlinx.android.synthetic.main.activity_two_factor.*


class TwoFactorActivity : AppCompatActivity(R.layout.activity_two_factor) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setSupportActionBar(toolbar)
        supportActionBar?.run {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(true)
            setHomeAsUpIndicator(resources.getDrawable(R.drawable.ic_back_arrow))
        }
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW
                && intent.data?.host == "reg.bequant.io"
                && intent.data?.path == "/account/totp/confirm") {
            loader(true)
            Api.signRepository.accountTotpConfirm(lifecycleScope, intent.data?.getQueryParameter("token")
                    ?: "", {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_TOTP_CONFIRMED))
            },
                    error = { _, message ->
                        ErrorHandler(this).handle(message)
                    }, finally = {
                loader(false)
            })
        }
    }
}