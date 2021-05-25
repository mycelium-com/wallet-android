package com.mycelium.wapi.wallet.btcvault

import com.megiontechnologies.Bitcoins

/**
 * address - The address to send funds to
 * amount - The amount to send measured in satoshis
 */

class BtcvReceiver(var address: BtcvAddress, var amount: Long = 0) {
    private val serialVersionUID = 1L

    constructor(address: BtcvAddress, amount: Bitcoins) : this(address, amount.longValue)
}