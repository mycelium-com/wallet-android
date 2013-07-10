package com.mycelium.wallet;


import android.os.Environment;
import android.test.AndroidTestCase;

import java.io.File;
import java.io.FileOutputStream;


// you should be able to run this tests using "gradle connectedInstrumentTest" or "gradle cIT"
// in the mbw folder

public class ExternalStorageManagerTest extends AndroidTestCase {

   public void testOverwriteDelete() throws Exception {
      File deleteMe = new File(Environment.getExternalStorageDirectory()+"/deleteme.dat");
      assertFalse(deleteMe.exists());
      boolean created = deleteMe.createNewFile();
      assertTrue(created);
      FileOutputStream fos = new FileOutputStream(deleteMe);
      fos.write("Test".getBytes());
      fos.close();
      assertTrue(deleteMe.exists());
      new ExternalStorageManager(getContext()).overwriteDelete(deleteMe);
      assertFalse(deleteMe.exists());

   }

}
