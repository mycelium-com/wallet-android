package com.mycelium.lt.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Captcha implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public final byte[] png;

   @JsonProperty
   public final int length;

   public Captcha(@JsonProperty("png") byte[] png, @JsonProperty("length") int length) {
      this.png = png;
      this.length = length;
   }

}
