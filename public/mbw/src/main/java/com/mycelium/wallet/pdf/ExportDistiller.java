/*
 * Copyright 2013 Megion Research and Development GmbH
 *
 * Licensed under the Microsoft Reference Source License (MS-RSL)
 *
 * This license governs use of the accompanying software. If you use the software, you accept this license.
 * If you do not accept the license, do not use the software.
 *
 * 1. Definitions
 * The terms "reproduce," "reproduction," and "distribution" have the same meaning here as under U.S. copyright law.
 * "You" means the licensee of the software.
 * "Your company" means the company you worked for when you downloaded the software.
 * "Reference use" means use of the software within your company as a reference, in read only form, for the sole purposes
 * of debugging your products, maintaining your products, or enhancing the interoperability of your products with the
 * software, and specifically excludes the right to distribute the software outside of your company.
 * "Licensed patents" means any Licensor patent claims which read directly on the software as distributed by the Licensor
 * under this license.
 *
 * 2. Grant of Rights
 * (A) Copyright Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free copyright license to reproduce the software for reference use.
 * (B) Patent Grant- Subject to the terms of this license, the Licensor grants you a non-transferable, non-exclusive,
 * worldwide, royalty-free patent license under licensed patents for reference use.
 *
 * 3. Limitations
 * (A) No Trademark License- This license does not grant you any rights to use the Licensor’s name, logo, or trademarks.
 * (B) If you begin patent litigation against the Licensor over patents that you think may apply to the software
 * (including a cross-claim or counterclaim in a lawsuit), your license to the software ends automatically.
 * (C) The software is licensed "as-is." You bear the risk of using it. The Licensor gives no express warranties,
 * guarantees or conditions. You may have additional consumer rights under your local laws which this license cannot
 * change. To the extent permitted under your local laws, the Licensor excludes the implied warranties of merchantability,
 * fitness for a particular purpose and non-infringement.
 */

package com.mycelium.wallet.pdf;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Log;

import com.google.common.base.CharMatcher;
import com.google.common.base.Preconditions;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mycelium.wallet.R;

import crl.android.pdfwriter.PDFWriter;
import crl.android.pdfwriter.PaperSize;
import crl.android.pdfwriter.StandardFonts;

public class ExportDistiller {

   private static final int LABEL_FONT_SIZE = 15;
   private static final int RECORDS_PR_PAGE = 3;

   public static class ExportEntry implements Serializable {
      private static final long serialVersionUID = 1L;

      public String address;
      public String encryptedKey;
      public String label;

      public ExportEntry(String address, String encryptedKey, String label) {
         this.address = address;
         this.encryptedKey = encryptedKey;
         this.label = label;
      }

   }

   public static class ExportProgressTracker implements Serializable {
      private static final long serialVersionUID = 1L;
      private static final int WATER_MARK_WORK = 5;
      private static final int ADDRESS_WORK = 2;
      private static final int PRIVATE_KEY_WORK = 3;
      private int _totalWork;
      private int _workCompleted;

      public ExportProgressTracker(Iterable<ExportEntry> entries) {
         // Sum up all the work to do
         _totalWork = WATER_MARK_WORK;
         for (ExportEntry entry : entries) {
            _totalWork += ADDRESS_WORK;
            if (entry.encryptedKey != null) {
               _totalWork += PRIVATE_KEY_WORK;
            }
         }
      }

      private void watermarkCompleted() {
         _workCompleted = Math.min(_totalWork, _workCompleted + WATER_MARK_WORK);
      }

      private void addressCompleted() {
         _workCompleted = Math.min(_totalWork, _workCompleted + ADDRESS_WORK);
      }

      private void privateKeyCompleted() {
         _workCompleted = Math.min(_totalWork, _workCompleted + PRIVATE_KEY_WORK);
      }

      public double getProgress() {
         return (double) _workCompleted / (double) _totalWork;
      }

   }

   public static String exportPrivateKeysToFile(Context context, ExportPdfParameters params,
         ExportProgressTracker progressTracker, String filePath) throws IOException {
      // Write document to file
      String result = exportPrivateKeys(context, params, progressTracker);
      try {

         FileOutputStream stream;
         stream = getOutStream(context, filePath);
         stream.write(result.getBytes("UTF-8"));
         stream.close();
      } catch (IOException e) {
         Log.e("ExportDistiller", "IOException while writing file", e);
         throw e;
      }
      return result;
   }

   private static FileOutputStream getOutStream(Context context, String filePath) throws FileNotFoundException {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
         return context.openFileOutput(filePath, Context.MODE_PRIVATE);
      } else {
         return new FileOutputStream(filePath);
      }
   }

   public static String exportPrivateKeys(Context context, ExportPdfParameters params,
         ExportProgressTracker progressTracker) {

      int pageWidth = PaperSize.EXECUTIVE_WIDTH;
      int pageHeight = PaperSize.EXECUTIVE_HEIGHT;

      final String versionName;
      final String appName;
      try {
         PackageManager packageManager = Preconditions.checkNotNull(context.getPackageManager());
         versionName = packageManager.getPackageInfo(context.getPackageName(), 0).versionName;
         ApplicationInfo applicationInfo = Preconditions.checkNotNull(context.getApplicationInfo());
         appName = context.getString(applicationInfo.labelRes);
      } catch (NameNotFoundException e) {
         // Never happens
         throw new RuntimeException(e);
      }

      // Count keys, active, archive
      final int totalKeys = params.entriesWithEncryptedKeys();
      int activeRecords = params.getNumActive();
      int archivedRecords = params.getNumArchived();
      int totalRecords = activeRecords + archivedRecords;

      int totalPages = 1 + ((totalRecords - 1 + RECORDS_PR_PAGE - 1) / RECORDS_PR_PAGE) + 1;

      MyWriter writer = new MyWriter(pageWidth, pageHeight, 20, 20, 20, 20);

      // Watermark

      try {
         Bitmap watermark;
         watermark = BitmapFactory.decodeResource(context.getResources(), R.drawable.mycelium_splash_notext_corner);
         writer.addImage(0, 0, 126, 83, watermark);
      } catch (Throwable e) {
         // We have observed some devices having problems loading the watermark,
         // if it happens we try to continue without adding it
         // todo insert uncaught error handler
      }

      progressTracker.watermarkCompleted();

      // Title
      writer.setTextColor(0, 0.5, 1);
      writer.addText(7F, 0.5F, 30, "Mycelium Wallet Backup");

      writer.setTextColor(0, 0, 0);

      double fromTop = 3.3F;

      // Creation Date, use US locale to make the date appear readable
      // regardless of what locale is configured
      writer.addText(1F, fromTop, 10, "Creation Date:");
      Locale usLocale = new Locale("en_US");
      writer.addText(4.2F, fromTop, 10,
            DateFormat.getDateInstance(DateFormat.LONG, usLocale).format(new Date(params.time)));
      // writer.addText(4.2F, fromTop, 10, new Date(time).toLocaleString());
      fromTop += 0.4F;

      // Made With
      writer.addText(1F, fromTop, 10, "Made With:");
      writer.addText(4.2F, fromTop, 10, appName + " " + versionName);
      fromTop += 0.4F;

      // Export Format
      writer.addText(1F, fromTop, 10, "Backup Format:");
      writer.addText(4.2F, fromTop, 10, params.exportFormatString);
      fromTop += 0.4F;

      // Active Records
      writer.addText(1F, fromTop, 10, "Active Records:");
      writer.addText(4.2F, fromTop, 10, "" + activeRecords);
      fromTop += 0.4F;

      // Archived Records
      writer.addText(1F, fromTop, 10, "Archived Records:");
      writer.addText(4.2F, fromTop, 10, "" + archivedRecords);
      fromTop += 0.4F;

      // Total Keys
      writer.addText(1F, fromTop, 10, "Total Keys:");
      writer.addText(4.2F, fromTop, 10, "" + totalKeys);
      fromTop += 0.4F;

      // Total Addresses
      writer.addText(1F, fromTop, 10, "Total Addresses:");
      writer.addText(4.2F, fromTop, 10, "" + totalRecords);
      fromTop += 0.8F;

      // Description 1
      writer.addText(1F, fromTop, 12, "This document contains encrypted Bitcoin private keys. To access the bitcoins");
      fromTop += 0.5F;
      writer.addText(1F, fromTop, 12,
            "controlled by these private keys you need to import them into the Mycelium Bitcoin");
      fromTop += 0.5F;
      writer.addText(1F, fromTop, 12,
            "Wallet, or some other wallet that recognizes this format. The private keys contained in");
      fromTop += 0.5F;
      writer.addText(1F, fromTop, 12, "this document are password protected. The password was shown on display while");
      fromTop += 0.5F;
      writer.addText(1F, fromTop, 12,
            "creating the backup. It is not possible to access the bitcoins without the password.");
      fromTop += 1F;

      // Description 2
      writer.addText(1F, fromTop, 12,
            "Write the 15-character password and the checksum character from the display here:");
      fromTop += 0.8F;

      writer.setLineColor(0, 0.5, 1);
      addPasswordBoxes(new OffsetWriter(1F, fromTop, writer));
      fromTop += 0.8F;

      // Description 3
      writer.addText(1F, fromTop, 12, "Alternatively you can write it down elsewere.");
      fromTop += 1F;

      // Description 4
      writer.addText(1F, fromTop, 12,
            "To import a key in the Mycelium wallet you need to scan the private key QR code and");
      fromTop += 0.45;
      writer.addText(1F, fromTop, 12, "enter the encryption password.");
      fromTop += 1F;

      // Description 5
      writer.addText(1F, fromTop, 12,
            "Note that the embedded PDF viewer in Windows 8 cannot display the QR codes properly.");
      fromTop += 1.7F;

      // Add first key to first page (we know that there will always be one
      // acrive record)
      fromTop += addRecord(new OffsetWriter(0F, fromTop, writer), true, 1, activeRecords, params.firstEntry(), true,
            progressTracker);

      // Add page number
      int pageNum = 1;
      addPageNumber(writer, pageNum++, totalPages);
      if (totalRecords > 1) {
         writer.addPage();
      }

      // There are 3 records positions per page
      int recordsOnThisPage = 0;
      fromTop = 0F;

      // Add remaining active records
      List<ExportEntry> activeWithoutFirst = params.activeWithoutFirst();
        for (int i = 0; i < activeWithoutFirst.size(); i++) {
         ExportEntry exportEntry = activeWithoutFirst.get(i);
         recordsOnThisPage++;
         boolean moreRecords = i != totalRecords - 1;
         boolean lastRecordOnPage = recordsOnThisPage == RECORDS_PR_PAGE || !moreRecords;

         // Add Record
         fromTop += addRecord(new OffsetWriter(0F, fromTop, writer), true, i + 1 + 1, params.getNumActive(),
               exportEntry, lastRecordOnPage, progressTracker);

         // Add page number
         if (lastRecordOnPage) {
            recordsOnThisPage = 0;
            addPageNumber(writer, pageNum++, totalPages);
            if (moreRecords) {
               writer.addPage();
               fromTop = 0F;
            }
         }
      }

      // Add archived records
      List<ExportEntry> archiveWithoutFirst = params.archiveWithoutFirst();
      for (int i = 0; i < archiveWithoutFirst.size(); i++) {
         recordsOnThisPage++;
         boolean moreRecords = i != archiveWithoutFirst.size() - 1;
         boolean lastRecordOnPage = recordsOnThisPage == RECORDS_PR_PAGE || !moreRecords;

         // Add Record
         fromTop += addRecord(new OffsetWriter(0F, fromTop, writer), false, i + 1, archiveWithoutFirst.size(),
               archiveWithoutFirst.get(i), lastRecordOnPage, progressTracker);

         // Add page number
         if (lastRecordOnPage) {
            recordsOnThisPage = 0;
            addPageNumber(writer, pageNum++, totalPages);
            if (moreRecords) {
               writer.addPage();
               fromTop = 0F;
            }
         }

      }

      addFinalPage(writer, totalPages);
      return writer.asString();
   }

   private static void addPageNumber(MyWriter writer, int i, int totalPages) {
      writer.addText(16F, 26.4F, 12, "Page " + i + " of " + totalPages);
   }

   private static double addRecord(OffsetWriter writer, boolean active, int entryNum, int totalEntries,
         ExportEntry entry, boolean addEndLine, ExportProgressTracker progressTracker) {
      String address = entry.address;
      String encryptedKey = entry.encryptedKey;
      double fromTop = 0;
      // Add separator line and key title
      writer.setLineColor(0, 0.5, 1);
      writer.addLine(1F, fromTop, 6.5F, fromTop);
      writer.setTextColor(0, 0.5, 1);
      String title = (active ? "Active " : "Archived ") + entryNum + " of " + totalEntries;
      writer.addText(8F, fromTop - 0.4F, 13, title);
      writer.addLine(12.0F, fromTop, 18F, fromTop);
      fromTop += 0.5;

      // Label
      if (entry.label != null && entry.label.length() > 0 && isASCII(entry.label)) {
         // Use Monospace font, this allows us to calculate the width of the
         // label
         writer.setMonoFont();
         int width = (int) (entry.label.length() * LABEL_FONT_SIZE * 0.6F);
         // Center the label according to its width
         int xPos = writer.getWidth() / 2 - width / 2;
         writer.addText(xPos, writer.translateCmY(fromTop - 0.2), LABEL_FONT_SIZE, entry.label);
         fromTop += 0.7F;
         // Use Standard font
         writer.setStandardFont();

      }

      boolean hasEpk = entry.encryptedKey != null;

      // Titles
      writer.setTextColor(0, 0, 0);
      writer.addText(3F, fromTop, 13, "Bitcoin Address");
      if (hasEpk) {
         writer.addText(12F, fromTop, 13, "Encrypted Private Key");
      }
      fromTop += 1.5F;

      // QR codes
      // Bitmap addressQr = Utils.getQRCodeBitmap("bitcoin:" + address, 200, 0);
      // writer.addImage(2.9, fromTop, 3.5, 3.5, addressQr);

      writer.addQrCode(2.9, fromTop - 0.25, 3.5, "bitcoin:" + address);

      progressTracker.addressCompleted();
      // Encrypted private key QR-code
      if (hasEpk) {
         // Bitmap keyQr = Utils.getQRCodeBitmap(encryptedKey, 200, 0);
         // writer.addImage(12.5, fromTop, 3.5, 3.5, keyQr);

         writer.addQrCode(12.5, fromTop - 0.5, 4, encryptedKey);

         progressTracker.privateKeyCompleted();
      }
      fromTop += 4;

      // Use Monospace font
      writer.setMonoFont();

      // Strings
      String a1 = address.substring(0, address.length() / 2);
      String a2 = address.substring(address.length() / 2);
      String k1 = "";
      String k2 = "";
      if (hasEpk) {
         k1 = encryptedKey.substring(0, encryptedKey.length() / 2);
         k2 = encryptedKey.substring(encryptedKey.length() / 2);
      }
      writer.addText(2.3, fromTop, 12, a1);
      if (hasEpk) {
         writer.addText(9.8, fromTop, 12, k1);
      }
      fromTop += 0.5F;
      writer.addText(2.3, fromTop, 12, a2);
      if (hasEpk) {
         writer.addText(9.8, fromTop, 12, k2);
      }
      fromTop += 1;

      // Use Standard font
      writer.setStandardFont();

      // Add end line if necessary
      if (addEndLine) {
         writer.setLineColor(0, 0.5, 1);
         writer.addLine(1F, fromTop, 18F, fromTop);
      }
      fromTop += 0.5F;
      return fromTop;
   }
   
   private static boolean isASCII(String string){
      return CharMatcher.ASCII.matchesAllOf(string);
   }
   
   private static void addPasswordBoxes(OffsetWriter offsetWriter) {
      offsetWriter.setLineColor(0, 0.5, 1);
      add3PasswordBoxes(new OffsetWriter(0F, 0F, offsetWriter));
      add3PasswordBoxes(new OffsetWriter(3.2F, 0F, offsetWriter));
      add3PasswordBoxes(new OffsetWriter(6.4F, 0F, offsetWriter));
      add3PasswordBoxes(new OffsetWriter(9.6F, 0F, offsetWriter));
      add3PasswordBoxes(new OffsetWriter(12.8F, 0F, offsetWriter));
      // Add checksum box
      offsetWriter.setLineColor(0, 0.8, 0);
      offsetWriter.addRectangle(16F, 0F, 0.7F, 0.7F);

   }

   private static void add3PasswordBoxes(OffsetWriter writer) {
      writer.addRectangle(0F, 0F, 0.7F, 0.7F);
      writer.addRectangle(0.9F, 0F, 0.7F, 0.7F);
      writer.addRectangle(1.8f, 0F, 0.7F, 0.7F);
   }

   private static final String TOP_DESC = "The Mycelium Bitcoin Wallet performs the steps described below when decrypting and verifying an encrypted private key. The description is quite technical and allows a developer to create software that allows you to decrypt your private keys. This allows you to access your funds if the Mycelium software is no longer available. If you wish to read or review the implementation used by the Mycelium Bitcoin Wallet you can find it here:";
   private static final String TOP_DESC_LINK = "https://github.com/mycelium-com/wallet/tree/master/public/bitlib/src/main/java/com/mrd/bitlib/crypto/MrdExport.java";

   private static final String PARSE_HEADING = "Parsing The QR Code";
   private static final String PARSE_1 = "Scan the QR code to get a Base64 encoded string.";
   private static final String PARSE_2 = "Decode the Base64 encoded string to get exactly 46 bytes. The Base64 variant used is designed for URLs as specified in RFC 4648 section 5.";
   private static final String PARSE_3 = "The first 3 bytes are the the magic cookie 0xC4 0x49 0xDC: decoded[0...2]";
   private static final String PARSE_4 = "The next 3 bytes are the header bytes: H = decoded[3...5]";
   private static final String PARSE_5 = "The next 4 bytes is the random salt: SALT = decoded[6...9]";
   private static final String PARSE_6 = "The next 32 bytes are the encrypted private key: E = decoded[10...41]";
   private static final String PARSE_7 = "The next 4 bytes are the checksum: C = decoded[42...45]";

   private static final String DECODE_HEADER_HEADING = "Decoding the 3 Header Bytes";
   private static final String DECODE_HEADER_1 = "Regard the header as an array of 24 bits and decode the following values:";
   private static final String DECODE_HEADER_2 = "version    = XXXX???? ???????? ???????? (must be 1)";
   private static final String DECODE_HEADER_3 = "network    = ????X??? ???????? ???????? (0 = prodnet, 1 = testnet)";
   private static final String DECODE_HEADER_4 = "compressed = ???????X ???????? ???????? (0 = uncompressed, 1 = compressed)";
   private static final String DECODE_HEADER_5 = "HN         = ???????? XXXXX??? ???????? (0 <= HN <= 31)";
   private static final String DECODE_HEADER_6 = "Hr         = ???????? ?????XXX XX?????? (1 <= Hr <= 31)";
   private static final String DECODE_HEADER_7 = "Hp         = ???????? ???????? ??XXXXX? (1 <= Hp <= 31)";
   private static final String DECODE_HEADER_8 = "reserved   = ?????XX? ???????? ???????X (must be zero)";

   private static final String AES_HEADING = "AES Key Derivation";
   private static final String AES_1 = "Make the user enter a 15-character password using characters A-Z, all in upper case. Convert the characters to 15 bytes using normal ASCII conversion. An implementations may use additional checksum characters for password integrity. They are not part of the AES key derivation.";
   private static final String AES_2 = "Run scrypt using parameters N = 2^HN, r = Hr, p = Hp on the password bytes and SALT, to derive 32 bytes.";
   private static final String AES_3 = "The 32 output bytes are used as the 256-bit AES key used for decryption.";

   private static final String DECRYPT_HEADING = "Decrypting the Private Key";
   private static final String DECRYPT_DESC = "The decryption function is 256-bit AES in CBC mode.";
   private static final String DECRYPT_1 = "Generate the AES initialization vector (IV) by doing a single round of SHA256 on the concatenation of SALT and C, and use the first 16 bytes of the output.";
   private static final String DECRYPT_2 = "Split E into two 16-byte blocks E1 and E2";
   private static final String DECRYPT_3 = "Do an AES block decryption of E1 into P1 using the derived AES key";
   private static final String DECRYPT_4 = "X-or the initialization vector onto P1: P1 = P1 xor IV";
   private static final String DECRYPT_5 = "Do an AES block decryption of E2 into P2 using the derived AES key";
   private static final String DECRYPT_6 = "X-or E1 onto P2: P2 = P2 xor E1";
   private static final String DECRYPT_7 = "The 32 byte private key is the concatenation of P1 and P2: P = P1 || P2";

   private static final String GENERATE_ADDRESS_HEADING = "Generating the Bitcoin Address";
   private static final String GENERATE_ADDRESS_DESC = "For each bitcoin private key there are two public keys. A compressed public key and an uncompressed public key. As each public key gives a different bitcoin address it is important to use the right one. The right public key to use is encoded in the header (compressed). Bitcoin addresses for the test network are different from the production (real) network. The network that this private key is for is encoded in the header (network).";

   private static final String VERIFY_HEADING = "Verifying the Checksum";
   private static final String VERIFY_1 = "Convert the generated bitcoin address to an array of ASCII bytes";
   private static final String VERIFY_2 = "Do a single SHA256 operation on the array of bytes";
   private static final String VERIFY_3 = "The checksum is the first 4 bytes of the output";
   private static final String VERIFY_4 = "Verify that the calculated checksum equals C.";
   private static final String VERIFY_TEXT = "If a wrong password was entered the checksums will not match.";

   private static final String FUNCTIONS_HEADING = "Cryptographic Functions Used";
   private static final String FUNCTIONS_1 = "AES - http://csrc.nist.gov/publications/fips/fips197/fips-197.pdf";
   private static final String FUNCTIONS_2 = "SHA-256 - http://csrc.nist.gov/publications/fips/fips180-4/fips-180-4.pdf";
   private static final String FUNCTIONS_3 = "scrypt - http://www.tarsnap.com/scrypt/scrypt.pdf";

   private static void addFinalPage(MyWriter writer, int pageNum) {
      writer.addPage();
      addPageNumber(writer, pageNum, pageNum);
      double fromTop = 0;
      fromTop = addBodyText(writer, fromTop, TOP_DESC);
      fromTop = addBodyText(writer, fromTop, TOP_DESC_LINK);
      fromTop = addGap(fromTop);

      int item = 1;
      fromTop = addHeadingText(writer, fromTop, PARSE_HEADING);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_1);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_2);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_3);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_4);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_5);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_6);
      fromTop = addListItemText(writer, fromTop, item++, PARSE_7);
      fromTop = addGap(fromTop);

      item = 1;
      fromTop = addHeadingText(writer, fromTop, DECODE_HEADER_HEADING);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_1);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_2);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_3);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_4);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_5);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_6);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_7);
      fromTop = addListItemText(writer, fromTop, item++, DECODE_HEADER_8);
      fromTop = addGap(fromTop);

      item = 1;
      fromTop = addHeadingText(writer, fromTop, AES_HEADING);
      fromTop = addListItemText(writer, fromTop, item++, AES_1);
      fromTop = addListItemText(writer, fromTop, item++, AES_2);
      fromTop = addListItemText(writer, fromTop, item++, AES_3);
      fromTop = addGap(fromTop);

      item = 1;
      fromTop = addHeadingText(writer, fromTop, DECRYPT_HEADING);
      fromTop = addBodyText(writer, fromTop, DECRYPT_DESC);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_1);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_2);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_3);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_4);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_5);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_6);
      fromTop = addListItemText(writer, fromTop, item++, DECRYPT_7);
      fromTop = addGap(fromTop);

      fromTop = addHeadingText(writer, fromTop, GENERATE_ADDRESS_HEADING);
      fromTop = addBodyText(writer, fromTop, GENERATE_ADDRESS_DESC);
      fromTop = addGap(fromTop);

      item = 1;
      fromTop = addHeadingText(writer, fromTop, VERIFY_HEADING);
      fromTop = addListItemText(writer, fromTop, item++, VERIFY_1);
      fromTop = addListItemText(writer, fromTop, item++, VERIFY_2);
      fromTop = addListItemText(writer, fromTop, item++, VERIFY_3);
      fromTop = addListItemText(writer, fromTop, item++, VERIFY_4);
      fromTop = addBodyText(writer, fromTop, VERIFY_TEXT);
      fromTop = addGap(fromTop);

      fromTop = addHeadingText(writer, fromTop, FUNCTIONS_HEADING);
      fromTop = addBodyText(writer, fromTop, FUNCTIONS_1);
      fromTop = addBodyText(writer, fromTop, FUNCTIONS_2);
      fromTop = addBodyText(writer, fromTop, FUNCTIONS_3);

   }

   private static double addHeadingText(MyWriter writer, double fromTop, String text) {
      writer.setTextColor(0, 0.5, 1);
      writer.setStandardFont();
      writer.addText(1F, fromTop, 13, text);
      return fromTop + 0.6;
   }

   private static double addGap(double fromTop) {
      return fromTop + 0.3;
   }

   private static final int BODY_TEXT_LINE_LENGTH = 80;

   private static double addBodyText(MyWriter writer, double fromTop, String text) {
      writer.setTextColor(0, 0, 0);
      writer.setMonoFont();
      List<String> lines = chopByWords(text, BODY_TEXT_LINE_LENGTH);
      for (String line : lines) {
         writer.addText(1F, fromTop, 9, line);
         fromTop += 0.375;
      }
      return fromTop;
   }

   private static double addListItemText(MyWriter writer, double fromTop, int itemNumber, String text) {
      writer.setTextColor(0, 0, 0);
      writer.setMonoFont();
      List<String> lines = chopByWords(text, BODY_TEXT_LINE_LENGTH - 3);
      boolean first = true;
      for (String line : lines) {
         if (first) {
            line = "" + itemNumber + ". " + line;
            first = false;
         } else {
            line = "   " + line;
         }
         writer.addText(1F, fromTop, 9, line);
         fromTop += 0.375;
      }
      return fromTop;
   }

   private static List<String> chopByWords(String text, int maxLength) {
      List<String> lines = new LinkedList<String>();
      String[] words = text.split(" ");
      String currentLine = "";
      for (int i = 0; i < words.length; i++) {
         String word = words[i].trim();
         String[] splittedWord = wordSplitter(word, maxLength);
         for (String splinter : splittedWord) {
            if (currentLine.length() != 0) {
               if (currentLine.length() + splinter.length() > maxLength) {
                  lines.add(currentLine.trim());
                  currentLine = "";
               }
            }
            currentLine = currentLine + splinter + " ";
         }
      }
      if (currentLine.length() > 0) {
         lines.add(currentLine.trim());
      }
      return lines;
   }

   private static String[] wordSplitter(String word, int maxLength) {
      if (word.length() < maxLength) {
         return new String[] { word };
      }
      int num = (word.length() + maxLength - 1) / maxLength;
      String[] chopped = new String[num];
      for (int i = 0; i < num; i++) {
         int startIndex = i * maxLength;
         int endIndex = (i + 1) * maxLength;
         if (word.length() < endIndex) {
            endIndex = word.length();
         }
         chopped[i] = word.substring(startIndex, endIndex);
      }
      return chopped;
   }

   private static class OffsetWriter extends MyWriter {

      public OffsetWriter(double cmX, double cmY, OffsetWriter parent) {
         super(parent);
         _offX = translateCmX(cmX) + parent._offX;
         _offY = translateCmY(cmY) + parent._offY;
      }

      public OffsetWriter(double cmX, double cmY, MyWriter writer) {
         super(writer);
         _offX = translateCmX(cmX);
         _offY = translateCmY(cmY);
      }

   }

   private static class MyWriter {
      private PDFWriter _writer;
      private int _pageWidth;
      private int _pageHeight;
      private int _marginLeft;
      private int _marginTop;
      private int _marginRight;
      private int _marginBottom;
      protected int _offX;
      protected int _offY;

      public MyWriter(int pageWidth, int pageHeight, int marginLeft, int marginRight, int marginTop, int marginBottom) {
         _pageWidth = pageWidth;
         _pageHeight = pageHeight;
         _writer = new PDFWriter(pageWidth, pageHeight);
         _marginLeft = marginLeft;
         _marginRight = marginRight;
         _marginTop = marginTop;
         _marginBottom = marginBottom;
         _offX = 0;
         _offY = 0;
         setStandardFont();

         // Add visible Bounding box
         // _writer.addRectangle(1, 1, _pageWidth - 2, _pageHeight - 2);
      }

      public MyWriter(MyWriter writer) {
         _pageWidth = writer._pageWidth;
         _pageHeight = writer._pageHeight;
         _writer = writer._writer;
         _marginLeft = writer._marginLeft;
         _marginRight = writer._marginRight;
         _marginTop = writer._marginTop;
         _marginBottom = writer._marginBottom;
         _offX = 0;
         _offY = 0;
      }

      public int getWidth() {
         return _pageWidth - _marginLeft - _marginRight;
      }

      public int getHeight() {
         return _pageHeight - _marginTop - _marginBottom;
      }

      public void setTextColor(double r, double g, double b) {
         _writer.addRawContent(r + " " + g + " " + b + " rg\n");
      }

      public void setLineColor(double r, double g, double b) {
         _writer.addRawContent(r + " " + g + " " + b + " RG\n");
      }

      public void setStandardFont() {
         _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.TIMES_ROMAN);
      }

      public void setMonoFont() {
         _writer.setFont(StandardFonts.SUBTYPE, StandardFonts.COURIER);
      }

      public void addPage() {
         _writer.newPage();
      }

      public void addText(int x, int y, int fontSize, String text) {
         _writer.addText(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y - fontSize, fontSize, text);
      }

      public void addText(double cmX, double cmY, int fontSize, String text) {
         addText(translateCmX(cmX), translateCmY(cmY), fontSize, text);
      }

      public void addRectangle(double cmX, double cmY, double cmWidth, double cmHeight) {
         addRectangle(translateCmX(cmX), translateCmY(cmY), translateCmX(cmWidth), translateCmY(cmHeight));
      }

      public void addRectangle(int x, int y, int width, int height) {
         _writer.addRectangle(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y, width, -height);
      }

      public void addFilledRectangle(double x, double y, double width, double height) {
         _writer.addFilledRectangle(_marginLeft + _offX + x, _pageHeight - _marginTop - _offY - y, width, -height);
      }

      public void addLine(double cmX1, double cmY1, double cmX2, double cmY2) {
         addLine(translateCmX(cmX1), translateCmY(cmY1), translateCmX(cmX2), translateCmY(cmY2));
      }

      public void addLine(int x1, int y1, int x2, int y2) {
         _writer.addLine(_marginLeft + _offX + x1, _pageHeight - _marginTop - _offY - y1, _marginLeft + _offX + x2,
               _pageHeight - _marginTop - _offY - y2);
      }

      @SuppressWarnings("unused")
      public void addImage(double cmX, double cmY, double cmWidth, double cmHeight, Bitmap bitmap) {
         addImage(translateCmX(cmX), translateCmY(cmY), translateCmX(cmWidth), translateCmY(cmHeight), bitmap);
      }

      public void addImage(int x, int y, int width, int height, Bitmap bitmap) {
         _writer.addImageKeepRatio(_offX + _marginLeft + x, _pageHeight - _marginTop - _offY - y - height, width,
               height, bitmap);
      }

      public void addQrCode(double cmX, double cmY, double cmSize, String url) {
         BitMatrix matrix = getQRCodeMatrix(url);
         int xPos = translateCmX(cmX);
         int yPos = translateCmX(cmY);
         int width = matrix.getWidth();
         int height = matrix.getHeight();
         double size = translateCmX(cmSize);
         double boxHeight = size / height;
         double boxWidth = size / width;
         double boxFillHeight = boxHeight + 0.1;
         double boxFillWidth = boxWidth + 0.1;
         for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
               if (matrix.get(x, y)) {
                  addFilledRectangle(toTwoDecimalPalces(xPos + boxWidth * x), toTwoDecimalPalces(yPos + boxHeight * y),
                        toTwoDecimalPalces(boxFillWidth), toTwoDecimalPalces(boxFillHeight));
               }
            }
         }
      }

      private double toTwoDecimalPalces(double value) {
         return Math.round(value * 10) / 10.0;
      }

      private static BitMatrix getQRCodeMatrix(String url) {
         Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
         hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
         hints.put(EncodeHintType.MARGIN, 0);
         try {
            return new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, hints);
         } catch (final WriterException e) {
            throw new RuntimeException(e);
         }
      }

      public String asString() {
         return _writer.asString();
      }

      public int translateCmX(double cmX) {
         return (int) (cmX / 19.2F * getWidth());
      }

      public int translateCmY(double cmY) {
         return (int) (cmY / 27F * getHeight());
      }
   }

}
