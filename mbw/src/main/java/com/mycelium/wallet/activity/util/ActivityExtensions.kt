package com.mycelium.wallet.activity.util

import android.app.Activity
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException

fun Activity.fileProviderAuthority(): String {
    try {
        val packageManager = this.application.packageManager
        val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PROVIDERS)
        packageInfo.providers?.forEach { info ->
            if (info.name.equals("androidx.core.content.FileProvider")) {
                return info.authority;
            }
        }
    } catch (e: NameNotFoundException) {
        throw RuntimeException(e);
    }
    throw RuntimeException("No file provider authority specified in manifest");
}