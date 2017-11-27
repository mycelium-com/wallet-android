package com.mycelium.wallet.activity.send.helper;

/**
 * interface for get fee items value sat/kB
 */
public interface FeeItemsAlgorithm {
    long computeValue(int position);
    int getMinPosition();
    int getMaxPosition();
}
