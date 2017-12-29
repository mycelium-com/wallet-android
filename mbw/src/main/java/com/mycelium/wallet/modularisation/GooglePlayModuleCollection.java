package com.mycelium.wallet.modularisation;


import android.content.Context;

import com.mycelium.modularizationtools.model.Module;
import com.mycelium.wallet.BuildConfig;
import com.mycelium.wallet.R;

import java.util.HashMap;
import java.util.Map;

public class GooglePlayModuleCollection {
    public static Map<String, Module> getModules(Context context) {
        Map<String, Module> result = new HashMap<>();
        result.put("bch", new Module("com.mycelium.module.spvbch"
                + (BuildConfig.FLAVOR.equals("btctestnet") ? ".testnet" : "")
                + (BuildConfig.DEBUG ? ".debug" : "")
                , context.getString(R.string.bitcoin_cash_module)
                , context.getString(R.string.bch_module_description)));

        return result;
    }
}
