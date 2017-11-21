package com.mycelium.wallet.activity.send.helper;

import org.junit.Test;

public class FeeItemsAlgorithmTest {
    @Test
    public void printThemAll() throws Exception {
        long MIN_FEE = 3000;
        long MAX_FEE = 640000;
        int MIN_POSITION = 11;
        int MAX_POSITION = 48; // 38 steps as that is about the count of steps the user would currently get over all fees

        System.out.println("Linear");
        // apparently the linear case ends up slightly off at the end
        printStuff(new LinearAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, MAX_POSITION));

        System.out.println("Cubic");
        // gets going very slowly
        printStuff(new CubicAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, MAX_POSITION));

        System.out.println("Exponential");
        // x^38 is off by one when using double :(
        printStuff(new ExponentialFeeItemsAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, MAX_POSITION));

        System.out.println("RoundNumberAffineExponential 1.1");
        printStuff(new RoundNumberAffineExponentialFeeItemsAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, 1.1));

        System.out.println("RoundNumberAffineExponential 1.5");
        printStuff(new RoundNumberAffineExponentialFeeItemsAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, 1.5));

        System.out.println("RoundNumberAffineExponential 2");
        printStuff(new RoundNumberAffineExponentialFeeItemsAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, 2));

        System.out.println("RoundNumberAffineExponential 3");
        printStuff(new RoundNumberAffineExponentialFeeItemsAlgorithm(MIN_FEE, MIN_POSITION, MAX_FEE, 3));
    }

    private void printStuff(FeeItemsAlgorithm algorithm) {
        for(int position=algorithm.getMinPosition(); position<=algorithm.getMaxPosition(); position++) {
            System.out.println(algorithm.computeValue(position));
        }
    }
}
