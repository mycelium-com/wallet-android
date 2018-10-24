package com.mycelium.wallet.activity.send.model;

import android.graphics.drawable.Drawable;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;

import java.util.UUID;

public class AccountForFee extends AddressBookManager.IconEntry {
    private Value balance;

    public AccountForFee(Address address, String name, Drawable icon, UUID id, Value balance) {
        super(AddressUtils.fromAddress(address), name, icon, id);
        this.balance = balance;
    }

    public Value getBalance() {
        return balance;
    }
}
