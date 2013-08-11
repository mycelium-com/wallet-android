package se.grunka.fortuna.accumulator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import se.grunka.fortuna.Pool;

public class Accumulator {
    private final Map<Integer, Context> eventContexts = new ConcurrentHashMap<Integer, Context>();
    private final AtomicInteger sourceCount = new AtomicInteger(0);
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
        private final ThreadFactory delegate = Executors.defaultThreadFactory();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = delegate.newThread(r);
            thread.setDaemon(true);
            return thread;
        }
    });
    private final Pool[] pools;

    public Accumulator(Pool[] pools) {
        this.pools = pools;
    }

    public void addSource(EntropySource entropySource) {
        int sourceId = sourceCount.getAndIncrement();
        EventAdder eventAdder = new EventAdderImpl(sourceId, pools);
        EventScheduler eventScheduler = new EventSchedulerImpl(sourceId, eventContexts, scheduler);
        Context context = new Context(entropySource, eventAdder, eventScheduler);
        eventContexts.put(sourceId, context);
        eventScheduler.schedule(0, TimeUnit.MILLISECONDS);
    }

}
