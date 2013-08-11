package se.grunka.fortuna.accumulator;

public class Context {
    public final EntropySource source;
    public final EventAdder adder;
    public final EventScheduler scheduler;

    Context(EntropySource source, EventAdder adder, EventScheduler scheduler) {
        this.source = source;
        this.adder = adder;
        this.scheduler = scheduler;
    }
}
