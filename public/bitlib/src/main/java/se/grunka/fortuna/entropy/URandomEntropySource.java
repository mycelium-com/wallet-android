package se.grunka.fortuna.entropy;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.accumulator.EventAdder;
import se.grunka.fortuna.accumulator.EventScheduler;

public class URandomEntropySource implements EntropySource {

    private final byte[] bytes = new byte[32];

    @Override
    public void event(EventScheduler scheduler, EventAdder adder) {
        try {
            FileInputStream inputStream = new FileInputStream("/dev/urandom");
            try {
                inputStream.read(bytes);
                adder.add(bytes);
                scheduler.schedule(100, TimeUnit.MILLISECONDS);
            } finally {
                inputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
