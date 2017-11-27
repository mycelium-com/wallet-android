package com.mycelium.wallet.activity.send.helper;

import static java.lang.Math.exp;
import static java.lang.Math.log;
import static java.lang.Math.pow;
import static java.lang.Math.round;

/**
 * This FeeItemsAlgorithm distributes the values at minPos to maxPos such that the relative distance
 * from one position to the next is a constant scale factor.
 * value(position) = minValue * scale ^ (position - minPosition)
 */
public class ExponentialFeeItemsAlgorithm implements FeeItemsAlgorithm {
    private long minValue;
    private int minPosition;
    private int maxPosition;
    private double scale;

    public ExponentialFeeItemsAlgorithm(long minValue, int minPosition, long maxValue, int maxPosition) {
        this.minPosition = minPosition;
        this.maxPosition = maxPosition;
        this.minValue = minValue;
        int steps = maxPosition - minPosition;
        scale = exp(log((double)maxValue/(double)minValue)/(double)steps);
    }

    @Override
    public long computeValue(int position) {
        int step = position - minPosition;
        return round((double)minValue * pow(scale, step));
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
