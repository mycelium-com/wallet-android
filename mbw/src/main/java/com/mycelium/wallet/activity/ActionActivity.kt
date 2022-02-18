package com.mycelium.wallet.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.external.partner.startContentLink


class ActionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        if (MbwManager.getInstance(this).activityCount > 0) {
            startContentLink(intent.getStringExtra("action"))
        } else {
            startActivity(Intent(this, StartupActivity::class.java).putExtras(intent))
        }
        super.onCreate(savedInstanceState)
        finish()
    }
}