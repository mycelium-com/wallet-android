package com.mycelium.wapi.wallet;

public class Util {
    /**
     * the method is used to remove additional characters indicating testnet coins from currencies' symbols
     * before making request to the server with these symbols as parameters, as server provides
     * exchange rates only by pure symbols, i.e. BTC and not tBTC
     */
    public static String trimTestnetSymbolDecoration(String symbol) {
        if (symbol.equals("tBTC")) {
            return symbol.substring(1);
        }
        if (symbol.equals("MTt")) {
            return symbol.substring(0, symbol.length() - 1);
        }
        return symbol;
    }

    public static String addTestnetSymbolDecoration(String symbol) {
        if (symbol.equals("BTC")) {
            return "t" + symbol;
        }
        if (symbol.equals("MT")) {
            return symbol + "t";
        }
        return symbol;
    }
}
