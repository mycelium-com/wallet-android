package com.mycelium.wallet.modularisation

import android.content.Context
import com.mycelium.modularizationtools.model.Module
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.WalletAccount


object GooglePlayModuleCollection {
    @JvmStatic
    fun getModules(context: Context): Map<String, Module> =
            hashMapOf("bch" to Module(WalletApplication.getSpvModuleName(WalletAccount.Type.BCHBIP44)
                    , context.getString(R.string.bitcoin_cash_module)
                    , context.getString(R.string.bch_module_description)));
}