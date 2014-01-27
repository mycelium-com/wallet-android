package com.mrd.mbwapi.api;

import java.io.Serializable;

public class ErrorMetaData implements Serializable{
   private static final long serialVersionUID = 1L;
   public static final ErrorMetaData DUMMY = new ErrorMetaData(0, 0, "junit", "junit", "junit", "junit");
   private final int totalMemory;
   private final int sdk_level;
   private final String model;
   private final String vendor;
   private final String version;
   private final String device;

   public ErrorMetaData(int totalMemory, int sdk_level, String model, String vendor, String version, String device) {
      this.totalMemory = totalMemory;
      this.sdk_level = sdk_level;
      this.model = model;
      this.vendor = vendor;
      this.version = version;
      this.device = device;
   }

   @Override
   public String toString() {
      return "ErrorMetaData{" +
            "totalMemory=" + totalMemory +
            ", sdk_level=" + sdk_level +
            ", model='" + model + '\'' +
            ", vendor='" + vendor + '\'' +
            ", version='" + version + '\'' +
            ", device='" + device + '\'' +
            '}';
   }
}
