package com.mycelium.wallet.external.changelly;

class CurrencyInfo {
    private final String name;
    private final int smallIcon;

    CurrencyInfo(String name, int smallIcon) {
        this.name = name;
        this.smallIcon = smallIcon;
    }

    final String getName() {
        return name;
    }

    int getSmallIcon() {
        return smallIcon;
    }
}
