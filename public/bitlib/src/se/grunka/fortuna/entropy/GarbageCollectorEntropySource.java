package se.grunka.fortuna.entropy;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;
import se.grunka.fortuna.Util;

public class GarbageCollectorEntropySource implements EntropySource {
    private final List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        long sum = 0;
        for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
            sum += garbageCollectorMXBean.getCollectionCount() + garbageCollectorMXBean.getCollectionTime();
        }
        adder.add(Util.twoLeastSignificantBytes(sum));
        scheduler.schedule(10, TimeUnit.SECONDS);
    }
}
