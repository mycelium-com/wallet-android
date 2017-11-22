package com.mycelium.wallet.activity.send.helper;

/**
 * Wrapper on ExponentialFeeItemsAlgorithm for Low Prio fees
 * values will not change depends from server value, instead we change count of steps
 */

public class ExponentialLowPrioAlgorithm implements FeeItemsAlgorithm {
    private long minValue;
    private int minPosition;
    private int maxPosition;
    private ExponentialFeeItemsAlgorithm algorithm;

    public ExponentialLowPrioAlgorithm(long minValue, long maxValue) {
        minPosition = 1;
        algorithm = new ExponentialFeeItemsAlgorithm(minValue, 1, 140000, 15);
        maxPosition = minPosition;
        while (algorithm.computeValue(maxPosition + 1) < maxValue) {
            maxPosition++;
        }
    }

    @Override
    public long computeValue(int position) {
        return algorithm.computeValue(position);
    }

    @Override
    public int getMinPosition() {
        return minPosition;
    }

    @Override
    public int getMaxPosition() {
        return maxPosition;
    }
}
