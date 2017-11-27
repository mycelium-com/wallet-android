package com.mycelium.wallet.activity.send.helper;

/**
 * value(position) = a * position^3 + b
 * a = (max - min) / (maxPosition^3 - minPosition^3);
 * b = min -  a * minPosition^3 ;
 */

public class CubicAlgorithm implements FeeItemsAlgorithm {
    private int minPosition;
    private int maxPosition;
    private float a;
    private float b;

    public CubicAlgorithm(long min, int minPos, long max, int maxPos) {
        this.minPosition = minPos;
        this.maxPosition = maxPos;
        a = (max - min) * 1f / (pow3(maxPos) - pow3(minPos));
        b = min - pow3(minPos) * a;
    }

    public long computeValue(int position) {
        return (long) (pow3(position) * a + b);
    }

    @Override
    public int getMinPosition() {
        return minPosition;
    }

    @Override
    public int getMaxPosition() {
        return maxPosition;
    }

    public static int pow3(int value) {
        return value * value * value;
    }
}
