package se.grunka.fortuna.entropy;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.Util;
import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;

public class UptimeEntropySource implements EntropySource {
    private final RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        long uptime = runtimeMXBean.getUptime();
        adder.add(Util.twoLeastSignificantBytes(uptime));
        scheduler.schedule(1, TimeUnit.SECONDS);
    }
}
