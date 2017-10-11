package com.mycelium.wallet.activity.send.model;

import android.graphics.drawable.Drawable;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.util.UUID;

/**
 * Created by elvis on 11.09.17.
 */

public class AccountForFee extends AddressBookManager.IconEntry {
    private CurrencyValue balance;

    public AccountForFee(Address address, String name, Drawable icon, CurrencyValue balance) {
        super(address, name, icon);
        this.balance = balance;
    }

    public AccountForFee(Address address, String name, Drawable icon, UUID id, CurrencyValue balance) {
        super(address, name, icon, id);
        this.balance = balance;
    }

    public CurrencyValue getBalance() {
        return balance;
    }
}
