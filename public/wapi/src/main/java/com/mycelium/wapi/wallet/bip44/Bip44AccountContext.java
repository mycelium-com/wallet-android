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

package com.mycelium.wapi.wallet.bip44;

import com.mycelium.wapi.wallet.Bip44AccountBacking;

import java.util.UUID;

/**
 * The abstract context of an account
 */
public class Bip44AccountContext {
   private UUID id;
   private int accountIndex;
   private boolean isArchived;
   private int blockHeight;
   private int lastExternalIndexWithActivity;
   private int lastInternalIndexWithActivity;
   private int firstMonitoredInternalIndex;
   private long lastDiscovery;
   protected boolean isDirty;

   public Bip44AccountContext(Bip44AccountContext context) {
      this(context.getId(), context.getAccountIndex(),
            context.isArchived(), context.getBlockHeight(), context.getLastExternalIndexWithActivity(),
            context.getLastInternalIndexWithActivity(), context.getFirstMonitoredInternalIndex(),
            context.getLastDiscovery());
   }

   public Bip44AccountContext(UUID id, int accountIndex, boolean isArchived) {
      this(id, accountIndex, isArchived, 0, -1, -1, 0, 0);
   }

   public Bip44AccountContext(UUID id, int accountIndex, boolean isArchived, int blockHeight,
                              int lastExternalIndexWithActivity, int lastInternalIndexWithActivity,
                              int firstMonitoredInternalIndex, long lastDiscovery) {
      this.id = id;
      this.accountIndex = accountIndex;
      this.isArchived = isArchived;
      this.blockHeight = blockHeight;
      this.lastExternalIndexWithActivity = lastExternalIndexWithActivity;
      this.lastInternalIndexWithActivity = lastInternalIndexWithActivity;
      this.firstMonitoredInternalIndex = firstMonitoredInternalIndex;
      isDirty = false;
   }


   public UUID getId() {
      return id;
   }

   public int getAccountIndex() {
      return accountIndex;
   }

   /**
    * Is this account archived?
    */
   public boolean isArchived() {
      return isArchived;
   }

   /**
    * Mark this account as archived
    */
   public void setArchived(boolean isArchived) {
      if (this.isArchived != isArchived) {
         isDirty = true;
         this.isArchived = isArchived;
      }
   }

   /**
    * Get the block chain height recorded for this context
    */
   public int getBlockHeight() {
      return blockHeight;
   }

   /**
    * Set the block chain height for this context
    */
   public void setBlockHeight(int blockHeight) {
      if (this.blockHeight != blockHeight) {
         isDirty = true;
         this.blockHeight = blockHeight;
      }
   }

   public int getLastExternalIndexWithActivity() {
      return lastExternalIndexWithActivity;
   }

   public void setLastExternalIndexWithActivity(int lastExternalIndexWithActivity) {
      if (this.lastExternalIndexWithActivity != lastExternalIndexWithActivity) {
         isDirty = true;
         this.lastExternalIndexWithActivity = lastExternalIndexWithActivity;
      }
   }

   public int getLastInternalIndexWithActivity() {
      return lastInternalIndexWithActivity;
   }

   public void setLastInternalIndexWithActivity(int lastInternalIndexWithActivity) {
      if (this.lastInternalIndexWithActivity != lastInternalIndexWithActivity) {
         isDirty = true;
         this.lastInternalIndexWithActivity = lastInternalIndexWithActivity;
      }
   }

   public int getFirstMonitoredInternalIndex() {
      return firstMonitoredInternalIndex;
   }

   public void setFirstMonitoredInternalIndex(int firstMonitoredInternalIndex) {
      if (this.firstMonitoredInternalIndex != firstMonitoredInternalIndex) {
         isDirty = true;
         this.firstMonitoredInternalIndex = firstMonitoredInternalIndex;
      }
   }

   public long getLastDiscovery() {
      return lastDiscovery;
   }

   public void setLastDiscovery(long lastDiscovery) {
      if (this.lastDiscovery != lastDiscovery) {
         isDirty = true;
         this.lastDiscovery = lastDiscovery;
      }
   }

   /**
    * Persist this context if it is marked as dirty
    */
   public void persistIfNecessary(Bip44AccountBacking backing) {
      if (isDirty) {
         persist(backing);
      }
   }

   /**
    * Persist this context
    */
   public void persist(Bip44AccountBacking backing) {
      backing.updateAccountContext(this);
      isDirty = false;
   }

}
