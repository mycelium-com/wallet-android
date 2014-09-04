package com.mycelium.wapi.wallet;

import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
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
