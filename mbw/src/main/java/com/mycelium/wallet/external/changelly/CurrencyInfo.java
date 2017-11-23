package com.mycelium.wallet.external.changelly;

public class CurrencyInfo {
    private final String name;
    private final int smallIcon;

    public CurrencyInfo(String name, int smallIcon) {
        this.name = name;
        this.smallIcon = smallIcon;
    }

    public final String getName() {
        return name;
    }

    public int getSmallIcon() {
        return smallIcon;
    }

}
