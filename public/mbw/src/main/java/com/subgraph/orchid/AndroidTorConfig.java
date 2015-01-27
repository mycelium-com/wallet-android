package com.subgraph.orchid;

import android.content.Context;
import com.subgraph.orchid.circuits.hs.HSDescriptorCookie;
import com.subgraph.orchid.config.TorConfigBridgeLine;
import com.subgraph.orchid.data.HexDigest;
import com.subgraph.orchid.data.IPv4Address;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AndroidTorConfig implements TorConfig {

   final TorConfig delegate;
   private final Context context;

   public AndroidTorConfig(TorConfig delegate, Context context) {
      this.delegate = delegate;
      this.context = context;
   }

   @Override
   public File getDataDirectory() {
      File appPath = new File(context.getApplicationInfo().dataDir, "tor");
      if (!appPath.exists()) {
         appPath.mkdir();
      }
      return appPath;
   }

   @Override
   public void setDataDirectory(File directory) {
      throw new UnsupportedOperationException("not implemented yet");
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "60 seconds")
   public long getCircuitBuildTimeout() {
      return delegate.getCircuitBuildTimeout();
   }

   @Override
   public void setCircuitBuildTimeout(long time, TimeUnit unit) {
      delegate.setCircuitBuildTimeout(time, unit);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "0")
   public long getCircuitStreamTimeout() {
      return delegate.getCircuitStreamTimeout();
   }

   @Override
   public void setCircuitStreamTimeout(long time, TimeUnit unit) {
      delegate.setCircuitStreamTimeout(time, unit);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "1 hour")
   public long getCircuitIdleTimeout() {
      return delegate.getCircuitIdleTimeout();
   }

   @Override
   public void setCircuitIdleTimeout(long time, TimeUnit unit) {
      delegate.setCircuitIdleTimeout(time, unit);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "30 seconds")
   public long getNewCircuitPeriod() {
      return delegate.getNewCircuitPeriod();
   }

   @Override
   public void setNewCircuitPeriod(long time, TimeUnit unit) {
      delegate.setNewCircuitPeriod(time, unit);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "10 minutes")
   public long getMaxCircuitDirtiness() {
      return delegate.getMaxCircuitDirtiness();
   }

   @Override
   public void setMaxCircuitDirtiness(long time, TimeUnit unit) {
      delegate.setMaxCircuitDirtiness(time, unit);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTEGER, defaultValue = "32")
   public int getMaxClientCircuitsPending() {
      return delegate.getMaxClientCircuitsPending();
   }

   @Override
   public void setMaxClientCircuitsPending(int value) {
      delegate.setMaxClientCircuitsPending(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getEnforceDistinctSubnets() {
      return delegate.getEnforceDistinctSubnets();
   }

   @Override
   public void setEnforceDistinctSubnets(boolean value) {
      delegate.setEnforceDistinctSubnets(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTERVAL, defaultValue = "2 minutes")
   public long getSocksTimeout() {
      return delegate.getSocksTimeout();
   }

   @Override
   public void setSocksTimeout(long value) {
      delegate.setSocksTimeout(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.INTEGER, defaultValue = "3")
   public int getNumEntryGuards() {
      return delegate.getNumEntryGuards();
   }

   @Override
   public void setNumEntryGuards(int value) {
      delegate.setNumEntryGuards(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getUseEntryGuards() {
      return delegate.getUseEntryGuards();
   }

   @Override
   public void setUseEntryGuards(boolean value) {
      delegate.setUseEntryGuards(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.PORTLIST, defaultValue = "21,22,706,1863,5050,5190,5222,5223,6523,6667,6697,8300")
   public List<Integer> getLongLivedPorts() {
      return delegate.getLongLivedPorts();
   }

   @Override
   public void setLongLivedPorts(List<Integer> ports) {
      delegate.setLongLivedPorts(ports);
   }

   @Override
   @ConfigVar(type = ConfigVarType.STRINGLIST)
   public List<String> getExcludeNodes() {
      return delegate.getExcludeNodes();
   }

   @Override
   public void setExcludeNodes(List<String> nodes) {
      delegate.setExcludeNodes(nodes);
   }

   @Override
   @ConfigVar(type = ConfigVarType.STRINGLIST)
   public List<String> getExcludeExitNodes() {
      return delegate.getExcludeExitNodes();
   }

   @Override
   public void setExcludeExitNodes(List<String> nodes) {
      delegate.setExcludeExitNodes(nodes);
   }

   @Override
   @ConfigVar(type = ConfigVarType.STRINGLIST)
   public List<String> getExitNodes() {
      return delegate.getExitNodes();
   }

   @Override
   public void setExitNodes(List<String> nodes) {
      delegate.setExitNodes(nodes);
   }

   @Override
   @ConfigVar(type = ConfigVarType.STRINGLIST)
   public List<String> getEntryNodes() {
      return delegate.getEntryNodes();
   }

   @Override
   public void setEntryNodes(List<String> nodes) {
      delegate.setEntryNodes(nodes);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "false")
   public boolean getStrictNodes() {
      return delegate.getStrictNodes();
   }

   @Override
   public void setStrictNodes(boolean value) {
      delegate.setStrictNodes(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "false")
   public boolean getFascistFirewall() {
      return delegate.getFascistFirewall();
   }

   @Override
   public void setFascistFirewall(boolean value) {
      delegate.setFascistFirewall(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.PORTLIST, defaultValue = "80,443")
   public List<Integer> getFirewallPorts() {
      return delegate.getFirewallPorts();
   }

   @Override
   public void setFirewallPorts(List<Integer> ports) {
      delegate.setFirewallPorts(ports);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "false")
   public boolean getSafeSocks() {
      return delegate.getSafeSocks();
   }

   @Override
   public void setSafeSocks(boolean value) {
      delegate.setSafeSocks(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getSafeLogging() {
      return delegate.getSafeLogging();
   }

   @Override
   public void setSafeLogging(boolean value) {
      delegate.setSafeLogging(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getWarnUnsafeSocks() {
      return delegate.getWarnUnsafeSocks();
   }

   @Override
   public void setWarnUnsafeSocks(boolean value) {
      delegate.setWarnUnsafeSocks(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getClientRejectInternalAddress() {
      return delegate.getClientRejectInternalAddress();
   }

   @Override
   public void setClientRejectInternalAddress(boolean value) {
      delegate.setClientRejectInternalAddress(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getHandshakeV3Enabled() {
      return delegate.getHandshakeV3Enabled();
   }

   @Override
   public void setHandshakeV3Enabled(boolean value) {
      delegate.setHandshakeV3Enabled(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "true")
   public boolean getHandshakeV2Enabled() {
      return delegate.getHandshakeV2Enabled();
   }

   @Override
   public void setHandshakeV2Enabled(boolean value) {
      delegate.setHandshakeV2Enabled(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.HS_AUTH)
   public HSDescriptorCookie getHidServAuth(String key) {
      return delegate.getHidServAuth(key);
   }

   @Override
   public void addHidServAuth(String key, String value) {
      delegate.addHidServAuth(key, value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.AUTOBOOL, defaultValue = "auto")
   public AutoBoolValue getUseNTorHandshake() {
      return delegate.getUseNTorHandshake();
   }

   @Override
   public void setUseNTorHandshake(AutoBoolValue value) {
      delegate.setUseNTorHandshake(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.AUTOBOOL, defaultValue = "auto")
   public AutoBoolValue getUseMicrodescriptors() {
      return delegate.getUseMicrodescriptors();
   }

   @Override
   public void setUseMicrodescriptors(AutoBoolValue value) {
      delegate.setUseMicrodescriptors(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BOOLEAN, defaultValue = "false")
   public boolean getUseBridges() {
      return delegate.getUseBridges();
   }

   @Override
   public void setUseBridges(boolean value) {
      delegate.setUseBridges(value);
   }

   @Override
   @ConfigVar(type = ConfigVarType.BRIDGE_LINE)
   public List<TorConfigBridgeLine> getBridges() {
      return delegate.getBridges();
   }

   @Override
   public void addBridge(IPv4Address address, int port) {
      delegate.addBridge(address, port);
   }

   @Override
   public void addBridge(IPv4Address address, int port, HexDigest fingerprint) {
      delegate.addBridge(address, port, fingerprint);
   }
}
