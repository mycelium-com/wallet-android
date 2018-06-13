package com.mycelium.wallet.activity.settings.helper;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import com.mycelium.wallet.Constants;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.settings.adapter.DialogListAdapter;

public class DisplayPreferenceDialogHandler implements PreferenceManager.OnDisplayPreferenceDialogListener {
    private Context context;
    private AlertDialog alertDialog;

    public DisplayPreferenceDialogHandler(Context context) {
        this.context = context;
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {
        int theme = R.style.MyceliumSettings_Dialog;
        switch (preference.getKey()) {
            case Constants.SETTING_TOR:
            case Constants.SETTING_DENOMINATION:
            case Constants.SETTING_MINER_FEE:
                theme = R.style.MyceliumSettings_Dialog_Small;
                break;
        }

        if (preference instanceof ListPreference) {
            final ListPreference listPreference = (ListPreference) preference;

            View view = LayoutInflater.from(context).inflate(R.layout.dialog_pref_list, null);
            final RecyclerView listView = view.findViewById(android.R.id.list);
            listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            final int selectedIndex = listPreference.findIndexOfValue(listPreference.getValue());
            DialogListAdapter arrayAdapter = new DialogListAdapter(listPreference.getEntries(), selectedIndex);
            listView.setAdapter(arrayAdapter);
            arrayAdapter.setClickListener(new DialogListAdapter.ClickListener() {
                @Override
                public void onClick(String val, int position) {
                    String value = listPreference.getEntryValues()[position].toString();
                    if (listPreference.callChangeListener(value)) {
                        listPreference.setValue(value);
                    }
                    alertDialog.dismiss();
                }
            });
            listView.scrollToPosition(selectedIndex);

            alertDialog = new AlertDialog.Builder(context, theme)
                    .setTitle(listPreference.getDialogTitle())
                    .setView(view)
                    .setNegativeButton(R.string.cancel, null)
                    .create();
            alertDialog.show();
        } else {
            throw new IllegalArgumentException("Tried to display dialog for unknown " +
                    "preference type. Did you forget to override onDisplayPreferenceDialog()?");
        }

    }
}
