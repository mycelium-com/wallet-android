package com.mycelium.wallet.activity.settings;

import android.content.Context;
import android.content.SharedPreferences;


public class SettingsPreference {
    private static SettingsPreference instance = new SettingsPreference();

    public static SettingsPreference getInstance() {
        return instance;
    }

    private SharedPreferences sharedPreferences;

    public void init(Context context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
    }
}
