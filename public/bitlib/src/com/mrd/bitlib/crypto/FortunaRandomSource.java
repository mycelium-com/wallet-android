package com.mrd.bitlib.crypto;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import se.grunka.fortuna.Fortuna;
import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.entropy.*;

public class FortunaRandomSource extends RandomSource {

   private final Fortuna fortuna;

   public FortunaRandomSource() {
      fortuna = Fortuna.createInstance(Iterables.concat(jmxSources(),defaultSources(), linuxSource()));
   }

   @Override
   public void nextBytes(byte[] bytes) {
      fortuna.nextBytes(bytes);
   }

   private List<EntropySource> defaultSources() {
      return ImmutableList.of(new SchedulingEntropySource(), new FreeMemoryEntropySource());
   }

   private List<EntropySource> jmxSources() {
      try {
         Class.forName("java.lang.management.PlatformManagedObject");
      } catch (ClassNotFoundException e) {
         return ImmutableList.of();
      }
      return Arrays.asList(
               new GarbageCollectorEntropySource()
              , new LoadAverageEntropySource()
              , new ThreadTimeEntropySource()
              , new UptimeEntropySource());
   }

   private List<EntropySource> linuxSource() {
      if (new File("/dev/urandom").exists()) {
         return ImmutableList.<EntropySource>of(new URandomEntropySource());
      } else return ImmutableList.of();
   }


}
