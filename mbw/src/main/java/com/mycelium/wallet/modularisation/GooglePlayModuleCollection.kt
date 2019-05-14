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
            hashMapOf()

    @JvmStatic
    fun getModuleByPackage(context: Context, packageName: String) =
            getModules(context).values.first { it.modulePackage == packageName }

    @JvmStatic
    fun getBigLogos(context: Context): Map<String, Drawable> =
            hashMapOf()

    @JvmStatic
    fun getBigLogo(context: Context, packageName: String) =
            getBigLogos(context).get(packageName)
}