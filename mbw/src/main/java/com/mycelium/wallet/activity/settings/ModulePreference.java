package com.mycelium.wallet.activity.settings;

public interface ModulePreference {
    void setSummary(CharSequence summary);

    void setSyncStateText(String syncStatus);

    void setUnderIconText(String underIconText);
}
