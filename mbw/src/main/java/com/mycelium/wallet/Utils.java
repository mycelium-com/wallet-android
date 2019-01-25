/*
 * Copyright 2013, 2014 Megion Research and Development GmbH
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

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Ordering;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.bitlib.util.CoinUtil;
import com.mycelium.wallet.activity.AdditionalBackupWarningActivity;
import com.mycelium.wallet.activity.BackupWordListActivity;
import com.mycelium.wallet.activity.export.BackupToPdfActivity;
import com.mycelium.wallet.activity.export.ExportAsQrActivity;
import com.mycelium.wallet.coinapult.CoinapultAccount;
import com.mycelium.wallet.colu.ColuAccount;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.AbstractAccount;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.HDAccount;
import com.mycelium.wapi.wallet.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.bip44.HDAccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.currency.BitcoinValue;
import com.mycelium.wapi.wallet.currency.CurrencyValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinCashValue;
import com.mycelium.wapi.wallet.currency.ExactBitcoinValue;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.TimeUnit;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nullable;

import static com.mycelium.wallet.Constants.TAG;

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
      symbols.setGroupingSeparator(',');
      FIAT_FORMAT.setDecimalFormatSymbols(symbols);
   }

   public static final Function<AddressBookManager.Entry, Comparable> ENTRY_NAME = new Function<AddressBookManager.Entry, Comparable>() {
      @Override
      public Comparable apply(AddressBookManager.Entry input) {
         return input.getName();
      }
   };

   @SuppressLint(Constants.IGNORE_NEW_API)
   public static void setAlpha(View view, float alpha) {
      view.setAlpha(alpha);
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

   public static Resources getResourcesByLocale(Context context, String localeName) {
      Configuration conf = context.getResources().getConfiguration();
      conf = new Configuration(conf);
      conf.setLocale(new Locale(localeName));
      Context localizedContext = context.createConfigurationContext(conf);
      return localizedContext.getResources();
   }

   public static Bitmap getMinimalQRCodeBitmap(String url) {
      Hashtable<EncodeHintType, Object> hints = new Hashtable<>();
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

   public static boolean isConnected(Context context) {
      ConnectivityManager cm =
              (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo activeNetwork = null;
      if (cm != null) {
         activeNetwork = cm.getActiveNetworkInfo();
      }
      return activeNetwork != null &&
              activeNetwork.isConnectedOrConnecting();
   }

   public static void toastConnectionError(Context context) {
      if (isConnected(context)) {
         Toast.makeText(context, R.string.no_server_connection, Toast.LENGTH_LONG).show();
      } else {
         Toast.makeText(context, R.string.no_network_connection, Toast.LENGTH_LONG).show();
      }
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

   public static void showSimpleMessageDialog(final Context context, int messageResource) {
      showSimpleMessageDialog(context, messageResource, null);
   }

   public static void showSimpleMessageDialog(final Context context, int messageResource, Runnable postRunner) {
      String message = context.getResources().getString(messageResource);
      showSimpleMessageDialog(context, message, null, postRunner);
   }

   /**
    * For ru locale pretty time library have problem, if(locale == "ru") fix this problem
    * for ru locale Duration should be not in past and not in future
    * otherwise library add "через" or "назад"
    */
   public static String formatBlockcountAsApproxDuration(final Context context, final int blocks) {
      MbwManager mbwManager = MbwManager.getInstance(context);
      PrettyTime p = new PrettyTime(mbwManager.getLocale());
      Date date = new Date((new Date()).getTime() + Math.max((long) blocks, 1L) * 10 * 60 * 1000);
      final Duration duration = p.approximateDuration(date);
      if (mbwManager.getLocale().getLanguage().equals("ru")) {
         Duration duration1 = new Duration(){

            @Override
            public long getQuantity() {
               return duration.getQuantity();
            }

            @Override
            public long getQuantityRounded(int tolerance) {
               return duration.getQuantityRounded(tolerance);
            }

            @Override
            public TimeUnit getUnit() {
               return duration.getUnit();
            }

            @Override
            public long getDelta() {
               return duration.getDelta();
            }

            @Override
            public boolean isInPast() {
               return false;
            }

            @Override
            public boolean isInFuture() {
               return false;
            }
         };
         return p.getFormat(duration1.getUnit()).decorate(duration1, p.formatDuration(duration1));
      } else {
         return p.formatDuration(duration);
      }
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
   public static void showSimpleMessageDialog(final Context context, String message, final Runnable okayRunner) {
      showSimpleMessageDialog(context, message, okayRunner, null);
   }

   /**
    * Show a dialog without buttons that displays a message. Click the message
    * or the back button to make it disappear.
    */
   public static void showSimpleMessageDialog(final Context context, String message, final Runnable okayRunner, final Runnable postRunner) {
      showSimpleMessageDialog(context, message, okayRunner, R.string.ok, postRunner);
   }

   /**
    * Show a dialog with a buttons that displays a message. Click the message
    * or the back button to make it disappear.
    */
   public static void showSimpleMessageDialog(final Context context, String message, final Runnable okayRunner,
                                              @StringRes int okayButtonText, final Runnable postRunner) {
      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.simple_message_dialog, null);
      TextView tvMessage = layout.findViewById(R.id.tvMessage);
      tvMessage.setText(message);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      builder.setPositiveButton(okayButtonText, new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialogInterface, int i) {
            if (okayRunner != null) {
               okayRunner.run();
            }
         }
      });
      builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialogInterface) {
            if (postRunner != null) {
               postRunner.run();
            }
         }
      });
      final AlertDialog dialog = builder.create();
      dialog.show();
   }

   /**
    * Show an optional message/
    * <p>
    * The user can check a "never shot this again" check box and the message
    * will never get displayed again.
    *
    * @param context           The context
    * @param messageResourceId The resource ID of the message to show
    */
   public static boolean showOptionalMessage(final Context context, int messageResourceId) {
      return showOptionalMessage(context, messageResourceId, null);
   }

   /**
    * Show an optional message/
    * <p>
    * The user can check a "never show this again" check box and the message
    * will never get displayed again.
    *
    * @param context           The context
    * @param messageResourceId The resource ID of the message to show
    * @param onOkay            This runnable gets executed either if the user clicks Okay or if he choose to never-see-this-message-again
    */
   public static boolean showOptionalMessage(final Context context, int messageResourceId, final Runnable onOkay) {
      String message = context.getString(messageResourceId);
      final String optionalMessageId = Integer.toString(message.hashCode());
      SharedPreferences settings = context.getSharedPreferences("optionalMessagePreferences", Activity.MODE_PRIVATE);
      boolean ignore = settings.getBoolean(optionalMessageId, false);
      // The user has opted never to get this message shown again
      if (ignore) {
         if (onOkay != null){
            onOkay.run();
         }
         return false;
      }

      LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
      final View layout = inflater.inflate(R.layout.optional_message_dialog, null);
      AlertDialog.Builder builder = new AlertDialog.Builder(context).setView(layout);
      final AlertDialog dialog = builder.create();
      TextView tvMessage = layout.findViewById(R.id.tvMessage);
      tvMessage.setText(message);
      CheckBox cb = layout.findViewById(R.id.checkbox);
      cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {

         @Override
         public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            // Persist checked state
            context.getSharedPreferences("optionalMessagePreferences", Activity.MODE_PRIVATE).edit()
                  .putBoolean(optionalMessageId, isChecked).apply();
         }
      });

      layout.findViewById(R.id.btOk).setOnClickListener(new OnClickListener() {

         @Override
         public void onClick(View v) {
            if (onOkay != null){
               onOkay.run();
            }
            dialog.dismiss();
         }
      });
      dialog.show();
      return true;
   }

   /**
    * Chop a string into an array of strings no longer then the specified chop
    * length
    */
   public static String[] stringChopper(String string, int chopLength) {
      return Iterables.toArray(Splitter.fixedLength(chopLength).split(string), String.class);
   }

   public static String stringChopper(String string, int chopLength, String joiner) {
      String[] parts = Iterables.toArray(Splitter.fixedLength(chopLength).split(string), String.class);
      return Joiner.on(joiner).join(parts);
   }

   public static Double getFiatValue(long satoshis, Double oneBtcInFiat) {
      if (oneBtcInFiat == null) {
         return null;
      }
      return (double) satoshis * oneBtcInFiat / Constants.ONE_BTC_IN_SATOSHIS;
   }

   public static String getFiatValueAsString(long satoshis, Double oneBtcInFiat) {
      Double converted = getFiatValue(satoshis, oneBtcInFiat);
      if (converted == null) {
         return null;
      }
      return FIAT_FORMAT.format(converted);
   }

   private static SparseArray<DecimalFormat> formatCache = new SparseArray<>(2);

   public static String formatFiatValueAsString(BigDecimal fiat) {
      return FIAT_FORMAT.format(fiat);
   }

   public static String formatFiatWithUnit(CurrencyValue fiat, int fractionDigit) {
      DecimalFormat decimalFormat = (DecimalFormat) FIAT_FORMAT.clone();
      decimalFormat.setMaximumFractionDigits(fractionDigit);
      return decimalFormat.format(fiat.getValue()) + " " + fiat.getCurrency();
   }

   public static String formatFiatWithUnit(CurrencyValue fiat) {
      try {
         return FIAT_FORMAT.format(fiat.getValue()) + " " + fiat.getCurrency();
      } catch (Exception e) {
         Log.e(TAG, e.getMessage());
         return "???";
      }
   }

   public static void setClipboardString(String string, Context context) {
      try {
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setPrimaryClip(ClipData.newPlainText("Mycelium", string));
      } catch (NullPointerException ex) {
         MbwManager.getInstance(context).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(context, context.getString(R.string.unable_to_set_clipboard), Toast.LENGTH_LONG).show();
      }
   }

   public static String getClipboardString(Context context) {
      try {
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         if(clipboard.getPrimaryClip() == null || clipboard.getPrimaryClip().getItemCount() < 1) {
            return "";
         }
         CharSequence content = clipboard.getPrimaryClip().getItemAt(0).getText();
         if (content == null) {
            return "";
         }
         return content.toString();
      } catch (SecurityException ex) {
         //some devices reported java.lang.SecurityException: Permission Denial:
         // reading com.android.providers.media.MediaProvider uri content://media/external/file/6595
         // it appears as if we have a file in clipboard that the system is trying to read. we don't want to do that anyways, so lets ignore it.
         Toast.makeText(context, context.getString(R.string.unable_to_get_clipboard), Toast.LENGTH_LONG).show();
         return "";
      } catch (NullPointerException ex) {
         MbwManager.getInstance(context).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(context, context.getString(R.string.unable_to_get_clipboard), Toast.LENGTH_LONG).show();
         return "";
      }
   }

   public static void clearClipboardString(Activity activity) {
      try {
         ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
         // some phones have clipboard history, we override it all
         for (int i = 0; i < 100 ; i++) {
            clipboard.setPrimaryClip(ClipData.newPlainText("Mycelium " + i, "wiped " + i));
         }
      } catch (NullPointerException ex) {
         MbwManager.getInstance(activity).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(activity, activity.getString(R.string.unable_to_clear_clipboard), Toast.LENGTH_LONG).show();
      }
   }

   public static Optional<Address> addressFromString(String someString, NetworkParameters network) {
      if (someString == null) {
         return Optional.absent();
      }
      someString = someString.trim();
      if (someString.matches("[a-zA-Z0-9]*")) {
         // Raw format
         return Optional.fromNullable(Address.fromString(someString, network));
      } else {
         Optional<BitcoinUriWithAddress> b = BitcoinUriWithAddress.parseWithAddress(someString, network);
         if (b.isPresent()) {
            // On URI format
            return Optional.of(b.get().address);
         }
      }
      return Optional.absent();
   }

   /**
    * Truncate and transform a decimal string to a maximum number of digits
    * <p>
    * The string will be truncated and verified to be a valid decimal number
    * with one comma or dot separator. A comma separator will be converted to a
    * dot. The resulting string will have at most the number of decimals
    * specified
    *
    * @param number           the number to truncate
    * @param maxDecimalPlaces the maximum number of decimal places
    * @return a truncated decimal string or null if the input string is not a
    * valid decimal string
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
      for (char c : chars) {
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
      return !((foundDot || foundComma) && digitsAfter == 0);

   }

   public static void pinProtectedWordlistBackup(final Activity activity) {
      MbwManager manager = MbwManager.getInstance(activity);
      manager.runPinProtectedFunction(activity, new Runnable() {

         @Override
         public void run() {
            Utils.wordlistBackup(activity);
         }
      });
   }

   private static void wordlistBackup(final Activity parent) {
      MbwManager _mbwManager = MbwManager.getInstance(parent);
      if (_mbwManager.getMetadataStorage().firstMasterseedBackupFinished()) {
         // second+ backup
         AdditionalBackupWarningActivity.callMe(parent);
      } else {
         // first backup
         new AlertDialog.Builder(parent)
                 .setMessage(R.string.backup_all_warning)
                 .setCancelable(true)
                 .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                       dialog.dismiss();
                       BackupWordListActivity.callMe(parent);
                    }
                 })
                 .setNegativeButton(R.string.no, null)
                 .create()
                 .show();
      }
   }

   public static boolean isAppInstalled(Context context, String uri) {
      PackageManager pm = context.getPackageManager();
      boolean installed;
      try {
         pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
         installed = true;
      } catch (PackageManager.NameNotFoundException e) {
         installed = false;
      }
      return installed;
   }

   private static boolean haveBackup(MbwManager mbwManager) {
      WalletAccount account = mbwManager.getSelectedAccount();
      MetadataStorage meta = mbwManager.getMetadataStorage();

      // Then check if there are some SingleAddressAccounts with funds on it
      if ((account instanceof ColuAccount || account instanceof SingleAddressAccount) && account.canSpend()) {
         MetadataStorage.BackupState state = meta.getOtherAccountBackupState(account.getId());
         return state == MetadataStorage.BackupState.NOT_VERIFIED || state == MetadataStorage.BackupState.VERIFIED;
      }
      return false;
   }

   public static void pinProtectedBackup(final Activity activity) {
      final MbwManager manager = MbwManager.getInstance(activity);
      manager.runPinProtectedFunction(activity, new Runnable() {
         @Override
         public void run() {
             Utils.backup(activity);
         }
      });
   }

   private static void backup(final Activity parent) {
      final MbwManager manager = MbwManager.getInstance(parent);
      new AlertDialog.Builder(parent)
              .setMessage(R.string.backup_legacy_warning).setCancelable(true)
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                    if (haveBackup(manager)) {
                       secondaryBackup(parent);
                    } else {
                       BackupToPdfActivity.callMe(parent);
                    }
                 }
              })
              .setNegativeButton(R.string.no, null)
              .create()
              .show();
   }

   private static void secondaryBackup(final Activity parent) {
      new AlertDialog.Builder(parent)
              .setMessage(R.string.did_backup).setCancelable(true)
              .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                 public void onClick(DialogInterface dialog, int id) {
                    BackupToPdfActivity.callMe(parent);
                 }
              })
              .setNegativeButton(R.string.cancel, null)
              .create()
              .show();
   }

   public static void exportSelectedAccount(final Activity parent) {
      final WalletAccount account = MbwManager.getInstance(parent).getSelectedAccount();
      if (!(account instanceof ExportableAccount)) {
         return;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.export_account_data_warning).setCancelable(true)
            .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
               public void onClick(DialogInterface dialog, int id) {
                  dialog.dismiss();
                  ExportAsQrActivity.callMe(parent, ((ExportableAccount) account).getExportData(AesKeyCipher.defaultKeyCipher()),
                          account);

               }
            }).setNegativeButton(R.string.no, null);
      AlertDialog alertDialog = builder.create();
      alertDialog.show();
   }

   /**
    * Prevent the OS from taking screenshots for the specified activity
    */
   public static void preventScreenshots(Activity activity) {
      activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
   }

   public static boolean checkIsLinked(WalletAccount account, final Collection<WalletAccount> accounts) {
      for (WalletAccount walletAccount : accounts) {
         if (walletAccount instanceof ColuAccount
                 && ((ColuAccount) walletAccount).getLinkedAccount() != null
                 && ((ColuAccount) walletAccount).getLinkedAccount().equals(account)) {
            return true;
         }
      }
      return false;
   }

   public static WalletAccount getLinkedAccount(WalletAccount account, final Collection<WalletAccount> accounts) {
      for (WalletAccount walletAccount : accounts) {
         if (walletAccount instanceof ColuAccount
                 && ((ColuAccount) walletAccount).getLinkedAccount() != null
                 && ((ColuAccount) walletAccount).getLinkedAccount().equals(account)) {
            return walletAccount;
         }
      }
      return null;
   }

   public static Collection<WalletAccount> getUniqueAccounts(final Collection<WalletAccount> accounts) {
      return new HashSet<>(accounts);
   }

   public static List<WalletAccount> sortAccounts(final Collection<WalletAccount> accounts, final MetadataStorage storage) {
      Ordering<WalletAccount> type = Ordering.natural().onResultOf(new Function<WalletAccount, Integer>() {
         @Override
         public Integer apply(@Nullable WalletAccount input) {
            switch (input.getType()) {
               case BTCBIP44:
               case BCHBIP44:
                  return 0;
               case BTCSINGLEADDRESS:
               case BCHSINGLEADDRESS:
                  return checkIsLinked(input, accounts) ? 5 : 1;
               case COLU:
                  return 5;
               case COINAPULT:
                  return 6; //coinapult last
               default:
                  return 4;
            }
         }
      });
      Ordering<WalletAccount> index = Ordering.natural().onResultOf(new Function<WalletAccount, Integer>() {
         @Nullable
         @Override
         public Integer apply(@Nullable WalletAccount input) {
            if (input instanceof HDAccount) {
               HDAccount HDAccount = (HDAccount) input;
               return HDAccount.getAccountIndex();
            }
            return Integer.MAX_VALUE;
         }
      });

      Comparator<WalletAccount> linked = new Comparator<WalletAccount>() {
         @Override
         public int compare(WalletAccount w1, WalletAccount w2) {
            if (w1.getType() == WalletAccount.Type.COLU) {
               return ((ColuAccount) w1).getLinkedAccount().getId().equals(w2.getId()) ? -1 : 0;
            } else if (w2.getType() == WalletAccount.Type.COLU) {
               return ((ColuAccount) w2).getLinkedAccount().getId().equals(w1.getId()) ? 1 : 0;
            } else if (w1.getType() == WalletAccount.Type.BCHBIP44
                    && w2.getType() == WalletAccount.Type.BTCBIP44
                    && MbwManager.getBitcoinCashAccountId(w2).equals(w1.getId())) {
               return 1;
            } else if (w1.getType() == WalletAccount.Type.BTCBIP44
                    && w2.getType() == WalletAccount.Type.BCHBIP44
                    && MbwManager.getBitcoinCashAccountId(w1).equals(w2.getId())) {
               return -1;
            } else if (w1.getType() == WalletAccount.Type.BCHSINGLEADDRESS
                    && w2.getType() == WalletAccount.Type.BTCSINGLEADDRESS
                    && MbwManager.getBitcoinCashAccountId(w2).equals(w1.getId())) {
               return 1;
            } else if (w1.getType() == WalletAccount.Type.BTCSINGLEADDRESS
                    && w2.getType() == WalletAccount.Type.BCHSINGLEADDRESS
                    && MbwManager.getBitcoinCashAccountId(w1).equals(w2.getId())) {
               return -1;
            } else {
               return 0;
            }
         }
      };

      Ordering<WalletAccount> name = Ordering.natural().onResultOf(new Function<WalletAccount, String>() {
         @Nullable
         @Override
         public String apply(@Nullable WalletAccount input) {
            return storage.getLabelByAccount(input.getId());
         }
      });
      return type.compound(index).compound(linked).compound(name).sortedCopy(accounts);
   }

   public static List<Address> sortAddresses(List<Address> addresses) {
      return Ordering.usingToString().sortedCopy(addresses);
   }

   public static List<AddressBookManager.Entry> sortAddressbookEntries(List<AddressBookManager.Entry> entries) {
      return Ordering.natural().onResultOf(ENTRY_NAME).sortedCopy(entries);
   }

   public static Drawable getDrawableForAccount(WalletAccount walletAccount, boolean isSelectedAccount, Resources resources) {
      if (walletAccount instanceof ColuAccount) {
         ColuAccount account = (ColuAccount) walletAccount;
         switch (account.getColuAsset().assetType) {
            case MT:
               return account.canSpend() ? resources.getDrawable(R.drawable.mt_icon) :
                       resources.getDrawable(R.drawable.mt_icon_no_priv_key);
            case MASS:
               return account.canSpend() ? resources.getDrawable(R.drawable.mass_icon)
                       : resources.getDrawable(R.drawable.mass_icon_no_priv_key);
            case RMC:
               return account.canSpend() ? resources.getDrawable(R.drawable.rmc_icon)
                       : resources.getDrawable(R.drawable.rmc_icon_no_priv_key);
         }
      }

      // Watch only
      if (!walletAccount.canSpend()) {
         return null;
      }

      //trezor account
      if (walletAccount instanceof HDAccountExternalSignature) {
         int accountType = ((HDAccountExternalSignature) walletAccount).getAccountType();
         if (accountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER) {
            return resources.getDrawable(R.drawable.ledger_icon);
		 } else if (accountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY) {
            return resources.getDrawable(R.drawable.keepkey_icon);
         } else {
            return resources.getDrawable(R.drawable.trezor_icon_only);
         }

      }
      //regular HD account
      if (walletAccount instanceof HDAccount) {
         return resources.getDrawable(R.drawable.multikeys_grey);
      }
      if (walletAccount instanceof CoinapultAccount) {
         if (isSelectedAccount) {
            return resources.getDrawable(R.drawable.coinapult);
         } else {
            return resources.getDrawable(R.drawable.coinapultgrey);
         }
      }

      //single key account
      return resources.getDrawable(R.drawable.singlekey_grey);
   }

   public static String getNameForNewAccount(WalletAccount account, Context context) {
      if (account instanceof HDAccountExternalSignature) {
         String baseName;
         int accountType = ((HDAccountExternalSignature) account).getAccountType();
         if (accountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER) {
            baseName = MbwManager.getInstance(context).getLedgerManager().getLabelOrDefault();
		 } else if (accountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY) {
            baseName = MbwManager.getInstance(context).getKeepKeyManager().getLabelOrDefault();
         } else {
            baseName = MbwManager.getInstance(context).getTrezorManager().getLabelOrDefault();
         }
         return baseName + " #" + (((HDAccountExternalSignature) account).getAccountIndex() + 1);
      } else if (account instanceof HDPubOnlyAccount) {
         return context.getString(R.string.account_prefix_imported);
      } else if (account instanceof HDAccount) {
         return context.getString(R.string.account) + " " + (((HDAccount) account).getAccountIndex() + 1);
      } else {
         return DateFormat.getMediumDateFormat(context).format(new Date());
      }
   }

   public static boolean isAllowedForLocalTrader(WalletAccount account) {
      if (account instanceof CoinapultAccount
              || account.getType() == WalletAccount.Type.BCHBIP44
              || account.getType() == WalletAccount.Type.BCHSINGLEADDRESS
              || account.getType() == WalletAccount.Type.COLU) {
         return false; //we do not support coinapult accs in lt (yet)
      }
      if (!account.getReceivingAddress().isPresent()) {
         return false;  // the account has no valid receiving address (should not happen) - dont use it
      }
      if (account instanceof AbstractAccount) {
         if (!((AbstractAccount) account).getAvailableAddressTypes().contains(AddressType.P2PKH)) {
            return false;
         }
      }
      return true; //all other account types including trezor accs are fine
   }

   public static String getFormattedDate(Context context, Date date) {
      Locale locale = context.getResources().getConfiguration().locale;
      java.text.DateFormat format;
      Calendar now = Calendar.getInstance(locale);
      Calendar toFormat = Calendar.getInstance(locale);
      toFormat.setTime(date);
      // show the date part if it is not today
      if (toFormat.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
            toFormat.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)) {
         format = java.text.DateFormat.getTimeInstance(java.text.DateFormat.MEDIUM, locale);
      } else {
         format = java.text.DateFormat.getDateTimeInstance(java.text.DateFormat.SHORT, java.text.DateFormat.MEDIUM, locale);
      }
      return format.format(date);
   }

   public static String getFormattedValue(CurrencyValue value, CoinUtil.Denomination denomination) {
      if (value == null) {
         return "";
      }

      BigDecimal val = value.getValue();
      if (val == null) {
         return "";
      }
      if (value.isBtc() || value.isBch()) {
         return CoinUtil.valueString(val, denomination, false);
      } else {

         return FIAT_FORMAT.format(val);
      }
   }

   public static String getFormattedValue(CurrencyValue value, CoinUtil.Denomination denomination, int precision) {
      if (value == null) {
         return "";
      }

      BigDecimal val = value.getValue();
      if (val == null) {
         return "";
      }
      if (value.isBtc()) {
         return CoinUtil.valueString(
               ((BitcoinValue) value).getLongValue(),
               denomination, precision
         );
      } else {
         if (formatCache.get(precision) == null) {
            DecimalFormat fiatFormat = (DecimalFormat) FIAT_FORMAT.clone();
            fiatFormat.setMaximumFractionDigits(precision);
            formatCache.put(precision, fiatFormat);
         }
         return formatCache.get(precision).format(val);
      }
   }

   public static String getFormattedValueWithUnit(CurrencyValue value, CoinUtil.Denomination denomination) {
      if (value == null) {
         return "";
      }

      if (value.isBtc()) {
         return getFormattedValueWithUnit((BitcoinValue) value, denomination);
      } else if(value.isBch()) {
        return getFormattedValueWithUnit(ExactBitcoinCashValue.from(value.getValue()), denomination);
      } else {
         BigDecimal val = value.getValue();
         if (val == null) {
            return "";
         }
         return String.format("%s %s", FIAT_FORMAT.format(val), value.getCurrency());
      }
   }

   public static String getColuFormattedValueWithUnit(CurrencyValue value) {
      return String.format("%s %s", value.getValue().stripTrailingZeros().toPlainString(), value.getCurrency());
   }

   public static String getColuFormattedValue(CurrencyValue value) {
      return value.getValue().stripTrailingZeros().toPlainString();
   }

   // prevent ambiguous call for ExactBitcoinValue
   public static String getFormattedValueWithUnit(ExactBitcoinValue value, CoinUtil.Denomination denomination) {
      return getFormattedValueWithUnit((BitcoinValue)value, denomination);
   }

   public static String getFormattedValueWithUnit(BitcoinValue value, CoinUtil.Denomination denomination) {
      BigDecimal val = value.getValue();
      if (val == null) {
         return "";
      }
      return String.format("%s %s", CoinUtil.valueString(val, denomination, false), denomination.getUnicodeName());
   }

   public static String getFormattedValueWithUnit(ExactBitcoinCashValue value, CoinUtil.Denomination denomination) {
      BigDecimal val = value.getValue();
      if (val == null) {
         return "";
      }
      return String.format("%s %s", CoinUtil.valueString(val, denomination, false), denomination.getUnicodeName().replace("BTC", "BCH"));
   }


      public static String getFormattedValueWithUnit(CurrencyValue value, CoinUtil.Denomination denomination, int precision) {
      if (value == null) {
         return "";
      }

      BigDecimal val = value.getValue();
      if (val == null) {
         return "";
      }

      if (value.isBtc()) {
         return String.format("%s %s", CoinUtil.valueString(((BitcoinValue) value).getLongValue(),
                     denomination, precision), denomination.getUnicodeName()
         );
      } else {
         if (formatCache.get(precision) == null) {
            DecimalFormat fiatFormat = (DecimalFormat) FIAT_FORMAT.clone();
            fiatFormat.setMaximumFractionDigits(precision);
            formatCache.put(precision, fiatFormat);
         }
         return String.format("%s %s", formatCache.get(precision).format(val), value.getCurrency());
      }
   }

   public static boolean isValidEmailAddress(String value) {
      return android.util.Patterns.EMAIL_ADDRESS.matcher(value).matches();
   }

   public static boolean openWebsite(Context context, String uri) {
      Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
      if (browserIntent.resolveActivity(context.getPackageManager()) != null) {
         context.startActivity(browserIntent);
         return true;
      } else {
         return false;
      }
   }

   public static final int REQUEST_CAMERA = 9465169;

   /**
    * Request camera access if not already granted
    *
    * @return true if write permission was already been granted
    */
   public static boolean hasOrRequestCameraAccess(Activity activity) {
      return hasOrRequestAccess(activity, Manifest.permission.CAMERA, REQUEST_CAMERA);
   }

   public static final int REQUEST_LOCATION = 9465199;

   /**
    * Request camera access if not already granted
    *
    * @return true if write permission was already been granted
    */
   public static boolean hasOrRequestLocationAccess(Activity activity) {
      return hasOrRequestAccess(activity, Manifest.permission.ACCESS_COARSE_LOCATION, REQUEST_LOCATION);
   }

   /**
    * Request permission if not already granted
    *
    * @return true if write permission was already been granted
    */
   public static boolean hasOrRequestAccess(Activity activity, String permission, int requestCode) {
      boolean hasPermission = (ContextCompat.checkSelfPermission(activity, permission)
              == PackageManager.PERMISSION_GRANTED);
      if (!hasPermission) {
         ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
      }
      return hasPermission;
   }
}
