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
import android.widget.TextView;
import android.widget.Toast;

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
            case Constants.SETTING_DENOMINATION:
            case Constants.SETTING_MINER_FEE:
                theme = R.style.MyceliumSettings_Dialog_Small;
                break;
        }

        if (preference instanceof ListPreference) {
            final ListPreference listPreference = (ListPreference) preference;
            final int origSize = listPreference.getEntryValues().length;

            View view = LayoutInflater.from(context).inflate(R.layout.dialog_pref_list, null);
            TextView title = view.findViewById(R.id.title);
            title.setText(listPreference.getDialogTitle());
            final RecyclerView listView = view.findViewById(android.R.id.list);
            listView.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false));

            final int selectedIndex = listPreference.findIndexOfValue(listPreference.getValue());
            final DialogListAdapter arrayAdapter = new DialogListAdapter(listPreference.getEntries(), selectedIndex);
            listView.setAdapter(arrayAdapter);
            listView.scrollToPosition(selectedIndex);
            view.findViewById(R.id.buttonCancel).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    alertDialog.dismiss();
                }
            });

            view.findViewById(R.id.buttonok).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // it seems that the size of a list (such as exchanges) might change before clicking OK
                    // so we compare it to new value just before proceeding
                    final int newSize = listPreference.getEntryValues().length;
                    if (newSize == origSize) {
                        String value = listPreference.getEntryValues()[arrayAdapter.getSelected()].toString();
                        if (listPreference.callChangeListener(value)) {
                            listPreference.setValue(value);
                        }
                    }
                    else {
                        Toast.makeText(context, context.getString(R.string.try_again), Toast.LENGTH_SHORT).show();
                    }
                    alertDialog.dismiss();
                }
            });
            alertDialog = new AlertDialog.Builder(context, theme)
                    .setView(view)
                    .create();
            alertDialog.show();
        } else {
            throw new IllegalArgumentException("Tried to display dialog for unknown " +
                    "preference type. Did you forget to override onDisplayPreferenceDialog()?");
        }
    }
}
