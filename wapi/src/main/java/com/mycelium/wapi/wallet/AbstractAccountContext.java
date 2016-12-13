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

package com.mycelium.wapi.wallet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The abstract context of an account
 */
public abstract class AbstractAccountContext {
   @JsonProperty
   private boolean isArchived;
   @JsonProperty
   private int blockHeight;
   @JsonIgnore
   protected boolean isDirty;

   protected static final ObjectMapper OBJECT_MAPPER;
   static {
      OBJECT_MAPPER = new ObjectMapper();
      // We ignore properties that do not map onto the version of the class
      // we deserialize
      OBJECT_MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
   }

   @JsonCreator
   protected AbstractAccountContext(@JsonProperty("isArchived") boolean isArchived,
         @JsonProperty("blockHeight") int blockHeight) {
      this.isArchived = isArchived;
      this.blockHeight = blockHeight;
      isDirty = false;
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
    * 
    * @return
    */
   public int getBlockHeight() {
      return blockHeight;
   }

   /**
    * Set the block chain height for this context
    * 
    * @param blockHeight
    */
   public void setBlockHeight(int blockHeight) {
      if (this.blockHeight != blockHeight) {
         isDirty = true;
         this.blockHeight = blockHeight;
      }
   }

}
