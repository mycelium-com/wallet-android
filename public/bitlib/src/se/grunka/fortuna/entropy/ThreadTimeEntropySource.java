package se.grunka.fortuna.entropy;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.Util;
import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;

public class ThreadTimeEntropySource implements EntropySource {

    private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        long threadTime = threadMXBean.getCurrentThreadCpuTime() + threadMXBean.getCurrentThreadUserTime();
        adder.add(Util.twoLeastSignificantBytes(threadTime));
        scheduler.schedule(100, TimeUnit.MILLISECONDS);
    }
}
