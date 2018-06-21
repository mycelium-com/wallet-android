package com.mrd.bitlib.model;

import com.mrd.bitlib.util.ByteWriter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

class TransactionWitness implements Serializable {
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

    public int getStackSize() {
        return stack.size();
    }

    public void toByteWriter(ByteWriter writer) {
        writer.putCompactInt(stack.size());
        for (byte[] element : stack) {
            writer.putCompactInt(element.length);
            writer.putBytes(element);
        }
    }
}
