package com.mycelium.wapi.wallet.btc;

import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.model.Address;

import java.io.Serializable;

/**
 * Class representing a receiver of funds
 */
public class BtcReceiver implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The address to send funds to
     */
    public final Address address;

    /**
     * The amount to send measured in satoshis
     */
    public final long amount;

    public BtcReceiver(Address address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public BtcReceiver(Address address, Bitcoins amount) {
        this(address, amount.getLongValue());
    }
}
