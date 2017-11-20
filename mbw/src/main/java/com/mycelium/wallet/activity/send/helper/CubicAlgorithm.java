package com.mycelium.wallet.activity.send.helper;

/**
 * Created by elvis on 20.11.17.
 */

public class CubicAlgorithm implements FeeItemsAlgorithm {
    private int minPos;
    private int maxPos;
    private float a;
    private float b;

    public CubicAlgorithm(long min, int minPos, long max, int maxPos) {
        this.minPos = minPos;
        this.maxPos = maxPos;
        a = (max - min) * 1f / (pow3(maxPos) - pow3(minPos));
        b = min - pow3(minPos) * a;
    }

    public long computeValue(int position) {
        return (long) (pow3(position) * a + b);
    }

    @Override
    public int getMinPosition() {
        return minPos;
    }

    @Override
    public int getMaxPosition() {
        return maxPos;
    }

    public static int pow3(int value) {
        return value * value * value;
    }
}
