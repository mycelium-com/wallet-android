package com.mycelium.wallet.external

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.fio.FioKeyManager


object Ads {
    fun doAction(key: String, context: Context) {
        when (key) {
            "open-fio" -> openFio(context)
        }
    }

    @JvmStatic
    fun openFio(context: Context) {
        val mbwManager = MbwManager.getInstance(context)
        val account = mbwManager.selectedAccount
        if (account is HDAccount && account.isDerivedFromInternalMasterseed()) {
            AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.confirm_fio_link, (account.accountIndex + 1).toString()))
                    .setPositiveButton(R.string.yes) { _, _ ->
                        val fioKeyManager = mbwManager.fioKeyManager
                        val fpk = fioKeyManager.getFioPublicKey(account.accountIndex)
                        val fpkString = fioKeyManager.formatPubKey(fpk)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://addresses.fio.foundation/fiorequest/mycelium/$fpkString")))
                    }
                    .setNegativeButton(R.string.no, null)
                    .create()
                    .show()
        } else {
            Toaster(context).toast(R.string.fio_requires_hd_account, false)
        }
    }
}
