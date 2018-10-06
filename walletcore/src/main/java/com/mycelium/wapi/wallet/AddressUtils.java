package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;

public class AddressUtils {

    public static GenericAddress from(CryptoCurrency currencyType, String address) {
        if(currencyType instanceof BitcoinMain || currencyType instanceof BitcoinTest) {
            Address addr = Address.fromString(address);
            return new BtcAddress(currencyType, addr.getAllAddressBytes());
        } else if (currencyType instanceof ColuMain) {
            Address addr = Address.fromString(address);
            return new BtcAddress(currencyType, addr.getAllAddressBytes());
        } else {
            return null;
        }
    }
}
