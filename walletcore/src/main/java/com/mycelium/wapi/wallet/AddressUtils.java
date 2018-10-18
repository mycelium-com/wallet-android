package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.btc.BtcLegacyAddress;
import com.mycelium.wapi.wallet.coins.BitcoinMain;
import com.mycelium.wapi.wallet.coins.BitcoinTest;
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
        GenericAddress result = null;
        try {
            result = (address instanceof com.mrd.bitlib.model.SegwitAddress) ?
                    new SegwitAddress(new com.mrd.bitlib.model.SegwitAddress(address.getNetwork(),0x00,
                            address.getAllAddressBytes())) :
                    new BtcLegacyAddress(address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get(),
                            address.getAllAddressBytes());
        } catch (com.mrd.bitlib.model.SegwitAddress.SegwitAddressException e) {
            e.printStackTrace();
        } finally {
            return result;
        }
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
