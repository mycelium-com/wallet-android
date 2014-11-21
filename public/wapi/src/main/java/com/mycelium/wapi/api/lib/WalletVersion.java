package com.mycelium.wapi.api.lib;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import java.util.Set;

public class WalletVersion {
   final public int major;
   final public int minor;
   final public int maintenance;
   final public String variant;

   private static Set<WalletVersion> latestVersions = ImmutableSet.of(WalletVersion.from("2.0.5"),
         WalletVersion.from("2.0.5-TESTNET"), WalletVersion.from("1.3.3-BOG"));

   public WalletVersion(int major, int minor, int maintenance, String variant) {
      this.major = major;
      this.minor = minor;
      this.maintenance = maintenance;
      this.variant = variant;
   }

   public static WalletVersion from(String version) {
      if (version == null)
         return null;
      CharMatcher number = CharMatcher.anyOf("1234567890.");
      int index = number.negate().indexIn(version);
      final String versionNumber;
      if (index == -1) {
         versionNumber = version;
      } else {
         versionNumber = version.substring(0, index);
      }
      String variantWithDash = version.substring(versionNumber.length());
      final String versionVariant = CharMatcher.anyOf("-").trimLeadingFrom(variantWithDash);
      ImmutableList<String> versions = ImmutableList.copyOf(Splitter.on(".").split(versionNumber));
      if (versions.size() != 3)
         return null;
      return new WalletVersion(Integer.valueOf(versions.get(0)), Integer.valueOf(versions.get(1)),
            Integer.valueOf(versions.get(2)), versionVariant);
   }

   /**
    * 
    * @param client
    *           to compare to.
    * @return true if this object is strictly greater than the client argument.
    *         if the variant is different returns false.
    */
   public boolean isGreaterThan(WalletVersion client) {
      if (!variant.equals(client.variant)) {
         return false;
      }
      if (major == client.major) {
         if (minor == client.minor) {
            return maintenance > client.maintenance;
         }
         return minor > client.minor;
      }
      return major > client.major;
   }

   public static String responseVersion(String clientVersion) {
      WalletVersion client = from(clientVersion);
      for (WalletVersion latestVersion : latestVersions) {
         if (latestVersion.isGreaterThan(client)) {
            return latestVersion.toString();
         }
      }
      return clientVersion;
   }

   @Override
   public String toString() {
      String variant = !"".equals(this.variant) ? "-" + this.variant : "";
      return major + "." + minor + "." + maintenance + variant;
   }
}
