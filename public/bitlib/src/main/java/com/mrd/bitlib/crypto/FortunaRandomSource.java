/*
 * Copyright 2013, 2014 Megion Research & Development GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mrd.bitlib.crypto;

import java.util.Arrays;
import java.util.List;

import se.grunka.fortuna.Fortuna;
import se.grunka.fortuna.accumulator.EntropySource;
import se.grunka.fortuna.entropy.FreeMemoryEntropySource;
import se.grunka.fortuna.entropy.GarbageCollectorEntropySource;
import se.grunka.fortuna.entropy.LoadAverageEntropySource;
import se.grunka.fortuna.entropy.PlatformEntropySource;
import se.grunka.fortuna.entropy.SchedulingEntropySource;
import se.grunka.fortuna.entropy.ThreadTimeEntropySource;
import se.grunka.fortuna.entropy.UptimeEntropySource;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class FortunaRandomSource implements RandomSource {

   private final Fortuna fortuna;

   public FortunaRandomSource() {
      fortuna = Fortuna.createInstance(Iterables.concat(jmxSources(),defaultSources(), platformSource()));
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

   private List<EntropySource> platformSource() {
         return ImmutableList.<EntropySource>of(new PlatformEntropySource());
   }


}
