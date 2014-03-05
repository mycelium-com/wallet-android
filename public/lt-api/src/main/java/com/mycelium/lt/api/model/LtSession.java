package com.mycelium.lt.api.model;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonProperty;

public class LtSession implements Serializable {
   private static final long serialVersionUID = 1L;

   public enum CaptchaCommands {
      CREATE_TRADER, CREATE_INSTANT_BUY_ORDER, CREATE_SELL_ORDER
   };

   @JsonProperty
   public final UUID id;

   @JsonProperty
   public final List<CaptchaCommands> captcha;

   public LtSession(@JsonProperty("id") UUID id, @JsonProperty("captcha") List<CaptchaCommands> captcha) {
      this.id = id;
      this.captcha = captcha;
   }

}
