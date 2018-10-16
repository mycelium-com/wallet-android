package com.mycelium.wallet.activity.send.model;

import android.graphics.drawable.Drawable;

import com.mrd.bitlib.model.Address;
import com.mycelium.wallet.AddressBookManager;
import com.mycelium.wapi.wallet.GenericAddress;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;

import java.lang.reflect.Proxy;
import java.util.UUID;

public class AccountForFee extends AddressBookManager.IconEntry {
    private Value balance;

    public AccountForFee(Address address, String name, Drawable icon, UUID id, Value balance) throws com.mrd.bitlib.model.SegwitAddress.SegwitAddressException {
        super(address.isP2SH(address.getNetwork()) ?
                new SegwitAddress(new com.mrd.bitlib.model.SegwitAddress(address.getNetwork(),
                        0x00,address.getAllAddressBytes())) :
                new BtcAddress(address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get(),
                        address.getAllAddressBytes()), name, icon, id);

        this.balance = balance;
    }

    public Value getBalance() {
        return balance;
    }
}
