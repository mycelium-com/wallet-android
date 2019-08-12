package com.mycelium.wapi.wallet.coins.families;

public enum Families {
    NXT("nxt"),
    FIAT("fiat"),
    BITCOIN("bitcoin"),
    NUBITS("nubits"),
    PEERCOIN("peercoin"),
    REDDCOIN("reddcoin"),
    VPNCOIN("vpncoin"),
    CLAMS("clams"),
    ETHEREUM("ethereum");

    public final String family;

    Families(String family) {
        this.family = family;
    }

    @Override
    public String toString() {
        return family;
    }
}