package com.mycelium.wallet.activity.send.model;

import android.graphics.drawable.Drawable;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wapi.wallet.currency.CurrencyValue;

import java.util.UUID;

public class AccountForFee extends AddressBookManager.IconEntry {
    private CurrencyValue balance;

    public AccountForFee(Address address, String name, Drawable icon, UUID id, CurrencyValue balance) {
        super(address, name, icon, id);
        this.balance = balance;
    }

    public CurrencyValue getBalance() {
        return balance;
    }
}
