package com.mycelium.wapi.wallet;

import com.mycelium.wapi.wallet.coins.GenericAssetInfo;

public interface CommonAccountBacking {
    void beginTransaction();
    void setTransactionSuccessful();
    void endTransaction();
    void clear();

    void saveLastFeeEstimation(FeeEstimationsGeneric feeEstimation, GenericAssetInfo assetType);
    FeeEstimationsGeneric loadLastFeeEstimation(GenericAssetInfo assetType);
}
