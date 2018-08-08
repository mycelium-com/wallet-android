package com.mycelium.wallet.activity.settings;

import android.content.Context;

public interface ModulePreference {
    void setSummary(CharSequence summary);

    void setSyncStateText(String syncStatus);

    Context getContext();
}
