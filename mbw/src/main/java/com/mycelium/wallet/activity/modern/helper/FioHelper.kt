package com.mycelium.wallet.activity.modern.helper

import android.app.Activity
import android.content.Intent
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount
import java.text.SimpleDateFormat
import java.util.*


object FioHelper {
    @JvmStatic
    fun chooseAccountToMap(context: Activity, account: WalletAccount<Address>) {
        context.startActivity(Intent(context, AccountMappingActivity::class.java)
                .putExtra("accountId", account.id)
//                .putExtra("fioName", names.first())
        )
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