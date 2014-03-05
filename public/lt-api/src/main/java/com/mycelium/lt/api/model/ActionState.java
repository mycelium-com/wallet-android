package com.mycelium.lt.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents the state of an action for some object. For instance it could be
 * used to describe whether a trade session UI should display an abort button
 * session.abortAction.isApplicable(), should enable the button
 * session.abortAction.isEnabled(), or should disable the button
 * session.abortAction.isDisabled().
 */
public class ActionState implements Serializable {
   private static final long serialVersionUID = 1L;

   private static final int NA_STATE = 0;
   private static final int ENABLED_STATE = 1;
   private static final int DISABLED_STATE = 2;

   public static final ActionState ENABLED = new ActionState(ENABLED_STATE);
   public static final ActionState DISABLED = new ActionState(DISABLED_STATE);
   public static final ActionState NA = new ActionState(NA_STATE);

   @JsonProperty
   private final int state;

   private ActionState(@JsonProperty("state") int state) {
      this.state = state;
   }

   private ActionState() {
      state = NA_STATE;
   }

   @JsonIgnore
   public boolean isEnabled() {
      return state == ENABLED_STATE;
   }

   @JsonIgnore
   public boolean isDisabled() {
      return state == DISABLED_STATE;
   }

   @JsonIgnore
   public boolean isApplicable() {
      return state != NA_STATE;
   }

   @Override
   public int hashCode() {
      return state;
   }

   @Override
   public boolean equals(Object obj) {
      if (!(obj instanceof ActionState)) {
         return false;
      }
      return ((ActionState) obj).state == state;
   }

   @Override
   public String toString() {
      if (state == NA_STATE) {
         return "N/A";
      } else if (state == ENABLED_STATE) {
         return "enabled";
      } else if (state == DISABLED_STATE) {
         return "disabled";
      } else {
         return "unknown";
      }
   }

   public static final ActionState get(boolean enabled){
      return enabled ? ActionState.ENABLED : ActionState.DISABLED;
   }
}
