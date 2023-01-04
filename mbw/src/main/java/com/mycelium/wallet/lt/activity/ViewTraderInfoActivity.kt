package com.mycelium.wallet.lt.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mycelium.lt.api.model.PublicTraderInfo
import com.mycelium.wallet.R
import com.mycelium.wallet.databinding.LtViewTraderInfoActivityBinding

class ViewTraderInfoActivity : AppCompatActivity() {
    private var _traderInfo: PublicTraderInfo? = null

    /** Called when the activity is first created.  */
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LtViewTraderInfoActivityBinding.inflate(layoutInflater).root)
        _traderInfo = intent.getSerializableExtra("traderInfo") as PublicTraderInfo?
    }

    override fun onResume() {
        supportFragmentManager.beginTransaction()
                .replace(R.id.flTraderInfo, TraderInfoFragment.createInstance(_traderInfo))
                .commitAllowingStateLoss()
        super.onResume()
    }

    companion object {
        @JvmStatic
        fun callMe(currentActivity: Activity, traderInfo: PublicTraderInfo?) {
            val intent = Intent(currentActivity, ViewTraderInfoActivity::class.java)
            intent.putExtra("traderInfo", traderInfo)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
            currentActivity.startActivity(intent)
        }
    }
}