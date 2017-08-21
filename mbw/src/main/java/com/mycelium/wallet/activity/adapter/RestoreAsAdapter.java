package com.mycelium.wallet.activity.adapter;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.colu.ColuAccount;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by elvis on 21.08.17.
 */

public class RestoreAsAdapter extends ArrayAdapter<String> {
    private MbwManager mbwManager;
    private Address address;
    private Map<ColuAccount.ColuAssetType, Boolean> checkedMap = new HashMap<>();

    public RestoreAsAdapter(@NonNull Context context, MbwManager mbwManager, Address address) {
        super(context, 0, getStrings(mbwManager));
        this.mbwManager = mbwManager;
        this.address = address;
    }

    @NonNull
    private static List<String> getStrings(MbwManager mbwManager) {
        List<String> list = ColuAccount.ColuAsset.getAllAssetNames(mbwManager.getNetwork());
        list.add(0, "BTC");
        return list;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        CheckBox itemView;
        if (convertView == null) {
            itemView = new CheckBox(getContext());
            int padding = getContext().getResources().getDimensionPixelSize(R.dimen.margin_medium);
            itemView.setPadding(padding, padding, padding, padding);
        } else {
            itemView = (CheckBox) convertView;

        }
        String item = getItem(position);
        itemView.setText(item);
        itemView.setTag(position);
        ColuAccount.ColuAssetType type = ColuAccount.ColuAssetType.parse(item);
        boolean hasAccountWithType = type != null && mbwManager.getColuManager().hasAccountWithType(address, type);
        itemView.setEnabled(!hasAccountWithType);
        final boolean checked = checkedMap.get(type) != null ? checkedMap.get(type) : false;
        itemView.setChecked(hasAccountWithType || checked);
        itemView.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                ColuAccount.ColuAssetType type = ColuAccount.ColuAssetType.parse(getItem((Integer) compoundButton.getTag()));
                checkedMap.put(type, b);
            }
        });
        return itemView;
    }


    public List<ColuAccount.ColuAsset> getColuAssets() {
        List<ColuAccount.ColuAsset> result = new ArrayList<>();
        for (Map.Entry<ColuAccount.ColuAssetType, Boolean> coluAssetTypeBooleanEntry : checkedMap.entrySet()) {
            if (coluAssetTypeBooleanEntry.getValue() && coluAssetTypeBooleanEntry.getKey() != null) {
                result.add(ColuAccount.ColuAsset.getByType(coluAssetTypeBooleanEntry.getKey(), mbwManager.getNetwork()));
            } else if (coluAssetTypeBooleanEntry.getKey() == null) {
                result.add(null);
            }
        }
        return result;
    }
}
