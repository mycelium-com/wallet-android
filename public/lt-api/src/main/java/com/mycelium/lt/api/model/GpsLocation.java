package com.mycelium.lt.api.model;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.mycelium.lt.api.LtConst;

public class GpsLocation implements Serializable {
   private static final long serialVersionUID = 1L;

   @JsonProperty
   public double latitude;
   @JsonProperty
   public double longitude;
   @JsonProperty
   public String name;

   public GpsLocation(@JsonProperty(LtConst.Param.LATITUDE) double latitude,
         @JsonProperty(LtConst.Param.LONGITUDE) double longitude, @JsonProperty(LtConst.Param.NAME) String name) {
      this.latitude = latitude;
      this.longitude = longitude;
      this.name = name;
   }

   public GpsLocation() {
   }
}
