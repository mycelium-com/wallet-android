package com.mycelium.wapi.wallet.btc;

import com.mycelium.wapi.wallet.SendRequest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.coins.Value;

public class BtcSendRequest extends SendRequest<BtcTransaction> {

    public BtcSendRequest(CryptoCurrency type) {
        super(type);
    }

    public static BtcSendRequest to(BtcAddress destination, Value amount) {
        BtcSendRequest req = new BtcSendRequest(destination.getCoinType());
        return req;
    }
}
