package com.mrd.bitlib.model;

import java.util.ArrayList;
import java.util.List;

class TransactionWitness {
    public static final TransactionWitness EMPTY = new TransactionWitness(0);

    private final List<byte[]> stack;
    private static final int MAX_INITIAL_ARRAY_LENGTH = 20;

    public TransactionWitness(int pushCount) {
        stack = new ArrayList<>(Math.min(pushCount, MAX_INITIAL_ARRAY_LENGTH));
    }

    public void setStack(int i, byte[] value) {
        while (i >= stack.size()) {
            stack.add(new byte[]{});
        }
        stack.set(i, value);
    }
}
