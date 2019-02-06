package com.mycelium.wallet.activity.modern.adapter;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wallet.event.AssetSelected;
import com.mycelium.wapi.wallet.GenericAddress;
import com.squareup.otto.Bus;

import java.util.List;

public class SelectAssetDialog extends DialogFragment {
    private static List<GenericAddress> addressList;
    private static SelectAssetDialog instance;
    private Bus bus;

    public static SelectAssetDialog getInstance(List<GenericAddress> genericAddresses) {
        if (instance == null) {
            instance = new SelectAssetDialog();
        }
        addressList = genericAddresses;
        return instance;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setIcon(R.drawable.ic_launcher);
        // todo fix this, title not shown fully
        builder.setTitle(String.format(getString(R.string.diff_type), Utils.getClipboardString(getActivity())));

        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        CharSequence[] items = new CharSequence[addressList.size()];
        for (int i = 0; i < addressList.size(); i++) {
            items[i] = addressList.get(i).getCoinType().getName();
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("selectassetlog", "setItems onClick: item selected: " + which);
                bus.post(new AssetSelected(addressList.get(which)));
            }
        });

        return builder.create();
    }

    @Override
    public void onAttach(Context context) {
        bus = MbwManager.getEventBus();
        super.onAttach(context);
    }

    @Override
    public void onResume() {
        super.onResume();
        bus.register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        bus.unregister(this);
    }
}
