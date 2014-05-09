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

package com.mycelium.wallet;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Environment;
import android.text.ClipboardManager;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.model.UnspentTransactionOutput;
import com.mrd.mbwapi.api.QueryUnspentOutputsResponse;
import com.mycelium.wallet.Record.Tag;
import com.mycelium.wallet.activity.export.BackupToPdfActivity;
import com.mycelium.wallet.activity.export.ExportAsQrCodeActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.Charset;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

@SuppressWarnings("deprecation")
public class Utils {

   private static final DecimalFormat FIAT_FORMAT;

   static {
      FIAT_FORMAT = new DecimalFormat();
      FIAT_FORMAT.setGroupingSize(3);
      FIAT_FORMAT.setGroupingUsed(true);
      FIAT_FORMAT.setMaximumFractionDigits(2);
      FIAT_FORMAT.setMinimumFractionDigits(2);
      DecimalFormatSymbols symbols = FIAT_FORMAT.getDecimalFormatSymbols();
      symbols.setDecimalSeparator('.');
      symbols.setGroupingSeparator(' ');
      FIAT_FORMAT.setDecimalFormatSymbols(symbols);
   }

   @SuppressLint(Constants.IGNORE_NEW_API)
   public static void setAlpha(View view, float alpha) {
      if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
         AlphaAnimation aa = new AlphaAnimation(alpha, alpha);
         aa.setDuration(Long.MAX_VALUE);
         view.startAnimation(aa);
      } else {
         view.setAlpha(alpha);
      }
   }

   public static String loadEnglish(int resId) {
      // complex code messes up stuff, hardcoding two strings
      if (resId == R.string.settings) {
         return "Settings";
      }
      if (resId == R.string.pref_change_language) {
         return "Change Language";
      }
      throw new UnsupportedOperationException("not implemented");

      /*
       * Resources standardResources = getResources(); AssetManager assets =
       * standardResources.getAssets(); DisplayMetrics metrics =
       * standardResources.getDisplayMetrics(); Configuration config = new
       * Configuration(standardResources.getConfiguration()); config.locale =
       * Locale.US; Resources defaultResources = new Resources(assets, metrics,
       * config);
       * 
       * String lang = Locale.getDefault().getLanguage(); String settingsEn =
       * null; if (!lang.equals("en")) { settingsEn =
       * defaultResources.getString(resId); } return settingsEn;
       */
   }

   public static class BitcoinScanResult {
      public Address address;
      public Long amount;

      public BitcoinScanResult(Address address, Long amount) {
         this.address = address;
         this.amount = amount;
      }
   }

   public static BitcoinScanResult parseScanResult(final Intent intent, NetworkParameters network) {
      if (!("QR_CODE".equals(intent.getStringExtra("SCAN_RESULT_FORMAT")))) {
         return null;
      }
      String contents = intent.getStringExtra("SCAN_RESULT").trim();

      // Determine address string and amount
      if (contents.matches("[a-zA-Z0-9]*")) {
         // Raw format
         Address address = Address.fromString(contents.trim(), network);
         if (address == null) {
            return null;
         }
         return new BitcoinScanResult(address, null);
      } else {
         BitcoinUri b = BitcoinUri.parse(contents, network);
         if (b != null) {
            // On URI format
            return new BitcoinScanResult(b.address, b.amount);
         }
      }

      return null;
   }

   public static long maxAmountSendable(QueryUnspentOutputsResponse unspent) {
      long amount = 0;
      for (UnspentTransactionOutput out : unspent.unspent) {
         amount += out.value;
      }
      for (UnspentTransactionOutput out : unspent.change) {
         amount += out.value;
      }
      return amount;
   }

   public static Bitmap getLargeQRCodeBitmap(String text, MbwManager manager) {
      // make size 85% of display size
      int size = Math.min(manager.getDisplayWidth(), manager.getDisplayHeight()) * 81 / 100;
      int margin = Math.min(manager.getDisplayWidth(), manager.getDisplayHeight()) * 8 / 100;
      return getQRCodeBitmap(text, size, margin);
   }

   public static Bitmap getQRCodeBitmap(String url, int size, int margin) {
      Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, 0);
      return getQRCodeBitmap(url, size, margin, hints);
   }

   private static Bitmap getQRCodeBitmap(String url, int size, int margin, Hashtable<EncodeHintType, Object> hints) {
      try {
         final BitMatrix result = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, hints);

         final int width = result.getWidth();
         final int height = result.getHeight();
         final int[] pixels = new int[width * height];

         for (int y = 0; y < height; y++) {
            final int offset = y * width;
            for (int x = 0; x < width; x++) {
               pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
         }

         final Bitmap smallBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
         smallBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

         Bitmap largeBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
         Canvas canvas = new Canvas(largeBitmap);
         Paint p = new Paint();
         p.setDither(false);
         p.setAntiAlias(false);
         Rect src = new Rect(0, 0, smallBitmap.getWidth(), smallBitmap.getHeight());
         Rect dst = new Rect(margin, margin, largeBitmap.getWidth() - margin, largeBitmap.getHeight() - margin);
         canvas.drawColor(0xFFFFFFFF);
         canvas.drawBitmap(smallBitmap, src, dst, p);
         return largeBitmap;
      } catch (final WriterException x) {
         x.printStackTrace();
         return null;
      }
   }

   public static Bitmap getMinimalQRCodeBitmap(String url) {
      Hashtable<EncodeHintType, Object> hints = new Hashtable<EncodeHintType, Object>();
      hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
      hints.put(EncodeHintType.MARGIN, 5);

      try {
         final BitMatrix result = new QRCodeWriter().encode(url, BarcodeFormat.QR_CODE, 0, 0, hints);

         final int width = result.getWidth();
         final int height = result.getHeight();
         final int[] pixels = new int[width * height];

         for (int y = 0; y < height; y++) {
            final int offset = y * width;
            for (int x = 0; x < width; x++) {
               pixels[offset + x] = result.get(x, y) ? Color.BLACK : Color.WHITE;
            }
         }

         final Bitmap smallBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
         smallBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
         return smallBitmap;
      } catch (final WriterException x) {
         x.printStackTrace();
         return null;
      }
   }

   public static AlertDialog showQrCode(final Context context, int titleMessageId, Bitmap qrCode, final String value,
         int buttonLabelId, boolean pulse) {
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      View layout = inflater.inflate(R.layout.qr_code_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog qrCodeDialog = builder.create();
      qrCodeDialog.setCanceledOnTouchOutside(true);
      TextView text = (TextView) layout.findViewById(R.id.tvTitle);
      text.setText(titleMessageId);

      ImageView qrAdress = (ImageView) layout.findViewById(R.id.ivQrCode);
      qrAdress.setImageBitmap(qrCode);
      qrAdress.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            qrCodeDialog.dismiss();
         }
      });

      Button copy = (Button) layout.findViewById(R.id.btCopyToClipboard);
      copy.setText(buttonLabelId);
      copy.setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            Utils.setClipboardString(value, context);
            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show();
         }
      });

      // Make QR code fade along with the entire view
      if (pulse) {
         layout.startAnimation(AnimationUtils.loadAnimation(context, R.anim.slow_pulse));
      }
      qrCodeDialog.show();
      return qrCodeDialog;
   }

   public static boolean isConnected(Context context) {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo[] NI = cm.getAllNetworkInfo();
      for (int i = 0; i < NI.length; i++) {
         if (NI[i].isConnected()) {
            return true;
         }
      }
      return false;
   }

   public static void toastConnectionError(Context context) {
      if (isConnected(context)) {
         Toast.makeText(context, R.string.no_server_connection, Toast.LENGTH_LONG).show();
      } else {
         Toast.makeText(context, R.string.no_network_connection, Toast.LENGTH_LONG).show();
      }
   }

   public static void moveView(View view, int startDeltaX, int startDeltaY, long duration) {
      moveView(view, startDeltaX, startDeltaY, 0, 0, duration);
   }

   public static void moveView(View view, int startDeltaX, int startDeltaY, int endDeltaX, int endDeltaY, long duration) {
      AnimationSet set = new AnimationSet(true);
      Animation move = new TranslateAnimation(startDeltaX, endDeltaX, startDeltaY, endDeltaY);
      move.setDuration(duration);
      move.setFillAfter(true);
      move.setZAdjustment(Animation.ZORDER_TOP);
      set.addAnimation(move);
      set.setFillAfter(true);
      view.startAnimation(set);
   }

   public static void fadeViewInOut(View view) {
      fadeViewInOut(view, 0, 1000, 1000);
   }

   public static void fadeViewInOut(View view, long startDelay, long fadeTime, long stayTime) {
      AnimationSet set = new AnimationSet(false);
      Animation in = new AlphaAnimation(0.0f, 1.0f);
      in.setStartOffset(startDelay);
      in.setDuration(fadeTime);
      in.setFillAfter(false);
      in.setZAdjustment(Animation.ZORDER_TOP);
      set.addAnimation(in);
      Animation out = new AlphaAnimation(1.0f, 0.0f);
      out.setStartOffset(fadeTime + startDelay + stayTime);
      out.setDuration(fadeTime);
      out.setFillAfter(true);
      out.setZAdjustment(Animation.ZORDER_TOP);
      set.addAnimation(out);
      set.setFillAfter(true);
      view.startAnimation(set);
   }

   public static void showSimpleMessageDialog(final Context context, int messageResource) {
      showSimpleMessageDialog(context, messageResource, null);
   }

   public static void showSimpleMessageDialog(final Context context, int messageResource, Runnable postRunner) {
      String message = context.getResources().getString(messageResource);
      showSimpleMessageDialog(context, message, postRunner);
   }

   /**
    * Show a dialog without buttons that displays a message. Click the message
    * or the back button to make it disappear.
    */
   public static void showSimpleMessageDialog(final Context context, String message) {
      showSimpleMessageDialog(context, message, null);
   }

   /**
    * Show a dialog without buttons that displays a message. Click the message
    * or the back button to make it disappear.
    */
   public static void showSimpleMessageDialog(final Context context, String message, final Runnable postRunner) {
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.simple_message_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();
      TextView tvMessage = ((TextView) layout.findViewById(R.id.tvMessage));
      tvMessage.setText(message);
      layout.findViewById(R.id.btOk).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            dialog.dismiss();
            if (postRunner != null) {
               postRunner.run();
            }
         }
      });
      dialog.show();
   }

   /**
    * Show an optional message/
    * <p>
    * The user can check a "never shot this again" check box and the message
    * will never get displayed again.
    * 
    * @param context
    *           The context
    * @param messageResourceId
    *           The resource ID of the message to show
    */
   public static boolean showOptionalMessage(final Context context, int messageResourceId) {
      String message = context.getString(messageResourceId);
      final String optionalMessageId = Integer.toString(message.hashCode());
      SharedPreferences settings = context.getSharedPreferences("optionalMessagePreferences", Activity.MODE_PRIVATE);
      boolean ignore = settings.getBoolean(optionalMessageId, false);
      if (ignore) {
         // The user has opted never to get this message shown again
         return false;
      }

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.optional_message_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();
      TextView tvMessage = ((TextView) layout.findViewById(R.id.tvMessage));
      tvMessage.setText(message);
      CheckBox cb = (CheckBox) layout.findViewById(R.id.checkbox);
      cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // Persist checked state
            context.getSharedPreferences("optionalMessagePreferences", Activity.MODE_PRIVATE).edit()
                  .putBoolean(optionalMessageId, isChecked).commit();
         }
      });

      layout.findViewById(R.id.btOk).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            dialog.dismiss();
         }
      });
      dialog.show();
      return true;
   }

   /**
    * Chop a string into an array of string no longer then the specified chop
    * length
    */
   public static String[] stringChopper(String string, int chopLength) {
      return Iterables.toArray(Splitter.fixedLength(chopLength).split(string), String.class);
   }

   public static Double getFiatValue(long satoshis, Double oneBtcInFiat) {
      if (oneBtcInFiat == null) {
         return null;
      }
      return Double.valueOf(satoshis) * oneBtcInFiat / Constants.ONE_BTC_IN_SATOSHIS;
   }

   public static String getFiatValueAsString(long satoshis, Double oneBtcInFiat) {
      Double converted = getFiatValue(satoshis, oneBtcInFiat);
      if (converted == null) {
         return null;
      }
      return FIAT_FORMAT.format(converted);
   }

   private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100L);
   private static final BigDecimal BTC_IN_SATOSHIS = BigDecimal.valueOf(Constants.ONE_BTC_IN_SATOSHIS);

   public static Long getSatoshis(BigDecimal fiatValue, Double oneBtcInFiat) {
      if (fiatValue == null || oneBtcInFiat == null) {
         return null;
      }
      BigDecimal fiatCents = fiatValue.multiply(ONE_HUNDRED);
      BigDecimal oneBtcInFiatCents = BigDecimal.valueOf(oneBtcInFiat).multiply(ONE_HUNDRED);
      return fiatCents.multiply(BTC_IN_SATOSHIS).divide(oneBtcInFiatCents, 0, RoundingMode.HALF_UP).longValue();
   }

   public static void setClipboardString(String string, Context context) {
      try {
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setText(string);
      } catch (Exception e) {
         // Ingore
         // todo insert uncaught error handler
      }
   }

   public static String getClipboardString(Activity activity) {
      try {
         ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
         CharSequence content = clipboard.getText();
         if (content == null) {
            return "";
         }
         return content.toString();
      } catch (Exception e) {
         // todo insert uncaught error handler
         return "";
      }
   }

   public static void clearClipboardString(Activity activity) {
      try {
         ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setText("");
      } catch (Exception e) {
         // todo insert uncaught error handler
         // Ignore
      }
   }

   public static Address addressFromString(String someString, NetworkParameters network) {
      if (someString == null) {
         return null;
      }
      someString = someString.trim();
      if (someString.matches("[a-zA-Z0-9]*")) {
         // Raw format
         return Address.fromString(someString, network);
      } else {
         BitcoinUri b = BitcoinUri.parse(someString, network);
         if (b != null) {
            // On URI format
            return b.address;
         }
      }
      return null;
   }

   /**
    * Truncate and transform a decimal string to a maximum number of digits
    * <p>
    * The string will be truncated and verified to be a valid decimal number
    * with one comma or dot separator. A comma separator will be converted to a
    * dot. The resulting string will have at most the number of decimals
    * specified
    * 
    * @param number
    *           the number to truncate
    * @param maxDecimalPlaces
    *           the maximum number of decimal places
    * @return a truncated decimal string or null if the input string is not a
    *         valid decimal string
    */
   public static String truncateAndConvertDecimalString(String number, int maxDecimalPlaces) {
      if (number == null) {
         return null;
      }
      number = number.trim();
      if (!isValidDecimalNumber(number)) {
         return null;
      }

      // We now have a string with at least one digit before the separator
      // If it has a separator there is only one and it it is a dot or a comma
      // If it has a separator there will be at least one decimal after the
      // separator
      // All characters except the separator are between 0 and 9

      // Replace comma with dot
      number = number.replace(',', '.');

      boolean foundDot = false;
      int decimals = 0;
      char[] chars = number.toCharArray();
      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];

         // Check for dot
         if (c == '.') {
            if (maxDecimalPlaces == 0) {
               // We want everything till now except the dot
               return number.substring(0, i);
            }
            foundDot = true;
            continue;
         }

         // Count decimal places
         if (foundDot) {
            decimals++;
         }

         if (maxDecimalPlaces == decimals) {
            // We want everything till now
            return number.substring(0, i + 1);
         }

      }
      // We want everything;
      return number;
   }

   private static boolean isValidDecimalNumber(String string) {
      if (string == null) {
         return false;
      }
      if (string.length() == 0) {
         return false;
      }
      boolean foundDot = false;
      boolean foundComma = false;
      int digitsBefore = 0;
      int digitsAfter = 0;
      char[] chars = string.toCharArray();
      for (int i = 0; i < chars.length; i++) {
         char c = chars[i];

         // Check for digits
         if (c == '.') {
            if (foundDot || foundComma) {
               return false;
            }
            foundDot = true;
            continue;
         }

         // Check for comma
         if (c == ',') {
            if (foundDot || foundComma) {
               return false;
            }
            foundComma = true;
            continue;
         }

         // Only digits
         if (c < '0' || c > '9') {
            return false;
         }

         // Count decimal places
         if (foundDot || foundComma) {
            digitsAfter++;
         } else {
            digitsBefore++;
         }
      }
      if (digitsBefore == 0) {
         // There must be something before the decimal separator
         return false;
      }
      if ((foundDot || foundComma) && digitsAfter == 0) {
         // There must be something after the decimal separator
         return false;
      }

      return true;
   }

   public static void pinProtectedBackup(final Activity activity) {
      MbwManager manager = MbwManager.getInstance(activity);
      manager.runPinProtectedFunction(activity, new Runnable() {

         @Override
         public void run() {
            Utils.backup(activity);
         }
      });

   }

   private static void backup(final Activity parent) {

      // Get a list of all records
      RecordManager recordManager = MbwManager.getInstance(parent).getRecordManager();
      List<Record> records = new LinkedList<Record>();
      records.addAll(recordManager.getRecords(Tag.ACTIVE));
      records.addAll(recordManager.getRecords(Tag.ARCHIVE));

      if (records.size() == 1 && records.get(0).hasPrivateKey()) {
         // If there is only one record, and it has a private key we let the
         // user choose which backup method to use
         backupSingleRecord(parent, records.get(0));
      } else {
         // Otherwise we automatically launch encrypted PDF backup
         backupAllRecords(parent);
      }
   }

   private static void backupSingleRecord(final Activity parent, final Record record) {
      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.backup_single_private_key_warning).setCancelable(true)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
                  if (record == null) {
                     return;
                  }
                  BackupToPdfActivity.callMe(parent);
               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   private static void backupAllRecords(final Activity parent) {
      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.backup_all_warning).setCancelable(true)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
                  BackupToPdfActivity.callMe(parent);
               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   public static void exportSelectedPrivateKey(final Activity parent) {
      final Record record = MbwManager.getInstance(parent).getRecordManager().getSelectedRecord();
      if (record == null || !record.hasPrivateKey()) {
         return;
      }
      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.export_single_private_key_warning).setCancelable(true)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
                  Intent intent = new Intent(parent, ExportAsQrCodeActivity.class);
                  parent.startActivity(intent);
               }
            }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
               }
            });
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   public static void addHorizontalSwipeDotView(Context context, LinearLayout root, int dots, int selected) {
      // Calculate size of dots and gaps
      DisplayMetrics metrics = context.getResources().getDisplayMetrics();
      int imageSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);
      int gapWidth = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8, metrics);

      // Create layout parameters for images and gaps
      LinearLayout.LayoutParams imageParameters = new LinearLayout.LayoutParams(imageSize, imageSize, 0);
      LinearLayout.LayoutParams gapParameters = new LinearLayout.LayoutParams(gapWidth, 1, 0);

      // Create horizontal layout
      LinearLayout layout = new LinearLayout(context);
      layout.setOrientation(LinearLayout.HORIZONTAL);
      layout.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LayoutParams.FILL_PARENT, 0));

      // Fetch drawables for full and line circles
      Drawable full = context.getResources().getDrawable(R.drawable.circle_full_white);
      Drawable line = context.getResources().getDrawable(R.drawable.circle_line_white);

      boolean first = true;
      for (int i = 0; i < dots; i++) {
         if (first) {
            first = false;
         } else {
            View gap = new View(context);
            gap.setLayoutParams(gapParameters);
            layout.addView(gap);
         }
         ImageView image = new ImageView(context);
         image.setLayoutParams(imageParameters);
         if (i == selected) {
            image.setImageDrawable(full);
         } else {
            image.setImageDrawable(line);
         }
         layout.addView(image);
      }

      root.addView(layout);
   }

   /**
    * Prevent the OS from taking screenshots for the specified activity
    */
   public static void preventScreenshots(Activity activity) {
      // looks like gingerbread devices have this issue more commonly than
      // thought.
      // future: make a setting for this, and somehow gather feedback what
      // works,
      // and a positive list of devices.
      if (android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.GINGERBREAD
            || android.os.Build.VERSION.SDK_INT == Build.VERSION_CODES.GINGERBREAD_MR1) {
         return;
      }
      activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
   }

   /**
    * Search for possible backup files created by Schildbach's "Bitcoin Wallet" app
    * (matching by filename).
    *
    * @return list of possible files or empty list if none found
    */
   public static ArrayList<File> findAndroidWalletBackupFiles(final NetworkParameters network) {
      final File[] foundFiles;
      final String filenamePattern;
      if (network.isTestnet()) {
         filenamePattern = "bitcoin-wallet-keys-testnet-\\d\\d\\d\\d-\\d\\d-\\d\\d";
      } else {
         filenamePattern = "bitcoin-wallet-keys-\\d\\d\\d\\d-\\d\\d-\\d\\d";
      }
      File backupDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
      if (backupDir.exists() && backupDir.isDirectory()) {
         foundFiles = backupDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
               return filename.matches(filenamePattern);
            }
         });
         if (foundFiles.length > 1) {
            Arrays.sort(foundFiles, new Comparator<File>() {
               public int compare(File lhs, File rhs) {
                  return rhs.getName().compareTo(lhs.getName());
               }
            });
         }
         return new ArrayList<File>(Arrays.asList(foundFiles));
      }
      return new ArrayList<File>();
   }

   /**
    * Returns filecontent as string
    *
    * @param textfile
    * @return content of textfile as string
    * @throws java.io.IOException
    */
   public static String getFileContent(File textfile) throws IOException {
      final StringBuilder filecontent = new StringBuilder();
      final BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(textfile), Charset.forName("UTF-8")));
      while(true) {
         final String currLine = reader.readLine();
         if (currLine == null) {
            break;
         }
         filecontent.append(currLine);
      }
      return filecontent.toString();
   }

   /**
    * Decrypt text which was encrypted with password-based AES-256-CBC as it's done by OpenSSL.
    *
    * @param encryptedMessageBase64 encrypted ciphertext
    * @param password password used for encryption
    * @return decrypted message text
    * @throws java.security.GeneralSecurityException
    */
   public static String decryptOpenSslAes256Cbc(String encryptedMessageBase64, String password) throws GeneralSecurityException, UnsupportedEncodingException {
      final String charsetNameUtf8 = "UTF-8";

      // Offset from beginning of ciphertext where actual salt begins is always 8 bytes (when
      // using OpenSSL), as OpenSSL always places the magic string "Salted__" at the beginning
      // of the ciphertext to indicate that salt was used.
      final int SALT_OFFSET = 8;
      final int SALT_SIZE = 8; // next 8 bytes after header are the actual salt
      final int CIPHERTEXT_OFFSET = SALT_OFFSET + SALT_SIZE; // after that starts the ciphertext

      // As Bitcoin Wallet uses only salted encryption, we check if the ciphertext starts with
      // the string "Salted__". Base64-encoded the first bytes of the string "Salted__"
      // will look like "U2FsdGVkX1":
      if (!encryptedMessageBase64.startsWith("U2FsdGVkX1")) {
         throw new GeneralSecurityException("Ciphertext missing 'Salted__' header");
      }
      // Base64 decode the whole thing
      byte[] headerSaltAndCipherText = Base64.decode(encryptedMessageBase64, Base64.DEFAULT);

      byte[] salt = new byte[SALT_SIZE];
      // starts with magic header "Salted__" so omit first 8 bytes
      System.arraycopy(headerSaltAndCipherText, SALT_OFFSET, salt, 0, SALT_SIZE);
      // the rest is the actual ciphertext
      byte[] ciphertext = new byte[headerSaltAndCipherText.length - CIPHERTEXT_OFFSET];
      System.arraycopy(headerSaltAndCipherText, CIPHERTEXT_OFFSET, ciphertext, 0, headerSaltAndCipherText.length - CIPHERTEXT_OFFSET);

      Cipher aesCBC = Cipher.getInstance("AES/CBC/PKCS5Padding");
      MessageDigest md5 = MessageDigest.getInstance("MD5");

      // using getBytes(String charsetName) here as getBytes(Charset charset) is only available in API level >8
      byte[][] keyAndIV = openSslEVP_BytesToKey(256 / Byte.SIZE, aesCBC.getBlockSize(), md5, salt, password.getBytes("UTF-8"), 1);
      aesCBC.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndIV[0], "AES"), new IvParameterSpec(keyAndIV[1]));

      return new String(aesCBC.doFinal(ciphertext), charsetNameUtf8);
   }

   /**
    * Java implementation of OpenSSLs "EVP_BytesToKey()".<br/>
    * <br/>
    * This method is used to derive the IV and key for AES-256-CBC decryption from a given password
    * the same way as it's done by OpenSSL when using the commandline:
    *     "openssl enc -e -aes-256-cbc -a -in filenname.txt"
    * <br/><br/>
    * Thanks to Ola Bini for releasing sourcecode for this method on his blog.
    * This implementation is based on the sourcecode obtained from
    * http://olabini.com/blog/tag/evp_bytestokey/ (last accessed at May 08, 2014)
    * where it was released into public domain ("note, I release this into the public domain").
    */
   private static byte[][] openSslEVP_BytesToKey(int key_len, int iv_len, MessageDigest md, byte[] salt, byte[] data, int iterations) {
      byte[][] keyAndIv = new byte[2][];
      byte[] key = new byte[key_len];
      int key_ix = 0;
      byte[] iv = new byte[iv_len];
      int iv_ix = 0;
      keyAndIv[0] = key;
      keyAndIv[1] = iv;
      byte[] md_buf = null;
      int nkey = key_len;
      int niv = iv_len;
      int i = 0;
      if (data == null) {
         return keyAndIv;
      }
      int addmd = 0;
      for (;;) {
         md.reset();
         if (addmd++ > 0) {
            md.update(md_buf);
         }
         md.update(data);
         if (null != salt) {
            md.update(salt, 0, 8);
         }
         md_buf = md.digest();
         for (i = 1; i < iterations; i++) {
            md.reset();
            md.update(md_buf);
            md_buf = md.digest();
         }
         i = 0;
         if (nkey > 0) {
            for (;;) {
               if (nkey == 0)
                  break;
               if (i == md_buf.length)
                  break;
               key[key_ix++] = md_buf[i];
               nkey--;
               i++;
            }
         }
         if (niv > 0 && i != md_buf.length) {
            for (;;) {
               if (niv == 0)
                  break;
               if (i == md_buf.length)
                  break;
               iv[iv_ix++] = md_buf[i];
               niv--;
               i++;
            }
         }
         if (nkey == 0 && niv == 0) {
            break;
         }
      }
      for (i = 0; i < md_buf.length; i++) {
         md_buf[i] = 0;
      }
      return keyAndIv;
   }

}
