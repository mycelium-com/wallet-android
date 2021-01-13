package com.mycelium.wapi.wallet;

import com.mrd.bitlib.model.BitcoinAddress;

import com.mycelium.wapi.wallet.btc.BtcAddress;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.coins.ColuMain;
import com.mycelium.wapi.wallet.eth.EthAddress;
import com.mycelium.wapi.wallet.eth.coins.EthCoin;
import com.mycelium.wapi.wallet.fio.coins.FIOToken;

public class AddressUtils {

    public static Address from(CryptoCurrency currencyType, String address) {
        if (address.length() == 0) {
            return null;
        }
        if (currencyType instanceof BitcoinMain || currencyType instanceof BitcoinTest || currencyType instanceof FIOToken) {
            return currencyType.parseAddress(address);
        } else if (currencyType instanceof ColuMain) {
            BitcoinAddress addr = BitcoinAddress.fromString(address);
            if (addr != null) {
                return new BtcAddress(currencyType, addr);
            } else {
                return null;
            }
        } else if (currencyType instanceof EthCoin) {
            return new EthAddress(currencyType, address);
        } else {
            return null;
        }
    }

    //Use only for bitcoin address
    public static BtcAddress fromAddress(BitcoinAddress address) {
        CryptoCurrency currency = address.getNetwork().isProdnet() ? BitcoinMain.get() : BitcoinTest.get();
        return new BtcAddress(currency, address);
    }

    public static String toMultiLineString(String address) {
        int length = address.length();
        if (length <= 12) {
            return address;
        } else if (length <= 24) {
            return toDoubleLineString(address);
        } else {
            int i = 0;
            StringBuilder result = new StringBuilder();
            while (i + 12 < address.length()) {
                result.append(address, i, i + 12).append(System.lineSeparator());
                i = i + 12;
            }
            return result.append(address.substring(i)).toString();
        }
    }

    public static String toDoubleLineString(String address) {
        int splitIndex = address.length() / 2;
        return address.substring(0, splitIndex) + System.lineSeparator() + address.substring(splitIndex);
    }

    public static String toShortString(String address) {
        int showChars = 3;
        return address.substring(0, showChars) + "..." + address.substring(address.length() - showChars);
    }
}
