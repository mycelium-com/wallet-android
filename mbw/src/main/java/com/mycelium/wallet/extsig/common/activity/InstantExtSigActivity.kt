package com.mycelium.wallet.extsig.common.activity

import android.view.View
import android.widget.AdapterView
import android.widget.AdapterView.OnItemClickListener
import com.mycelium.wallet.activity.HdAccountSelectorActivity.HdAccountWrapper
import com.mycelium.wallet.activity.send.SendInitializationActivity.Companion.getIntent
import com.mycelium.wallet.activity.util.AbstractAccountScanManager

abstract class InstantExtSigActivity<AccountScanManager : AbstractAccountScanManager> :
    ExtSigAccountSelectorActivity<AccountScanManager>() {
    override fun accountClickListener(): OnItemClickListener? =
        object : OnItemClickListener {
            override fun onItemClick(adapterView: AdapterView<*>, view: View?, i: Int, l: Long) {
                (adapterView.getItemAtPosition(i) as? HdAccountWrapper)?.run {
                    val intent = getIntent(this@InstantExtSigActivity, id!!, true)
                    this@InstantExtSigActivity.startActivityForResult(intent, REQUEST_SEND)
                }
            }
        }
}
