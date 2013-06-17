/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 *  Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 *  This license governs use of the accompanying software. If you use the software, you accept this license.
 *  If you do not accept the license, do not use the software.
 *
 *  1. Definitions
 *  The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 *  "You" means the licensee of the software.
 *  "Your company" means the company you worked for when you downloaded the software.
 *  "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 *  of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 *  software, and specifically excludes the right to distribute the software outside of your company.
 *  "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 *  under this license.
 *
 *  2. Grant of Rights
 *  (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free copyright license to reproduce the software for reference use.
 *  (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 *  worldwide, royalty-free patent license under licensed patents for reference use.
 *
 *  3. Limitations
 *  (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 *  (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 *  (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 *  (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 *  guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 *  change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 *  fitness for a particular purpose and non-infringement.
 *
 */

package com.mrd.mbw;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.os.Environment;

import com.mrd.bitlib.model.Address;

public class ExternalStorageManager {

   @SuppressLint("SdCardPath")
   private final static String[] KNOWN_EXTERNAL_STORAGE_LOCATIONS = new String[] { "/mnt/_ExternalSD/",
         "/mnt/exsdcard/", "/mnt/extSdCard/", "/mnt/ext_card/", "/mnt/ext_sd/", "/mnt/extern_sd/", "/mnt/external1/",
         "/mnt/external_sd/", "/mnt/extsd/", "/mnt/sdcard-ext/", "/mnt/sdcard/extStorages/SdCard/",
         "/mnt/sdcard/external_sd/", "/mnt/sdcard/external_sd/", "/mnt/sdcard/external_sdcard/", "/mnt/emmc/",
         "/mnt/sdcard2/", "/mnt/sdcard/sd/", "/mnt/sd/", "/Removable/SD/OMN/", "/ext_card/", "/sdcard/external_sd/",
         "/sdcard2/", "/storage/extSdCard/", "/sdcard/sd/", "/storage/sdcard0/", "/sdcard/", "/mnt/sdcard/", };

   // Removed from the list of potentials:
   // "/storage/", "/mnt/sdcard/Android/data/"

   public ExternalStorageManager() {
   }

   public boolean hasExternalStorage() {
      return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
   }

   public List<String> listPotentialExternalSdCardPaths() {
      List<File> directories = listPotentialExternalSdCardDirectories();
      List<String> paths = new LinkedList<String>();
      for (File directory : directories) {
         paths.add(directory.getPath());
      }
      return paths;
   }

   private List<File> listPotentialExternalSdCardDirectories() {
      List<File> potentials = new LinkedList<File>();
      for (String path : KNOWN_EXTERNAL_STORAGE_LOCATIONS) {
         try {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
               String[] list = directory.list();
               if (list != null) {
                  potentials.add(directory.getAbsoluteFile());
               }
            }
         } catch (Exception e) {
            continue;
         }
      }

      // Add whatever the system claims is the external storage
      try {
         File ext = Environment.getExternalStorageDirectory();
         if (!potentials.contains(ext)) {
            potentials.add(ext);
         }
      } catch (Exception e) {
         // Ignore
      }

      return potentials;
   }

   public boolean hasExportedPrivateKeys() {
      List<File> directories = listPotentialExternalSdCardDirectories();
      for (File directory : directories) {
         if (hasExportedPrivateKeys(directory)) {
            return true;
         }
      }
      return false;
   }

   private boolean hasExportedPrivateKeys(File directory) {
      try {
         File exp = new File(directory, "mycelium");
         if (!exp.isDirectory()) {
            return false;
         }
         File[] files = exp.listFiles(); // may throw if not mounted
         if (files == null) {
            return false;
         }
         for (File file : files) {
            if (isExportedBitcoinPrivateKeyFile(file)) {
               return true;
            }
         }
      } catch (Exception e) {
         // Ignore
      }
      return false;
   }

   public boolean deleteExportedPrivateKeys() {
      List<File> directories = listPotentialExternalSdCardDirectories();
      boolean success = true;
      for (File directory : directories) {
         deleteExportedPrivateKeys(directory);
      }
      return success;
   }

   private void deleteExportedPrivateKeys(File directory) {
      try {
         File exp = new File(directory, "mycelium");
         if (!exp.isDirectory()) {
            return;
         }
         File[] files = exp.listFiles(); // may throw if not mounted
         if (files == null) {
            return;
         }
         for (File file : files) {
            if (isExportedBitcoinPrivateKeyFile(file)) {
               try {
                  file.delete();
               } catch (Exception e) {
                  // Ignore
               }
            }
         }
      } catch (Exception e) {
         // Ignore
      }
   }

   public String exportToExternalStorage(Record record, String path, boolean png) throws IOException {
      File dir = new File(path, "mycelium");
      if (!dir.isDirectory()) {
         if (!dir.mkdirs()) {
            throw new IOException("Unable to create external storage path: " + dir.getAbsolutePath().toString());
         }
      }
      Bitmap bitmap = getExportedBitmap(record);
      String baseName = record.address.toString();
      String fileName = png ? baseName + ".png" : baseName + ".jpg";
      File exportFile = new File(dir, fileName);
      FileOutputStream stream;
      stream = new FileOutputStream(exportFile);
      if (png) {
         bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
      } else {
         bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
      }
      stream.close();
      return exportFile.getAbsolutePath();
   }

   private boolean isExportedBitcoinPrivateKeyFile(File file) {
      String fileName = file.getName();
      // file name must end with .png or .jpg
      if (!fileName.endsWith(".png") && !fileName.endsWith(".jpg")) {
         return false;
      }
      String stripped = fileName.substring(0, fileName.length() - 4);
      // Must be long enough to contain a bitcoin address
      if (stripped.length() < 32) {
         return false;
      }
      // Stripped name must be a bitcoin address
      if (Address.fromString(stripped, Constants.network) == null) {
         return false;
      }
      return true;
   }

   
   /**
    * Constants used when generating exported bitmap
    */
   
   private static final int X_DIMENSION = 1500;
   private static final int Y_DIMENSION = 2000;
   private static final int NEW_QR_SIZE = 400;
   private static final int TITLE_X_POSITION = 0;
   private static final int TITLE_Y_POSITION = 0;
   private static final int QR_X_POSITION = 0;
   private static final int QR_Y_POSITION = 50;
   private static final int TEXT_X_POSITION = NEW_QR_SIZE + 100;
   private static final int TITLE_TEXT_SIZE = 80;
   private static final int NORMAL_TEXT_SIZE = 60;
   private static final int TEXT_Y_POSITION = QR_Y_POSITION + NEW_QR_SIZE / 2 - NORMAL_TEXT_SIZE * 2;

   private static Bitmap getExportedBitmap(Record record) {

      String address = record.address.toString();
      String[] formattedAddress = Utils.stringChopper(address, 12);
      Bitmap qrAddress = Utils.getQRCodeBitmap(address, NEW_QR_SIZE, 0);

      String base58 = record.key.getBase58EncodedPrivateKey(Constants.network);
      String[] formattedBase58 = Utils.stringChopper(base58, 12);
      Bitmap qrBase58 = Utils.getQRCodeBitmap(base58, NEW_QR_SIZE, 0);

      Bitmap export = Bitmap.createBitmap(X_DIMENSION, Y_DIMENSION, Config.ARGB_8888);
      Canvas canvas = new Canvas(export);
      Paint white = new Paint();
      white.setARGB(255, 255, 255, 255);
      canvas.drawPaint(white);
      drawSection(canvas, 200, 350, "Bitcoin Address", qrAddress, formattedAddress);
      drawSection(canvas, 200, 350 + 450 + 200, "Private Key", qrBase58, formattedBase58);
      return export;
   }

   private static void drawSection(Canvas canvas, int x, int y, String title, Bitmap qr, String[] text) {
      Paint titlePaint = new Paint();
      titlePaint.setTextSize(TITLE_TEXT_SIZE);
      Paint textPaint = new Paint();
      textPaint.setTextSize(NORMAL_TEXT_SIZE);
      textPaint.setTypeface(Typeface.MONOSPACE);
      Paint paint = new Paint();

      // Title
      canvas.drawText(title, x + TITLE_X_POSITION, y + TITLE_Y_POSITION, titlePaint);

      // QR
      canvas.drawBitmap(qr, x + QR_X_POSITION, y + QR_Y_POSITION, paint);

      // Text
      for (int i = 0; i < text.length; i++) {
         canvas.drawText(text[i], x + TEXT_X_POSITION, y + TEXT_Y_POSITION + NORMAL_TEXT_SIZE * i, textPaint);
      }
   }

}
