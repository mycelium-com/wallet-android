package com.mycelium.wallet;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class AndroidRandomSourceTest {
    @Test
    public void testNextBytes() throws Exception {
        // TODO: 11/24/16 this is basic and only tests if we get any variance at all. Do real test!!
        AndroidRandomSource androidRandomSource = new AndroidRandomSource();
        byte[] bytes = new byte[20];
        androidRandomSource.nextBytes(bytes);
        Set<String> strings = new HashSet<>(1000);
        strings.add(new String(bytes));
        strings.add(new String(bytes));
        assertEquals("testing the test", 1, strings.size());
        strings.clear();
        for(int i=0; i < 1000; i++) {
            androidRandomSource.nextBytes(bytes);
            strings.add(new String(bytes));
        }
        assertEquals("a repetition of 20 bytes within 1000 iterations would be a big red flag!", 1000, strings.size());
    }
}