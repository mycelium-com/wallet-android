package se.grunka.fortuna.entropy;

import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;

import static org.junit.Assert.assertEquals;

public class SchedulingEntropySourceTest {

    private SchedulingEntropySource target;
    private int schedules;
    private int adds;

    @Before
    public void before() throws Exception {
        target = new SchedulingEntropySource();
        schedules = 0;
        adds = 0;
    }

    @Test
    public void shouldUseTimeBetweenCallsToCreateEvents() throws Exception {
        target.event(
                new EventScheduler() {
                    @Override
                    public void schedule(long delay, TimeUnit timeUnit) {
                        assertEquals(10, timeUnit.toMillis(delay));
                        schedules++;
                    }
                },
                new EventAdder() {
                    @Override
                    public void add(byte[] event) {
                        assertEquals(2, event.length);
                        adds++;
                    }
                }
        );
        assertEquals(1, schedules);
        assertEquals(1, adds);
    }
}
