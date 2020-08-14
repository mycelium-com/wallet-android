package com.mycelium.bequant.market

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.mycelium.bequant.getInvestmentAccounts
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.ModernMain
import com.mycelium.wapi.wallet.SyncMode
import kotlinx.android.synthetic.main.activity_bequant_market.*


class BequantMarketActivity : AppCompatActivity(R.layout.activity_bequant_market) {

    companion object {

        val IS_DEMO_KEY = "is_demo_key"

        @JvmStatic
        fun start(context: Context, isDemo: Boolean = false) {
            val starter = Intent(context, BequantMarketActivity::class.java)
                    .putExtra(IS_DEMO_KEY, isDemo)
            context.startActivity(starter)
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val inflater = (nav_host_fragment as NavHostFragment).navController.navInflater
        val graph = inflater.inflate(R.navigation.bequant_main)
        nav_host_fragment.findNavController().setGraph(graph, intent.extras)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        val logoMenuClick = { _: View ->
            val isOpened = logoMenu.visibility == VISIBLE
            logoMenu.visibility = if (isOpened) GONE else VISIBLE
            logoArrow.setImageDrawable(logoArrow.resources.getDrawable(
                    if (isOpened) R.drawable.ic_arrow_drop_down else R.drawable.ic_arrow_drop_down_active))
        }
        logoButton.setOnClickListener(logoMenuClick)
        logoMenu.setOnClickListener(logoMenuClick)
        myceliumWallet.setOnClickListener {
            finish()
            startActivity(Intent(this, ModernMain::class.java))
        }
        startSyncInvestmentAccounts()
    }

    private fun startSyncInvestmentAccounts() {
        MbwManager.getInstance(applicationContext).getWalletManager(false).let {
            it.startSynchronization(SyncMode.NORMAL_FORCED, it.getInvestmentAccounts())
        }
    }
}