package com.mycelium.wallet.activity.modern.helper

import android.app.Activity
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wallet.activity.modern.Toaster
import com.mycelium.wapi.wallet.WalletManager
import com.mycelium.wapi.wallet.fio.FioModule


object FioHelper {
    @JvmStatic
    fun chooseAccountToMap(context: Activity, walletManager: WalletManager) {
        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
        val names = fioModule.getAllFIONames()

        if (names.size > 1) {
            AlertDialog.Builder(context)
                    .setTitle("Select FIO Name to map")
                    .setItems(names.toTypedArray()) { _, position ->
                        context.startActivity(Intent(context, AccountMappingActivity::class.java)
                                .putExtra("fioAccount", fioModule.getFioAccountByFioName(names[position]))
                                .putExtra("fioName", names[position]))
                    }
                    .create()
                    .show()
        } else if (names.size == 1) {
            context.startActivity(Intent(context, AccountMappingActivity::class.java)
                    .putExtra("fioAccount", fioModule.getFioAccountByFioName(names.first()))
                    .putExtra("fioName", names.first()))
        } else {
            Toaster(context).toast("No FIO Names currently to map with. Register FIO Name first", false)
        }
    }
}