package com.mycelium.wallet.activity.send.helper;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.Map;

import static java.lang.Math.ceil;
import static java.lang.Math.exp;
import static java.lang.Math.floor;
import static java.lang.Math.log;
import static java.lang.Math.log10;
import static java.lang.Math.pow;
import static java.lang.Math.round;

/**
 * This FeeItemsAlgorithm distributes the values at minPos to maxPos such that the relative distance
 * from one position to the next is at max the given scale, introducing at most one extra level per
 * range x to 10x.
 * value(position + 1) <= value(position) * scale
 * with the added properties that
 * * powers of 10 are hit exactly
 * * at most 2 significant digits are used
 * * minValue and maxValue are hit exactly
 */
public class RoundNumberAffineExponentialFeeItemsAlgorithm implements FeeItemsAlgorithm {
    private final int minPosition;
    private final int maxPosition;
    @SuppressLint("UseSparseArrays")
    private final Map<Integer, Long> items = new HashMap<>();

    public RoundNumberAffineExponentialFeeItemsAlgorithm(long minValue, int minPosition, long maxValue, double scaleFactor) {
        this.minPosition = minPosition;
        int minDecimalValue = (int) pow(10, floor(log10(minValue)));
        int maxDecimalValue = (int) pow(10, floor(log10(maxValue)));
        int decimalStepCount = (int) ceil(log(10) / log(scaleFactor));
        double scale = exp(log(10.0) / (double) decimalStepCount);

        // fill the map
        int position = minPosition;
        long value = minValue;
        items.put(position++, value);
        for (int decimal = minDecimalValue; decimal <= maxDecimalValue; decimal *= 10) {
            for (int i = 0; i <= decimalStepCount; i++) {
                long x = (long) (pow(scale, i) * decimal);
                // round to 2 significant digits
                x = (long) (round((double) x * 10 / decimal)) * decimal / 10;
                if (x >= maxValue) {
                    break;
                }
                if (x > value) {
                    value = x;
                    items.put(position++, value);
                }
            }
        }
        items.put(position, maxValue);
        maxPosition = position;
    }

    @Override
    public long computeValue(int position) {
        if (position >= minPosition && position <= maxPosition) {
            return items.get(position);
        }
        throw new IllegalArgumentException();
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
