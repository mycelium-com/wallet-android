package com.mycelium.wallet.activity.send.helper;

import android.support.annotation.NonNull;


import com.mycelium.wallet.MinerFee;
import com.mycelium.wallet.activity.send.adapter.FeeViewAdapter;
import com.mycelium.wallet.activity.send.model.FeeItem;
import com.mycelium.wapi.wallet.FeeEstimationsGeneric;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.coins.Value;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import java.util.ArrayList;
import java.util.List;

/**
 * se have 4 dynamic values from server LOWPRIO, ECO, NORMAL, PRIO
 * FeeItemsBuilder divide values  MIN_NON_ZIRO_FEE_PER_KB..LOWPRIO..ECO..NORMAL..PRIO..1.5*PRIO
 * on 8 part,
 * LOWPRIO tab
 * lower part - MIN_NON_ZIRO_FEE_PER_KB..LOWPRIO
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
    private static final int MIN_NON_ZIRO_FEE_PER_KB = 1000;
    private static final float MIN_FEE_INCREMENT = 1.025f; // fee(n+1) > fee(n) * MIN_FEE_INCREMENT

    private FeeEstimationsGeneric feeEstimation;

    public FeeItemsBuilder(FeeEstimationsGeneric feeEstimation) {
        this.feeEstimation = feeEstimation;
    }

    public List<FeeItem> getFeeItemList(FeeEstimationsGeneric feeEstimation, MinerFee minerFee, int txSize) {
        long min = MIN_NON_ZIRO_FEE_PER_KB;
        long current = 0;
        long previous = 0;
        long next = 0;
        switch (minerFee){
            case LOWPRIO:
                current = feeEstimation.getLow().value;
                next = feeEstimation.getNormal().value;
                break;
            case NORMAL:
                current = feeEstimation.getNormal().value;
                previous = feeEstimation.getLow().value;
                next = feeEstimation.getHigh().value;
                break;
            case PRIORITY:
                current = feeEstimation.getHigh().value;
                previous = feeEstimation.getNormal().value;
            break;
        }

        if (minerFee != MinerFee.LOWPRIO) {
            min = (current + previous) / 2;
        }

        long max = 3 * feeEstimation.getHigh().value / 2;
        if (minerFee != MinerFee.PRIORITY) {
            max = (next + current) / 2;
        }

        FeeItemsAlgorithm algorithmLower = new LinearAlgorithm(min, 1, max, 10);
        FeeItemsAlgorithm algorithmUpper;
        if (minerFee == MinerFee.LOWPRIO) {
            algorithmLower = new ExponentialLowPrioAlgorithm(min, current);
        }

        List<FeeItem> feeItems = new ArrayList<>();
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));
        addItemsInRange(feeItems, algorithmLower, txSize);
        if (minerFee == MinerFee.LOWPRIO) {
            algorithmUpper = new LinearAlgorithm(current, algorithmLower.getMaxPosition()+1
                    , max, algorithmLower.getMaxPosition() + 4);
            addItemsInRange(feeItems, algorithmUpper, txSize);
        }
        feeItems.add(new FeeItem(0, null, null, FeeViewAdapter.VIEW_TYPE_PADDING));

        return feeItems;
    }

    private void addItemsInRange(List<FeeItem> feeItems, FeeItemsAlgorithm algorithm, int txSize) {
        for (int i = algorithm.getMinPosition(); i < algorithm.getMaxPosition(); i++) {
            FeeItem currFeeItem = createFeeItem(txSize, algorithm.computeValue(i));
            FeeItem prevFeeItem = feeItems.get(feeItems.size() - 1);
            boolean canAdd = prevFeeItem.feePerKb < currFeeItem.feePerKb;

            if(currFeeItem.value != null && prevFeeItem.value != null) {
                String thisFiatFee = currFeeItem.value.toString();
                String prevFiatFee = prevFeeItem.value.toString();

                // if we reached this, then we can override canAdd
                canAdd = (float) currFeeItem.feePerKb / prevFeeItem.feePerKb >= MIN_FEE_INCREMENT && !thisFiatFee.equals(prevFiatFee);
            }

            if (canAdd) {
                feeItems.add(currFeeItem);
            }
        }
    }

    @NonNull
    private FeeItem createFeeItem(int txSize, long feePerKb) {
        ExactBitcoinValue bitcoinValue;
        bitcoinValue = ExactBitcoinValue.from(txSize * feePerKb / 1000);
        Value fiatFee = Value.valueOf(BitcoinTest.get(), txSize * feePerKb / 1000);
        return new FeeItem(feePerKb, bitcoinValue.getAsBitcoin(), fiatFee, FeeViewAdapter.VIEW_TYPE_ITEM);
    }
}
