package com.mycelium.net;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.Inet4Address;
import java.net.InetAddress;

public class HiddenServiceInetAddress  {

  public static InetAddress getInstance(String host){
     // hacky: call the private constructor of Inet4Address to get an instance
     // with a hostname and a hardcoded IP without trying to resolve it via DNS
     Constructor<?> constructor = Inet4Address.class.getDeclaredConstructors()[0];
     byte[] ip = {0,0,0,0};
     try {
        constructor.setAccessible(true);
        return (InetAddress) constructor.newInstance(ip,host);
     } catch (InstantiationException e) {
        throw new RuntimeException(e);
     } catch (IllegalAccessException e) {
        throw new RuntimeException(e);
     } catch (InvocationTargetException e) {
        throw new RuntimeException(e);
     }
  }
}
