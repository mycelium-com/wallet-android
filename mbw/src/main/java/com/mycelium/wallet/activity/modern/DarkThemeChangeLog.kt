package com.mycelium.wallet.activity.modern

import android.content.Context
import info.hannes.changelog.ChangeLog

class DarkThemeChangeLog(context: Context?) : ChangeLog(context!!, DARK_THEME_CSS) {
    companion object {
        internal val DARK_THEME_CSS = "body { color: #ffffff; background-color: #424242; }\n" +
                "h1 { padding-bottom: 2px; margin-top:16px; margin-bottom:0px; line-height:100%; }\n" +
                "ul{ margin-top:1px; }\n" +
                "a{ color: #5fcbf2; }\n" +
                DEFAULT_CSS
    }
}