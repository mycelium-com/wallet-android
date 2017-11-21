package com.mycelium.wallet.activity.send.helper;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ExponentialFeeItemsAlgorithmTest {
    private FeeItemsAlgorithm algorithm;
    private final int MIN_POSITION = 12;
    private final int MAX_POSITION = 22;
    private final long MIN_VALUE = 3000;
    private final long MAX_VALUE = 890000;

    @Before
    public void setup() {
        algorithm = new ExponentialFeeItemsAlgorithm(MIN_VALUE, MIN_POSITION, MAX_VALUE, MAX_POSITION);
    }

    @Test
    public void computeValue() throws Exception {
        // due to integer values the following tests fail if the fee gets too low but it's sat/kB.
        assertEquals(MIN_VALUE, algorithm.computeValue(MIN_POSITION));
        assertEquals(MAX_VALUE, algorithm.computeValue(MAX_POSITION));
        assertTrue(algorithm.computeValue(MIN_POSITION + 1) > MIN_VALUE);
        assertTrue(algorithm.computeValue(MAX_POSITION - 1) < MAX_VALUE);
    }

    @Test
    public void getMinPosition() throws Exception {
        assertEquals(MIN_POSITION, algorithm.getMinPosition());
    }

    @Test
    public void getMaxPosition() throws Exception {
        assertEquals(MAX_POSITION, algorithm.getMaxPosition());
    }
}