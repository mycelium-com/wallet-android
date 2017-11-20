package com.mycelium.wallet.activity.send.helper;

/**
 * Created by elvis on 20.11.17.
 */

public interface FeeItemsAlgorithm {
    long computeValue(int position);
    int getMinPosition();
    int getMaxPosition();
}
