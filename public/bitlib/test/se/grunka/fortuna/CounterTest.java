package se.grunka.fortuna;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class CounterTest {

    private Counter counter;

    @Before
    public void before() {
        counter = new Counter(128);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForInvalidNumberOfBits() throws Exception {
        new Counter(127);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldFailForTooFewBits() throws Exception {
        new Counter(0);
    }

    @Test
    public void shouldRollOverBackToZero() throws Exception {
        Counter smallCounter = new Counter(8);
        for (int i = 0; i < 256; i++) {
            smallCounter.increment();
        }
        assertTrue(smallCounter.isZero());
    }

    @Test
    public void shouldBeZero() throws Exception {
        assertTrue(counter.isZero());
    }

    @Test
    public void shouldIncrementOneStep() throws Exception {
        counter.increment();
        byte[] state = counter.getState();
        assertEquals(16, state.length);
        assertEquals(1, state[0]);
        assertFalse(counter.isZero());
    }

    @Test
    public void shouldFillFirstByteWithOnes() throws Exception {
        for (int i = 0; i < 255; i++) {
            counter.increment();
        }
        byte[] state = counter.getState();
        assertEquals((byte) 0xff, state[0]);
    }

    @Test
    public void shouldRollOverIntoNextByte() throws Exception {
        for (int i = 0; i < 256; i++) {
            counter.increment();
        }
        byte[] state = counter.getState();
        assertEquals((byte) 0x0, state[0]);
        assertEquals((byte) 0x1, state[1]);
    }

    @Test
    public void shouldRollOverIntoNextByteAgain() throws Exception {
        for (int i = 0; i < 257; i++) {
            counter.increment();
        }
        byte[] state = counter.getState();
        assertEquals((byte) 0x1, state[0]);
        assertEquals((byte) 0x1, state[1]);
    }

    @Test
    public void shouldRollOverIntoThirdByte() throws Exception {
        for (int i = 0; i < 256*256; i++) {
            counter.increment();
        }
        byte[] state = counter.getState();
        assertEquals((byte) 0x0, state[0]);
        assertEquals((byte) 0x0, state[1]);
        assertEquals((byte) 0x1, state[2]);
    }
}
