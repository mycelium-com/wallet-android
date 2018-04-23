package com.mycelium.wapi.model;

public class IssuedKeysInfo {
    private int internalKeys;
    private int externalKeys;

    public IssuedKeysInfo(int externalKeys, int internalKeys) {
        this.internalKeys = internalKeys;
        this.externalKeys = externalKeys;
    }

    public int getInternalKeys() {
        return internalKeys;
    }

    public int getExternalKeys() {
        return externalKeys;
    }
}
