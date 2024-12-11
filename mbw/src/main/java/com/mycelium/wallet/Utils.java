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
import android.text.format.DateFormat;
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

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
import com.mrd.bitlib.model.AddressType;
import com.mrd.bitlib.model.BitcoinAddress;
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.activity.AdditionalBackupWarningActivity;
import com.mycelium.wallet.activity.BackupWordListActivity;
import com.mycelium.wallet.activity.export.BackupToPdfActivity;
import com.mycelium.wallet.activity.export.ExportAsQrActivity;
import com.mycelium.wallet.activity.modern.Toaster;
import com.mycelium.wallet.activity.modern.model.accounts.AccountViewModel;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.api.lib.CurrencyCode;
import com.mycelium.wapi.content.AssetUri;
import com.mycelium.wapi.content.btc.BitcoinUriParser;
import com.mycelium.wapi.wallet.Address;
import com.mycelium.wapi.wallet.AddressUtils;
import com.mycelium.wapi.wallet.AesKeyCipher;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.WalletManager;
import com.mycelium.wapi.wallet.bch.bip44.Bip44BCHAccount;
import com.mycelium.wapi.wallet.bch.single.SingleAddressBCHAccount;
import com.mycelium.wapi.wallet.btc.AbstractBtcAccount;
import com.mycelium.wapi.wallet.btc.WalletBtcAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccount;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountContext;
import com.mycelium.wapi.wallet.btc.bip44.HDAccountExternalSignature;
import com.mycelium.wapi.wallet.btc.bip44.HDPubOnlyAccount;
import com.mycelium.wapi.wallet.btc.coins.BitcoinMain;
import com.mycelium.wapi.wallet.btc.coins.BitcoinTest;
import com.mycelium.wapi.wallet.btc.single.SingleAddressAccount;
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultMain;
import com.mycelium.wapi.wallet.btcvault.coins.BitcoinVaultTest;
import com.mycelium.wapi.wallet.btcvault.hd.BitcoinVaultHdAccount;
import com.mycelium.wapi.wallet.coins.AssetInfo;
import com.mycelium.wapi.wallet.coins.CoinsKt;
import com.mycelium.wapi.wallet.coins.CryptoCurrency;
import com.mycelium.wapi.wallet.colu.ColuAccount;
import com.mycelium.wapi.wallet.colu.coins.MASSCoin;
import com.mycelium.wapi.wallet.colu.coins.MASSCoinTest;
import com.mycelium.wapi.wallet.colu.coins.MTCoin;
import com.mycelium.wapi.wallet.colu.coins.MTCoinTest;
import com.mycelium.wapi.wallet.colu.coins.RMCCoin;
import com.mycelium.wapi.wallet.colu.coins.RMCCoinTest;
import com.mycelium.wapi.wallet.erc20.ERC20Account;
import com.mycelium.wapi.wallet.erc20.ERC20ModuleKt;
import com.mycelium.wapi.wallet.eth.AbstractEthERC20Account;
import com.mycelium.wapi.wallet.eth.EthAccount;
import com.mycelium.wapi.wallet.eth.coins.EthMain;
import com.mycelium.wapi.wallet.eth.coins.EthTest;
import com.mycelium.wapi.wallet.fiat.coins.FiatType;
import com.mycelium.wapi.wallet.fio.FioAccount;
import com.mycelium.wapi.wallet.fio.coins.FIOMain;
import com.mycelium.wapi.wallet.fio.coins.FIOTest;
import com.mycelium.wapi.wallet.fio.coins.FIOToken;

import org.ocpsoft.prettytime.Duration;
import org.ocpsoft.prettytime.PrettyTime;
import org.ocpsoft.prettytime.TimeUnit;
import org.ocpsoft.prettytime.units.Minute;
import org.ocpsoft.prettytime.units.Second;

import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;


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
      return isConnected(context, "");
   }

   public static boolean isConnected(Context context, String where) {
      ConnectivityManager cm =
              (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

      NetworkInfo activeNetwork = null;
      if (cm != null) {
         activeNetwork = cm.getActiveNetworkInfo();
      }
      if (!where.isEmpty()) {
         Logger.getLogger(Utils.class.getSimpleName()).log(Level.INFO, "Network state on '" + where + "' event: " + activeNetwork);
      }
      return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
   }

   public static void toastConnectionError(Context context) {
      new Toaster(context).toastConnectionError();
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
   public static String formatBlockcountAsApproxDuration(MbwManager mbwManager, final int blocks, final int blockTimeInSeconds) {
      PrettyTime p = new PrettyTime(mbwManager.getLocale());
      long confirmationTime = Math.max((long) blocks, 1L) * blockTimeInSeconds * 1000;
      Date ref = new Date();
      Date then = new Date(ref.getTime() + confirmationTime);
      long absoluteDifference = Math.abs(then.getTime() - ref.getTime());
      Duration duration = p.approximateDuration(then);
      // for time differences less than 5 minutes (300000 millisecs) PrettyTime lib functionality
      // is not satisfactory for our purposes, so we are using custom duration otherwise
      if (absoluteDifference > 300000 && !mbwManager.getLocale().getLanguage().equals("ru")) {
         return p.formatDuration(duration);
      }
      Duration customDuration = new Duration() {
         @Override
         public long getQuantity() {
            if (absoluteDifference <= 300000) {
               return absoluteDifference / getUnit().getMillisPerUnit();
            }
            return duration.getQuantity();
         }

         @Override
         public long getQuantityRounded(int tolerance) {
            if (absoluteDifference <= 300000) {
               return getQuantity();
            }
            return duration.getQuantityRounded(tolerance);
         }

         @Override
         public TimeUnit getUnit() {
            if (absoluteDifference <= 300000) {
               if (absoluteDifference > 60000) {
                  return p.getUnit(Minute.class);
               } else {
                  return p.getUnit(Second.class);
               }
            }
            return duration.getUnit();
         }

         @Override
         public long getDelta() {
            if (absoluteDifference <= 300000) {
               return 0;
            }
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
      if (mbwManager.getLocale().getLanguage().equals("ru")) {
         return p.getFormat(customDuration.getUnit()).decorate(customDuration, p.formatDuration(customDuration));
      } else {
         return p.formatDuration(customDuration);
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

   public static void setClipboardString(String string, Context context) {
      try {
         ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setPrimaryClip(ClipData.newPlainText("Mycelium", string));
      } catch (NullPointerException ex) {
         MbwManager.getInstance(context).reportIgnoredException(new RuntimeException(ex.getMessage()));
         new Toaster(context).toast(R.string.unable_to_set_clipboard, false);
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
         new Toaster(context).toast(R.string.unable_to_get_clipboard, false);
         return "";
      } catch (NullPointerException ex) {
         MbwManager.getInstance(context).reportIgnoredException(new RuntimeException(ex.getMessage()));
         new Toaster(context).toast(R.string.unable_to_get_clipboard, false);
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
         new Toaster(activity).toast(R.string.unable_to_clear_clipboard, false);
      }
   }

   public static Optional<Address> addressFromString(String someString, NetworkParameters network) {
      if (someString == null) {
         return Optional.absent();
      }
      someString = someString.trim();
      if (someString.matches("[a-zA-Z0-9]*")) {
         // Raw format
         return Optional.fromNullable(AddressUtils.from(getBtcCoinType(), someString));
      } else {
         AssetUri b = (new BitcoinUriParser(network)).parse(someString);
         if (b != null && b.getAddress() != null) {
            // On URI format
            return Optional.of(b.getAddress());
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
            .setPositiveButton(R.string.yes, (dialog, id) -> {
               dialog.dismiss();
               account.interruptSync();
               ExportAsQrActivity.callMe(parent, ((ExportableAccount) account).getExportData(AesKeyCipher.defaultKeyCipher()),
                       account);

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

   public static boolean checkIsLinked(WalletAccount account, final Collection<? extends WalletAccount> accounts) {
      return getLinkedAccount(account, accounts) != null;
   }

   public static boolean isERC20Token(WalletManager walletManager, String symbol) {
      for (WalletAccount account : ERC20ModuleKt.getERC20Accounts(walletManager)) {
         if (account.getCoinType().getSymbol().equals(symbol)) {
            return true;
         }
      }
      return false;
   }

   public static WalletAccount getLinkedAccount(WalletAccount account, final Collection<? extends WalletAccount> accounts) {
      for (WalletAccount walletAccount : accounts) {
         if (!walletAccount.getId().equals(account.getId()) && account.isMineAddress(walletAccount.getReceiveAddress())) {
            return walletAccount;
         }
      }
      return null;
   }

   public static EthAccount getLinkedEthAccount(WalletAccount account, final Collection<? extends WalletAccount> accounts) {
      for (WalletAccount walletAccount : accounts) {
         if (walletAccount instanceof EthAccount && !walletAccount.getId().equals(account.getId()) && account.isMineAddress(walletAccount.getReceiveAddress())) {
            return (EthAccount) walletAccount;
         }
      }
      return null;
   }

   public static List<WalletAccount> getLinkedAccounts(WalletAccount account, final Collection<? extends WalletAccount> accounts) {
      List<WalletAccount> result = new ArrayList<>();
      for (WalletAccount walletAccount : accounts) {
         if (!walletAccount.getId().equals(account.getId()) && account.isMineAddress(walletAccount.getReceiveAddress())) {
            result.add(walletAccount);
         }
      }
      return result;
   }

   public static Collection<WalletAccount> getUniqueAccounts(final Collection<WalletAccount> accounts) {
      return new HashSet<>(accounts);
   }

   public static List<WalletAccount<?>> sortAccounts(final Collection<WalletAccount<?>> accounts, final MetadataStorage storage) {
      Ordering<WalletAccount> type = Ordering.natural().onResultOf(new Function<WalletAccount, Integer>() {
         //maybe need to add new method in WalletAccount and use polymorphism
         //but I think it's unnecessary
         @Override
         public Integer apply(@Nullable WalletAccount input) {
            // the intended ordering is:
            // HDAccount
            // SingleAddressAccount non-linked (BTC and BCH)
            // "anything else"????
            // PrivateColuAccount and their linked SingleAddressAccount
            // PublicColuAccount (never has anything linked)
            // EthAccount and ERC20
            if (input instanceof HDAccount) { // also covers Bip44BCHAccount
               return 0;
            }
            if (input instanceof SingleAddressAccount) { // also covers SingleAddressBCHAccount
               return checkIsLinked(input, accounts) ? 5 : 1;
            }
            if (input instanceof ColuAccount) {
               return 5;
            }
            if (input instanceof EthAccount || input instanceof ERC20Account) {
               return 6;
            }
            return 4;
         }
      });
      Ordering<WalletAccount> index = Ordering.natural().onResultOf(new Function<WalletAccount, Integer>() {
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
            if (w1 instanceof ColuAccount) {
               WalletAccount linkedAccount = getLinkedAccount(w1, accounts);
               if (linkedAccount == null) {
                  return 0;
               }
               return linkedAccount.getId().equals(w2.getId()) ? -1 : 0;
            } else if (w2 instanceof ColuAccount) {
               WalletAccount linkedAccount = getLinkedAccount(w2, accounts);
               if (linkedAccount == null) {
                  return 0;
               }
               return linkedAccount.getId().equals(w1.getId()) ? 1 : 0;
            } else if (w1 instanceof EthAccount && w2 instanceof EthAccount) {
               return Integer.compare(((EthAccount) w1).getAccountIndex(), ((EthAccount) w2).getAccountIndex());
            } else if (w1 instanceof EthAccount && w2 instanceof ERC20Account) {
               EthAccount linkedEthAccount = getLinkedEthAccount(w2, accounts);
               if (linkedEthAccount.equals(w1)) {
                  return -1;
               } else {
                  return Integer.compare(((EthAccount) w1).getAccountIndex(), linkedEthAccount.getAccountIndex());
               }
            } else if (w1 instanceof ERC20Account && w2 instanceof EthAccount) {
               EthAccount linkedEthAccount = getLinkedEthAccount(w1, accounts);
               if (linkedEthAccount.equals(w2)) {
                  return 1;
               } else {
                  return Integer.compare(linkedEthAccount.getAccountIndex(), ((EthAccount) w2).getAccountIndex());
               }
            } else if (w1 instanceof ERC20Account && w2 instanceof ERC20Account) {
               EthAccount linkedEthAccount1 = getLinkedEthAccount(w1, accounts);
               EthAccount linkedEthAccount2 = getLinkedEthAccount(w2, accounts);
               return Integer.compare(linkedEthAccount1.getAccountIndex(), linkedEthAccount2.getAccountIndex());
            }
            return 0;
         }
      };

      Ordering<WalletAccount> name = Ordering.natural().onResultOf(new Function<WalletAccount, String>() {
         @Nullable
         @Override
         public String apply(@Nullable WalletAccount input) {
            return input != null ? storage.getLabelByAccount(input.getId()) : "";
         }
      });
      return type.compound(index).compound(linked).compound(name).sortedCopy(accounts);
   }

   public static List<BitcoinAddress> sortAddresses(List<BitcoinAddress> addresses) {
      return Ordering.usingToString().sortedCopy(addresses);
   }

   public static List<AddressBookManager.Entry> sortAddressbookEntries(List<AddressBookManager.Entry> entries) {
      return Ordering.natural().onResultOf(ENTRY_NAME).sortedCopy(entries);
   }

   public static Drawable getDrawableForAccount(WalletAccount walletAccount, boolean isSelectedAccount, Resources resources) {
      return getDrawableForAccount(new AccountViewModel(walletAccount, null), isSelectedAccount, resources);
   }

   public static Drawable getDrawableForAccount(AccountViewModel accountView, boolean isSelectedAccount, Resources resources) {
      Class<? extends WalletAccount<? extends Address>> accountType = accountView.getAccountType();
      if (ColuAccount.class.isAssignableFrom(accountType)) {
         CryptoCurrency coinType = accountView.getCoinType();
         if (coinType == MTCoin.INSTANCE || coinType == MTCoinTest.INSTANCE) {
            return accountView.getCanSpend() ? resources.getDrawable(R.drawable.mt_icon) :
                    resources.getDrawable(R.drawable.mt_icon_no_priv_key);
         } else if (coinType == MASSCoin.INSTANCE || coinType == MASSCoinTest.INSTANCE) {
            return accountView.getCanSpend() ? resources.getDrawable(R.drawable.mass_icon)
                    : resources.getDrawable(R.drawable.mass_icon_no_priv_key);
         } else if (coinType == RMCCoin.INSTANCE || coinType == RMCCoinTest.INSTANCE)
            return accountView.getCanSpend() ? resources.getDrawable(R.drawable.rmc_icon)
                    : resources.getDrawable(R.drawable.rmc_icon_no_priv_key);
      }

      // Watch only
      if (!accountView.getCanSpend()) {
         return null;
      }

      //trezor account
      if (HDAccountExternalSignature.class.isAssignableFrom(accountType)) {
         int externalAccountType = accountView.getExternalAccountType();
         if (externalAccountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_LEDGER) {
            return resources.getDrawable(R.drawable.ledger_icon);
         } else if (externalAccountType == HDAccountContext.ACCOUNT_TYPE_UNRELATED_X_PUB_EXTERNAL_SIG_KEEPKEY) {
            return resources.getDrawable(R.drawable.keepkey_icon);
         } else {
            return resources.getDrawable(R.drawable.trezor_icon_only);
         }
      }

      if (ERC20Account.class.isAssignableFrom(accountType)) {
         Drawable drawable = null;
         String symbol = accountView.getCoinType().getSymbol();
         try {
            // get input stream
            InputStream ims = resources.getAssets().open("token-logos/" + symbol.toLowerCase() + "_logo.png");
            // load image as Drawable
            drawable = Drawable.createFromStream(ims, null);
         } catch (IOException e) {
            e.printStackTrace();
         }
         return drawable;
      }

      //regular HD account
      if (HDAccount.class.isAssignableFrom(accountType)) {
         return resources.getDrawable(R.drawable.multikeys_grey);
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
      if (account instanceof Bip44BCHAccount
              || account instanceof SingleAddressBCHAccount
              || account instanceof ColuAccount
              || account instanceof AbstractEthERC20Account
              || account instanceof FioAccount
              || account instanceof BitcoinVaultHdAccount) {
         return false; //we do not support these account types in LT
      }
      if (!((WalletBtcAccount)(account)).getReceivingAddress().isPresent()) {
         return false;  // the account has no valid receiving address (should not happen) - dont use it
      }
      if (account instanceof AbstractBtcAccount) {
         if (!((AbstractBtcAccount) account).getAvailableAddressTypes().contains(AddressType.P2PKH)) {
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

   public static CryptoCurrency getBtcCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? BitcoinMain.get() : BitcoinTest.get();
   }

   public static CryptoCurrency getBtcvCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? BitcoinVaultMain.INSTANCE : BitcoinVaultTest.INSTANCE;
   }

   public static CryptoCurrency getEthCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? EthMain.INSTANCE : EthTest.INSTANCE;
   }

   public static CryptoCurrency getMtCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? MTCoin.INSTANCE : MTCoinTest.INSTANCE;
   }

   public static CryptoCurrency getMassCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? MASSCoin.INSTANCE : MASSCoinTest.INSTANCE;
   }

   public static CryptoCurrency getRmcCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? RMCCoin.INSTANCE : RMCCoinTest.INSTANCE;
   }

   public static FIOToken getFIOCoinType() {
      return BuildConfig.FLAVOR.equals("prodnet") ? FIOMain.INSTANCE : FIOTest.INSTANCE;
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

   public static boolean hasOrRequestAccess(androidx.fragment.app.Fragment fragment, String permission, int requestCode){
      boolean hasPermission = (ContextCompat.checkSelfPermission(fragment.getActivity(), permission)
              == PackageManager.PERMISSION_GRANTED);
      if (!hasPermission) {
         fragment.requestPermissions(new String[]{permission}, requestCode);
      }
      return hasPermission;
   }

   public static AssetInfo getTypeByName(String name) {
      for (CurrencyCode currencyCode : CurrencyCode.values()) {
         if (name.equals(currencyCode.getShortString())) {
            // then it's a fiat type
            return new FiatType(name);
         }
      }
      for (CryptoCurrency coin : CoinsKt.getCOINS().values()) {
         if (coin.getName().equals(name) || coin.getSymbol().equals(name)) {
            return coin;
         }
      }
      Logger.getLogger(Utils.class.getSimpleName()).log(Level.SEVERE, "Unknown currency type '" + name + "'");
      // Never set to null. The currentCurrencyMap assumes non-null keys,
      // which can lead to an exception in CurrencySwitcher.setCurrency
      CryptoCurrency copyPropsFromType = getBtcCoinType();
      return new CryptoCurrency("Unknown_" + name, name, "?",
              copyPropsFromType.getUnitExponent(),
              copyPropsFromType.getFriendlyDigits(),
              copyPropsFromType.isUtxosBased());
   }
}
