package com.mycelium.wapi.wallet.metadata;

public interface IMetaDataStorage {
    void storeKeyCategoryValueEntry(final MetadataKeyCategory keyCategory, final String value);
    String getKeyCategoryValueEntry(final String key, final String category, final String defaultValue);
}
