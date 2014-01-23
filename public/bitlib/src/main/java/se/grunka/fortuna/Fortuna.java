package se.grunka.fortuna;

import java.io.File;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

import com.google.common.collect.ImmutableList;
import se.grunka.fortuna.accumulator.Accumulator;
import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.entropy.*;

public class Fortuna extends Random {
   private static final long serialVersionUID = 1L;
   
   private static final int MIN_POOL_SIZE = 64;
   private static final int[] POWERS_OF_TWO = initializePowersOfTwo();

   private static int[] initializePowersOfTwo() {
      int[] result = new int[32];
      for (int power = 0; power < result.length; power++) {
         result[power] = (int) Math.pow(2, power);
      }
      return result;
   }

   private long lastReseedTime = 0;
   private long reseedCount = 0;
   private final Generator generator;
   private final Pool[] pools;
   private final ReentrantLock lock = new ReentrantLock();

   public static Fortuna createInstance() {
      return createInstance(defaultSources());
   }

   private static Iterable<EntropySource> defaultSources() {
      ImmutableList.Builder<EntropySource> b = ImmutableList.builder();
      b.add(new SchedulingEntropySource()
              , new GarbageCollectorEntropySource()
              , new LoadAverageEntropySource()
              , new FreeMemoryEntropySource()
              , new ThreadTimeEntropySource()
              , new UptimeEntropySource());
      if (new File("/dev/urandom").exists()) {
         b.add(new PlatformEntropySource());
      }
      return b.build();

   }

   public static Fortuna createInstance(Iterable<EntropySource> sources) {
      Pool[] pools = new Pool[32];
      for (int pool = 0; pool < pools.length; pool++) {
         pools[pool] = new Pool();
      }
      Accumulator accumulator = new Accumulator(pools);
      for (EntropySource source : sources) {
         accumulator.addSource(source);
      }
      while (pools[0].size() < MIN_POOL_SIZE) {
         try {
            Thread.sleep(10);
         } catch (InterruptedException e) {
            throw new Error("Interrupted while waiting for initialization", e);
         }
      }
      return new Fortuna(new Generator(), pools);
   }

   private Fortuna(Generator generator, Pool[] pools) {
      this.generator = generator;
      this.pools = pools;
   }

   private byte[] randomData(int bytes) {
      lock.lock();
      try {
         long now = System.currentTimeMillis();
         if (pools[0].size() >= MIN_POOL_SIZE && now - lastReseedTime > 100) {
            lastReseedTime = now;
            reseedCount++;
            byte[] seed = new byte[pools.length * 32]; // Maximum potential length
            int seedLength = 0;
            for (int pool = 0; pool < pools.length; pool++) {
               if (reseedCount % POWERS_OF_TWO[pool] == 0) {
                  System.arraycopy(pools[pool].getAndClear(), 0, seed, seedLength, 32);
                  seedLength += 32;
               }
            }
            generator.reseed(Util.arrayCopyOf(seed, seedLength));
         }
         if (reseedCount == 0) {
            throw new IllegalStateException("Generator not reseeded yet");
         } else {
            return generator.pseudoRandomData(bytes);
         }
      } finally {
         lock.unlock();
      }
   }

   @Override
   protected int next(int bits) {
      byte[] bytes = randomData(Util.ceil(bits, 8));
      int result = 0;
      for (int i = 0; i < bytes.length; i++) {
         int shift = 8 * i;
         result |= (bytes[i] << shift) & (0xff << shift);
      }
      return result >>> (bytes.length * 8 - bits);
   }

   @Override
   public synchronized void setSeed(long seed) {
      // Does not do anything
   }

}
