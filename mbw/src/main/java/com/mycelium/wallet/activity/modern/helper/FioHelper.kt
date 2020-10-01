package com.mycelium.wallet.activity.modern.helper

import android.app.Activity
import android.content.Intent
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wapi.wallet.WalletManager
import java.text.SimpleDateFormat
import java.util.*


object FioHelper {
    @JvmStatic
    fun chooseAccountToMap(context: Activity, walletManager: WalletManager) {
//        val fioModule = walletManager.getModuleById(FioModule.ID) as FioModule
//        val names = fioModule.getAllFIONames()

        context.startActivity(Intent(context, AccountMappingActivity::class.java)
//                .putExtra("fioAccount", fioModule.getFioAccountByFioName(names.first()))
//                .putExtra("fioName", names.first())
        )

//        if (names.size > 1) {
//            AlertDialog.Builder(context)
//                    .setTitle("Select FIO Name to map")
//                    .setItems(names.toTypedArray()) { _, position ->
//                        context.startActivity(Intent(context, AccountMappingActivity::class.java)
//                                .putExtra("fioAccount", fioModule.getFioAccountByFioName(names[position]))
//                                .putExtra("fioName", names[position]))
//                    }
//                    .create()
//                    .show()
//        } else if (names.size == 1) {
//
//        } else {
//            Toaster(context).toast("No FIO Names currently to map with. Register FIO Name first", false)
//        }
    }

    @JvmStatic
    fun transformExpirationDate(dateStr: String): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("GMT")
        val date = sdf.parse(dateStr)

        // val requiredSdf = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.US) - old format
        // new format - September 20, 2021 at 6:23pm
        val requiredSdf = SimpleDateFormat("LLLL dd, yyyy 'at' hh:mm a", Locale.US)
        return requiredSdf.format(date)
    }
}