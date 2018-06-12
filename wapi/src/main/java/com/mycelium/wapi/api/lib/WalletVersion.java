package com.mycelium.wapi.api.lib;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.mycelium.wapi.api.response.VersionInfoExResponse;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.*;

public class WalletVersion {
   public int major;
   public int minor;
   public int patch;
   public int maintenance;
   public String variant;

   private static Map<String, Set<WalletVersion>> latestVersionsEx;
   private static final File walletVersionsSource = new File("config/wallet_versions.json");
   private static long lastModified = walletVersionsSource.lastModified();
   static {
      readVersions();
   }

   private static void readVersions() {
      ObjectMapper mapper = new ObjectMapper();
      try {
         latestVersionsEx = mapper.readValue(walletVersionsSource, new TypeReference<Map<String, HashSet<WalletVersion>>>(){});
      } catch (IOException ignored) {
      }
   }


   private WalletVersion() {

   }

   public WalletVersion(int major, int minor, int patch, int maintenance, String variant) {
      this.major = major;
      this.minor = minor;
      this.patch = patch;
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
      if (versions.size() != 4)
         return null;
      return new WalletVersion(Integer.valueOf(versions.get(0)), Integer.valueOf(versions.get(1)),
            Integer.valueOf(versions.get(2)), Integer.valueOf(versions.get(3)), versionVariant);
   }

   /**
    *
    * @param client
    *           to compare to.
    * @return true if this object is strictly greater than the client argument.
    *         if the variant is different returns false.
    */
   public boolean isGreaterThan(WalletVersion client) {
      if (!Objects.equals(variant,client.variant)) {
         return false;
      }
      if (major == client.major) {
         if (minor == client.minor) {
            if (patch == client.patch) {
               return maintenance > client.maintenance;
            }
            return patch > client.patch;
         }
         return minor > client.minor;
      }
      return major > client.major;
   }

   public static VersionInfoExResponse responseVersionEx(String branch, String version){
      if (lastModified < walletVersionsSource.lastModified()) {
         lastModified = walletVersionsSource.lastModified();
         readVersions();
      }
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
               return new VersionInfoExResponse(latestVersion.toString(), "Update available", URI.create("https://wallet.mycelium.com/contact.html"), null);
            }
         }
      }

      // return null -> no important update or warnings available for this branch/version
      return null;
   }

   @Override
   public String toString() {
      String variant = !"".equals(this.variant) ? "-" + this.variant : "";
      return major + "." + minor + "." + patch + "." + maintenance + variant;
   }
}
