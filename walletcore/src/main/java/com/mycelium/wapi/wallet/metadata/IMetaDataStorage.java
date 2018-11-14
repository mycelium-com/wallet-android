package com.mycelium.wapi.wallet.metadata;

import com.google.common.base.Optional;

public interface IMetaDataStorage {
    void storeKeyCategoryValueEntry(final MetadataKeyCategory keyCategory, final String value);
    String getKeyCategoryValueEntry(final String key, final String category, final String defaultValue);
    Optional<String> getFirstKeyForCategoryValue(final String category, final String value);
}
