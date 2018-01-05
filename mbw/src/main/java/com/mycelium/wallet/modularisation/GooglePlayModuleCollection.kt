package com.mycelium.wallet.modularisation

import android.content.Context
import com.mycelium.modularizationtools.model.Module
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R


object GooglePlayModuleCollection {
    fun getModules(context: Context): Map<String, Module> =
            hashMapOf("bch" to Module("com.mycelium.module.spvbch"
                    + (if (BuildConfig.FLAVOR == "btctestnet") ".testnet" else "")
                    + if (BuildConfig.DEBUG) ".debug" else ""
                    , context.getString(R.string.bitcoin_cash_module)
                    , context.getString(R.string.bch_module_description)));
}