package com.mycelium.wallet.activity.modern.adapter;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.util.Log;

import com.mycelium.wallet.R;
import com.mycelium.wallet.Utils;
import com.mycelium.wapi.wallet.GenericAddress;

import java.util.ArrayList;
import java.util.List;


public class SelectAssetDialog extends DialogFragment {

    private static List<GenericAddress> addressList;
    private List<GenericAddress> result = new ArrayList<>();

    public static SelectAssetDialog newInstance(List<GenericAddress> genericAddresses) {
        SelectAssetDialog frag = new SelectAssetDialog();
        addressList = genericAddresses;
        return frag;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        builder.setIcon(R.drawable.ic_launcher);
        // todo fix this, title not shown fully
        builder.setTitle(String.format("The address %s may belong to different crypto currency types." +
                "\n\nPlease choose which one it belongs to:", Utils.getClipboardString(getActivity())));


//        final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(getActivity(),
//                android.R.layout.select_dialog_singlechoice);
//        for(GenericAddress addr : addressList){
//            arrayAdapter.add(addr.getCoinType().getName());
//        }

        builder.setNegativeButton("cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

//        builder.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                String name = arrayAdapter.getItem(which);
//                for(GenericAddress addr : addressList){
//                    if(addr.getCoinType().getName().equals(name)){
//                        result.add(addr);
//                    }
//                }
//                //dialog.dismiss();
//            }
//        });

        CharSequence[] items = new CharSequence[addressList.size()];
        for (int i = 0; i < addressList.size(); i++){
            items[i] = addressList.get(i).getCoinType().getName();
        }

        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.d("selectassetlog", "setItems onClick: item selected: " + which);
                // todo based on 'which' you should create switch(which) and assign 'result' to it
            }
        });

        return builder.create();
    }

    public GenericAddress getResult(){
        if(result.size()!=0) {
            return result.get(0);
        }
        return null;
    }
}
