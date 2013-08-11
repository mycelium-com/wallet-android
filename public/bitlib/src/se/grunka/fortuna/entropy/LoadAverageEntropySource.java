package se.grunka.fortuna.entropy;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;
import se.grunka.fortuna.Util;

public class LoadAverageEntropySource implements EntropySource {

    private final OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();
        BigDecimal value = BigDecimal.valueOf(systemLoadAverage);
        long convertedValue = value.movePointRight(value.scale()).longValue();
        adder.add(Util.twoLeastSignificantBytes(convertedValue));
        scheduler.schedule(1000, TimeUnit.MILLISECONDS);
    }
}
