package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.segwit.SegwitAddress;

public class AddressUtils {

    public static GenericAddress from(CryptoCurrency currencyType, String address) {
        if(currencyType instanceof BitcoinMain || currencyType instanceof BitcoinTest) {
            Address addr = Address.fromString(address);
            return new BtcLegacyAddress(currencyType, addr.getAllAddressBytes());
        } else if (currencyType instanceof ColuMain) {
            Address addr = Address.fromString(address);
            return new BtcLegacyAddress(currencyType, addr.getAllAddressBytes());
        } else {
            return null;
        }
    }

    public static GenericAddress fromAddress(Address address){
        GenericAddress res = null;
            res = (address.getType() == AddressType.P2WPKH) ?
                        new SegwitAddress((com.mrd.bitlib.model.SegwitAddress) address) :
                        new BtcLegacyAddress(address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get(),
                                address.getAllAddressBytes());
        return res;
    }

    public static String toMultiLineString(String address){
        return address.substring(0, 12) + "\r\n" +
                address.substring(12, 24) + "\r\n" +
                address.substring(24);
    }

    public static String toDoubleLineString(String address){
        int splitIndex = address.length() / 2;
        return address.substring(0, splitIndex) + "\r\n" + address.substring(splitIndex);
    }

    public static String toShortString(String address){
        int showChars = 3;
        return address.substring(0, showChars) + "..." + address.substring(address.length() - showChars);
    }
}
