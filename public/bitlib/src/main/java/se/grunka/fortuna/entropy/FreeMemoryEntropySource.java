package se.grunka.fortuna.entropy;

import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;
import se.grunka.fortuna.Util;

public class FreeMemoryEntropySource implements EntropySource {
    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        long freeMemory = Runtime.getRuntime().freeMemory();
        adder.add(Util.twoLeastSignificantBytes(freeMemory));
        scheduler.schedule(100, TimeUnit.MILLISECONDS);
    }
}
