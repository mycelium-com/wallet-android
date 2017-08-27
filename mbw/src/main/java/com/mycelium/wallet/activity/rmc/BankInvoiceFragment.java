package com.mycelium.wallet.activity.rmc;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mycelium.wallet.R;

/**
 * Created by elvis on 22.06.17.
 */

public class BankInvoiceFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_rmc_bank_invoice, container, false);
    }

}
