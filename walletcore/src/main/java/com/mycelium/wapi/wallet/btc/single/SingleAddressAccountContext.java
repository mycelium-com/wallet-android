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

package com.mycelium.wapi.wallet.btc.single;

import com.mrd.bitlib.model.Address;
import com.mycelium.wapi.wallet.SingleAddressBtcAccountBacking;

import java.util.UUID;

/**
 * The abstract context of an account
 */
public class SingleAddressAccountContext {
   private UUID id;
   private Address address;
   private boolean isArchived;
   private int blockHeight;
   protected boolean isDirty;

   public SingleAddressAccountContext(SingleAddressAccountContext context) {
      this(context.getId(), context.getAddress(), context.isArchived(), context.getBlockHeight());
   }

   public SingleAddressAccountContext(UUID id, Address address, boolean isArchived, int blockHeight) {
      this.id = id;
      this.address = address;
      this.isArchived = isArchived;
      this.blockHeight = blockHeight;
      isDirty = false;
   }


   public UUID getId() {
      return id;
   }

   public Address getAddress() {
      return address;
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

   /**
    * Persist this context if it is marked as dirty
    */
   public void persistIfNecessary(SingleAddressBtcAccountBacking backing) {
      if (isDirty) {
         persist(backing);
      }
   }

   /**
    * Persist this context
    */
   public void persist(SingleAddressBtcAccountBacking backing) {
      backing.updateAccountContext(this);
      isDirty = false;
   }

}
