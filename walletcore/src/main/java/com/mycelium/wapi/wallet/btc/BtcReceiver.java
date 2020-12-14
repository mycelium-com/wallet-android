package com.mycelium.wapi.wallet.btc;

import com.megiontechnologies.Bitcoins;
import com.mrd.bitlib.model.BitcoinAddress;

import java.io.Serializable;

/**
 * Class representing a receiver of funds
 */
public class BtcReceiver implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * The address to send funds to
     */
    public final BitcoinAddress address;

    /**
     * The amount to send measured in satoshis
     */
    public final long amount;

    public BtcReceiver(BitcoinAddress address, long amount) {
        this.address = address;
        this.amount = amount;
    }

    public BtcReceiver(BitcoinAddress address, Bitcoins amount) {
        this(address, amount.getLongValue());
    }
}
