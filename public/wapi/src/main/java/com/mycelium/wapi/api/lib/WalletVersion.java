package com.mycelium.wapi.api.lib;

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.mycelium.wapi.api.response.Feature;
import com.mycelium.wapi.api.response.FeatureWarning;
import com.mycelium.wapi.api.response.VersionInfoExResponse;
import com.mycelium.wapi.api.response.WarningKind;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class WalletVersion {
   final public int major;
   final public int minor;
   final public int maintenance;
   final public String variant;

   // latest Version pre minSDK-Update
   private static WalletVersion noUpdatePrior = WalletVersion.from("2.0.7");

   private static Set<WalletVersion> latestVersions = ImmutableSet.of(
         WalletVersion.from("2.4.4"),
         WalletVersion.from("2.4.4-TESTNET"),
         WalletVersion.from("1.3.3-BOG"));

   private static Map<String, Set<WalletVersion>> latestVersionsEx = ImmutableMap.of(
         "android", latestVersions
   );


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

      // only replay with an update info, if the client is already on the highe minSdk Level
      if (client.isGreaterThan(noUpdatePrior)) {
         for (WalletVersion latestVersion : latestVersions) {
            if (latestVersion.isGreaterThan(client)) {
               return latestVersion.toString();
            }
         }
      }

      return clientVersion;
   }

   public static VersionInfoExResponse responseVersionEx(String branch, String version){

      WalletVersion clientVersion = WalletVersion.from(version);
      if (clientVersion == null) {
         return null;
      }
      // todo: add different warnings depending on branch/version
      if (branch.equals("android") && version.startsWith("1.1.1")) {
         //List<FeatureWarning> warnings = new ArrayList<FeatureWarning>();
         //warnings.add(new FeatureWarning(Feature.MAIN_SCREEN, WarningKind.WARN, "Warning", URI.create("https://mycelium.com")));
         //return new VersionInfoExResponse("2.3.2", "", URI.create("https://mycelium.com/bitcoinwallet"), warnings);
      }

      // check if we have a newer version
      if (latestVersionsEx.containsKey(branch)){
         Set<WalletVersion> walletVersions = latestVersionsEx.get(branch);

         for (WalletVersion latestVersion : walletVersions) {
            if (latestVersion.isGreaterThan(clientVersion)) {
               return  new VersionInfoExResponse(latestVersion.toString(), "Update available", URI.create("https://mycelium.com/bitcoinwallet"), null);
            }
         }
      }

      // return null -> no important update or warnings available for this branch/version
      return null;
   }

   @Override
   public String toString() {
      String variant = !"".equals(this.variant) ? "-" + this.variant : "";
      return major + "." + minor + "." + maintenance + variant;
   }
}
