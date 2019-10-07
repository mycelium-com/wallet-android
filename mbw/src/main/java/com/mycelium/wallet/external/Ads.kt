package com.mycelium.wallet.external

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.mycelium.wallet.MbwManager
import com.mycelium.wallet.R
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.wallet.btc.bip44.HDAccount
import com.mycelium.wapi.wallet.fio.FioKeyManager


object Ads {
    fun openFio(context: Context) {
        val mbwManager = MbwManager.getInstance(context)
        val account = mbwManager.selectedAccount
        if (account is HDAccount && account.isDerivedFromInternalMasterseed) {
            AlertDialog.Builder(context)
                    .setMessage(context.getString(R.string.confirm_fio_link, (account.accountIndex + 1).toString()))
                    .setPositiveButton(R.string.button_ok) { _, _ ->
                        val fioKeyManager = FioKeyManager(mbwManager.masterSeedManager)
                        val fpk = fioKeyManager.getFioPublicKey(account.accountIndex)
                        val fpkString = fioKeyManager.formatPubKey(fpk)
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://addresses.fio.foundation/fiorequest/mycelium/$fpkString")))
                    }
                    .create()
                    .show()
        } else {
            Toast.makeText(context, R.string.fio_requires_hd_account, Toast.LENGTH_LONG).show()
        }
    }
}
