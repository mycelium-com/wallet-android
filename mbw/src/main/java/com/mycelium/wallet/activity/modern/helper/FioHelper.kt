package com.mycelium.wallet.activity.modern.helper

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.fio.getActiveFioAccounts


object FioHelper {
    @JvmStatic
    fun chooseAccountToMap(context: Context, walletManager: WalletManager) {
        val accounts = walletManager.getActiveFioAccounts()
        if (accounts.size > 1) {
            AlertDialog.Builder(context)
                    .setTitle("Select FIO Account to map")
                    .setItems(accounts.map { it.label }.toTypedArray()) { _, position ->
                        context.startActivity(Intent(context, AccountMappingActivity::class.java)
                                .putExtra("fioAccount", accounts[position].id))
                    }
                    .create()
                    .show()
        } else {
            context.startActivity(Intent(context, AccountMappingActivity::class.java)
                    .putExtra("fioAccount", accounts.first().id))
        }
    }
}