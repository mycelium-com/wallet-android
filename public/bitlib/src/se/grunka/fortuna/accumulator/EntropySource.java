package se.grunka.fortuna.accumulator;

public interface EntropySource {

    void event(EventScheduler scheduler, EventAdder adder);
}
