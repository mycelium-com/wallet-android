package com.mycelium.wallet.activity.modern.helper

import android.content.Context
import android.content.Intent
import com.mycelium.wallet.activity.fio.mapaccount.AccountMappingActivity
import com.mycelium.wapi.wallet.Address
import com.mycelium.wapi.wallet.WalletAccount


object FioHelper {
    @JvmStatic
    fun chooseAccountToMap(context: Context, account: WalletAccount<Address>) {
        context.startActivity(Intent(context, AccountMappingActivity::class.java)
                .putExtra("accountId", account.id)
//                .putExtra("fioName", names.first())
        )
    }
}