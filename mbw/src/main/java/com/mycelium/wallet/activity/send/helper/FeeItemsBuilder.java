package com.mycelium.wallet.activity.send.helper;

import android.support.annotation.NonNull;

import com.mycelium.wallet.ExchangeRateManager;
import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.coins.GenericAssetInfo;
import com.mycelium.wapi.wallet.coins.Value;

import java.util.ArrayList;
import java.util.List;

/**
 * se have 4 dynamic values from server LOWPRIO, ECO, NORMAL, PRIO
 * FeeItemsBuilder divide values  MIN_NON_ZERO_FEE_PER_KB..LOWPRIO..ECO..NORMAL..PRIO..1.5*PRIO
 * on 8 part,
 * LOWPRIO tab
 * lower part - MIN_NON_ZERO_FEE_PER_KB..LOWPRIO
 * upper part - LOWPRIO..(LOWPRIO+ECO)/2
 * ECO tab
 * lower part - (LOWPRIO+ECO)/2..ECO
 * upper part - ECO..(ECO+NORMAL)/2
 * NORMAL tab
 * lower part - (ECO+NORMAL)/2..NORMAL
 * uppper part - NORMAL..(NORMAL+PRIO)/2
 * PRIO
 * lower part - (NORMAL+PRIO)/2..PRIO
 * upper part - PRIO..(1.5*PRIO)
 */
public class FeeItemsBuilder {
    private static final int MIN_NON_ZERO_FEE_PER_KB = 1000;
    private static final float MIN_FEE_INCREMENT = 1.025f; // fee(n+1) > fee(n) * MIN_FEE_INCREMENT

    private ExchangeRateManager exchangeRateManager;
    private GenericAssetInfo fiatType;

    public FeeItemsBuilder(ExchangeRateManager exchangeRateManager, GenericAssetInfo fiatType) {
        this.exchangeRateManager = exchangeRateManager;
        this.fiatType = fiatType;
    }

    public List<FeeItem> getFeeItemList(GenericAssetInfo asset, FeeEstimationsGeneric feeEstimation, MinerFee minerFee, int txSize) {
        long min = MIN_NON_ZERO_FEE_PER_KB;
        long current = 0;
        long previous = 0;
        long next = 0;
        switch (minerFee) {
            case LOWPRIO:
                current = feeEstimation.getLow().getValue();
                next = feeEstimation.getEconomy().getValue();
                break;
            case ECONOMIC:
                current = feeEstimation.getEconomy().getValue();
                previous = feeEstimation.getLow().getValue();
                next = feeEstimation.getNormal().getValue();
                break;
            case NORMAL:
                current = feeEstimation.getNormal().getValue();
                previous = feeEstimation.getEconomy().getValue();
                next = feeEstimation.getHigh().getValue();
                break;
            case PRIORITY:
                current = feeEstimation.getHigh().getValue();
                previous = feeEstimation.getNormal().getValue();
                break;
        }

        if (minerFee != MinerFee.LOWPRIO) {
            min = (current + previous) / 2;
        }

        long max = 3 * feeEstimation.getHigh().getValue() / 2;
        if (minerFee != MinerFee.PRIORITY) {
            max = (next + current) / 2;
        }

        FeeItemsAlgorithm algorithmLower = new LinearAlgorithm(min, 1, max, 10);
        FeeItemsAlgorithm algorithmUpper;
        if (minerFee == MinerFee.LOWPRIO) {
            algorithmLower = new ExponentialLowPrioAlgorithm(min, current);
        }

        List<FeeItem> feeItems = new ArrayList<>();
        addItemsInRange(asset, feeItems, algorithmLower, txSize);
        if (minerFee == MinerFee.LOWPRIO) {
            algorithmUpper = new LinearAlgorithm(current, algorithmLower.getMaxPosition() + 1
                    , max, algorithmLower.getMaxPosition() + 4);
            addItemsInRange(asset, feeItems, algorithmUpper, txSize);
        }

        return feeItems;
    }

    private void addItemsInRange(GenericAssetInfo asset, List<FeeItem> feeItems, FeeItemsAlgorithm algorithm, int txSize) {
        for (int i = algorithm.getMinPosition(); i < algorithm.getMaxPosition(); i++) {
            FeeItem currFeeItem = createFeeItem(asset, txSize, algorithm.computeValue(i));
            FeeItem prevFeeItem = feeItems.size() > 0 ? feeItems.get(feeItems.size() - 1) : null;
            boolean canAdd = prevFeeItem != null ? prevFeeItem.feePerKb < currFeeItem.feePerKb : true;

            if (currFeeItem.value != null && prevFeeItem != null && prevFeeItem.value != null
                    && currFeeItem.fiatValue != null && prevFeeItem.fiatValue != null) {
                String thisFiatFee = currFeeItem.fiatValue.toString();
                String prevFiatFee = prevFeeItem.fiatValue.toString();

                // if we reached this, then we can override canAdd
                canAdd = (float) currFeeItem.feePerKb / prevFeeItem.feePerKb >= MIN_FEE_INCREMENT && !thisFiatFee.equals(prevFiatFee);
            }

            if (canAdd) {
                feeItems.add(currFeeItem);
            }
        }
    }

    @NonNull
    private FeeItem createFeeItem(GenericAssetInfo asset, int txSize, long feePerKb) {
        Value fee = Value.valueOf(asset, txSize * feePerKb / 1000);
        Value fiatFee = exchangeRateManager.get(fee, fiatType);
        return new FeeItem(feePerKb, fee, fiatFee, FeeViewAdapter.VIEW_TYPE_ITEM);
    }
}
