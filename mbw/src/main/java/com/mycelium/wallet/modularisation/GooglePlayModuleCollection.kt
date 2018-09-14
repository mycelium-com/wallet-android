package com.mycelium.wallet.modularisation

import android.content.Context
import android.graphics.drawable.Drawable
import com.mycelium.modularizationtools.model.Module
import com.mycelium.wallet.BuildConfig
import com.mycelium.wallet.R
import com.mycelium.wallet.WalletApplication
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount


object GooglePlayModuleCollection {
    @JvmStatic
    fun getModules(context: Context): Map<String, Module> =
            hashMapOf("bch" to Module(WalletApplication.getSpvModuleName(Bip44BCHAccount::class.java)
                    , context.getString(R.string.bitcoin_cash_module)
                    , context.getString(R.string.bitcoin_cash_module_short)
                    , context.getString(R.string.bch_module_description)),
                    "meb" to Module(BuildConfig.appIdMeb,
                            context.getString(R.string.make_extra_btc_module),
                            context.getString(R.string.make_extra_btc_module_short),
                            context.getString(R.string.make_extra_btc_module_desc)))

    @JvmStatic
    fun getModuleByPackage(context: Context, packageName: String) =
            getModules(context).values.first { it.modulePackage == packageName }

    @JvmStatic
    fun getBigLogos(context: Context): Map<String, Drawable> =
            hashMapOf(WalletApplication.getSpvModuleName(Bip44BCHAccount::class.java) to context.resources.getDrawable(R.drawable.image_bch_module))

    @JvmStatic
    fun getBigLogo(context: Context, packageName: String) =
            getBigLogos(context).get(packageName)
}