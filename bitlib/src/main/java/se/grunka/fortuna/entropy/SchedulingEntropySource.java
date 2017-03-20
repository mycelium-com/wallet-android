package se.grunka.fortuna.entropy;

import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;
import se.grunka.fortuna.Util;

public class SchedulingEntropySource implements EntropySource {
    private long lastTime = 0;

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        long now = System.nanoTime();
        long elapsed = now - lastTime;
        lastTime = now;
        adder.add(Util.twoLeastSignificantBytes(elapsed));
        scheduler.schedule(10, TimeUnit.MILLISECONDS);
    }
}
