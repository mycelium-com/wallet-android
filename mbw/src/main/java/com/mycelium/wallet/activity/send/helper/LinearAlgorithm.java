package com.mycelium.wallet.activity.send.helper;

/**
 * Created by elvis on 20.11.17.
 */

public class LinearAlgorithm implements FeeItemsAlgorithm {
    private int minPos;
    private int maxPos;

    private float a;
    private float b;

    public LinearAlgorithm(long min, int minPos, long max, int maxPos) {
        this.minPos = minPos;
        this.maxPos = maxPos;

        a = (max - min) / (maxPos - minPos);
        b = min - minPos * a;
    }

    @Override
    public long computeValue(int position) {
        return (long) (a * position + b);
    }

    @Override
    public int getMinPosition() {
        return minPos;
    }

    @Override
    public int getMaxPosition() {
        return maxPos;
    }
}
