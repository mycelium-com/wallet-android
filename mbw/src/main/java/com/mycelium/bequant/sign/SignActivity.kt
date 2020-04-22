package com.mycelium.bequant.sign

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.mycelium.bequant.Constants.ACTION_BEQUANT_EMAIL_CONFIRMED
import com.mycelium.bequant.common.ErrorHandler
import com.mycelium.bequant.common.LoaderFragment
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
            val loader = LoaderFragment()
            loader.show(supportFragmentManager, "loader")
            SignRepository.repository.confirmEmail(intent.data?.getQueryParameter("token") ?: "", {
                loader.dismissAllowingStateLoss()
                LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(ACTION_BEQUANT_EMAIL_CONFIRMED))
            }, {
                loader.dismissAllowingStateLoss()
                ErrorHandler(this).handle(it)
            })
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