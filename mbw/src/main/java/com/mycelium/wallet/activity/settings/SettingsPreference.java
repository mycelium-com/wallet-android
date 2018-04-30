package com.mycelium.wallet.activity.settings;

import android.content.Context;
import android.content.SharedPreferences;

public class SettingsPreference {
    private static final String MYDFS_TOKEN_ENABLE = "mydfs_token_enable";
    private static SettingsPreference instance = new SettingsPreference();

    public static SettingsPreference getInstance() {
        return instance;
    }

    private SharedPreferences sharedPreferences;

    public void init(Context context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public void setEnableMyDFS(boolean enable) {
        sharedPreferences.edit()
                .putBoolean(MYDFS_TOKEN_ENABLE, enable)
                .apply();

    }

    public boolean isMyDFSEnabled() {
        return sharedPreferences.getBoolean(MYDFS_TOKEN_ENABLE, true);
    }
}
