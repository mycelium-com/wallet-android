package com.mycelium.bequant.sign

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.Constants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.Constants.ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED
import com.mycelium.bequant.Constants.ACTION_BEQUANT_TOTP_CONFIRMED
import com.mycelium.bequant.remote.SignRepository
import com.mycelium.wallet.R


class SignActivity : AppCompatActivity(R.layout.activity_bequant_sign) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                && intent.data?.path == "/account/email/confirm") {
            SignRepository.repository.accountEmailConfirm(this,
                    intent.data?.getQueryParameter("token") ?: "",
                    {
                        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
                    })
        } else if (intent.action == Intent.ACTION_VIEW
                && intent.data?.host == "reg.bequant.io"
                && intent.data?.path == "/account/totp/confirm") {
            SignRepository.repository.accountTotpConfirm(this, intent.data?.getQueryParameter("token")
                    ?: "", {
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_TOTP_CONFIRMED))
            })
        } else if (intent.action == Intent.ACTION_VIEW
                && intent.data?.host == "reg.bequant.io"
                && intent.data?.path == "/account/password/set") {
            LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_RESET_PASSWORD_CONFIRMED)
                    .putExtra("token", intent.data?.getQueryParameter("token") ?: ""))
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean =
            when (item?.itemId) {
                android.R.id.home -> {
                    onBackPressed()
                    true
                }
                else -> super.onOptionsItemSelected(item)
            }
}