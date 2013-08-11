package com.mycelium.wallet;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import com.mrd.bitlib.crypto.RandomSource;

public class AndroidRandomSource extends RandomSource {

   @Override
   public synchronized void nextBytes(byte[] bytes) {
      // On Android we use /dev/urandom for providing random data
      File file = new File("/dev/urandom");
      if (!file.exists()) {
         throw new RuntimeException("Unable to generate random bytes on this Android device");
      }
      try {
         FileInputStream stream = new FileInputStream(file);
         DataInputStream dis = new DataInputStream(stream);
         dis.readFully(bytes);
         dis.close();
      } catch (IOException e) {
         throw new RuntimeException("Unable to generate random bytes on this Android device", e);
      }
   }

}
