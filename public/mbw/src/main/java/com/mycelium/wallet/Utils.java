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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.text.ClipboardManager;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
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
import com.mrd.bitlib.model.NetworkParameters;
import com.mycelium.wallet.activity.BackupWordListActivity;
import com.mycelium.wallet.activity.AdditionalBackupWarningActivity;
import com.mycelium.wallet.activity.export.BackupToPdfActivity;
import com.mycelium.wallet.activity.export.ExportAsQrCodeActivity;
import com.mycelium.wallet.persistence.MetadataStorage;
import com.mycelium.wapi.wallet.ExportableAccount;
import com.mycelium.wapi.wallet.WalletAccount;
import com.mycelium.wapi.wallet.bip44.Bip44Account;
import com.mycelium.wapi.wallet.bip44.Bip44AccountExternalSignature;
import com.mycelium.wapi.wallet.bip44.Bip44PubOnlyAccount;
import com.mycelium.wapi.wallet.single.SingleAddressAccount;
import org.ocpsoft.prettytime.PrettyTime;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.*;

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

   public static final Function<AddressBookManager.Entry, Comparable> ENTRY_NAME = new Function<AddressBookManager.Entry, Comparable>() {
      @Override
      public Comparable apply(AddressBookManager.Entry input) {
         return input.getName();
      }
   };

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

   public static boolean isConnected(Context context) {
      ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo[] NI = cm.getAllNetworkInfo();
      for (NetworkInfo aNI : NI) {
         if (aNI.isConnected()) {
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
      showSimpleMessageDialog(context, message, postRunner);
   }

   public static String formatBlockcountAsApproxDuration(final Context context, final int blocks){
      MbwManager mbwManager = MbwManager.getInstance(context);
      PrettyTime p = new PrettyTime(mbwManager.getLocale());
      String ret = p.formatApproximateDuration(new Date((new Date()).getTime() + Math.max(blocks, 1) * 10 * 60 * 1000));
      return ret;
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
            if (okayRunner != null) {
               okayRunner.run();
            }
         }
      });

      dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
         @Override
         public void onDismiss(DialogInterface dialog) {
            if (postRunner != null) {
               postRunner.run();
            }
         }
      });
      dialog.show();
   }

   /**
    * Show an optional message/
    * <p/>
    * The user can check a "never shot this again" check box and the message
    * will never get displayed again.
    *
    * @param context           The context
    * @param messageResourceId The resource ID of the message to show
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

   private static HashMap<Integer, DecimalFormat> formatCache = new HashMap<Integer, DecimalFormat>(2);

   public static String formatFiatValueAsString(BigDecimal fiat) {
      return FIAT_FORMAT.format(fiat);
   }
   public static String getFiatValueAsString(long satoshis, Double oneBtcInFiat, int precision) {

      Double converted = getFiatValue(satoshis, oneBtcInFiat);
      if (converted == null) {
         return null;
      }

      if (!formatCache.containsKey(precision)){
         DecimalFormat fiatFormat = (DecimalFormat) FIAT_FORMAT.clone();
         fiatFormat.setMaximumFractionDigits(precision);
         formatCache.put(precision, fiatFormat);
      }
      return formatCache.get(precision).format(converted);
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
      } catch (NullPointerException ex) {
         MbwManager.getInstance(context).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(context, context.getString(R.string.unable_to_set_clipboard), Toast.LENGTH_LONG).show();
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
      } catch (SecurityException ex) {
         //some devices reported java.lang.SecurityException: Permission Denial:
         // reading com.android.providers.media.MediaProvider uri content://media/external/file/6595
         //it appears as if we have a file in clipboard that the system is trying to read. we don't want to do that anyways, so lets ignore it.
         //we could also skip the reporting to server, but lets see if this works.
         MbwManager.getInstance(activity).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(activity,activity.getString(R.string.unable_to_get_clipboard), Toast.LENGTH_LONG).show();
         return "";
      } catch (NullPointerException ex) {
         MbwManager.getInstance(activity).reportIgnoredException(new RuntimeException(ex.getMessage()));
         Toast.makeText(activity,activity.getString(R.string.unable_to_get_clipboard), Toast.LENGTH_LONG).show();
         return "";
      }
   }

   public static void clearClipboardString(Activity activity) {
      try {
         ClipboardManager clipboard = (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
         clipboard.setText("");
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
    * <p/>
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
      }else{
         // first backup
         AlertDialog.Builder builder = new AlertDialog.Builder(parent);
         builder.setMessage(R.string.backup_all_warning).setCancelable(true)
               .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                  public void onClick(DialogInterface dialog, int id) {
                     dialog.dismiss();
                     BackupWordListActivity.callMe(parent);
                  }
               }).setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
            }
         });
         AlertDialog alertDialog = builder.create();
         alertDialog.show();
      }

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
      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.backup_legacy_warning).setCancelable(true)
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

   public static void exportSelectedAccount(final Activity parent) {
      WalletAccount account = MbwManager.getInstance(parent).getSelectedAccount();
      if (!(account instanceof ExportableAccount)) {
         return;
      }

      AlertDialog.Builder builder = new AlertDialog.Builder(parent);
      builder.setMessage(R.string.export_account_data_warning).setCancelable(true)
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

   public static List<WalletAccount> sortAccounts(List<WalletAccount> accounts, final MetadataStorage storage) {
      Comparator<WalletAccount> comparator = new Comparator<WalletAccount>() {
         @Override
         public int compare(WalletAccount lhs, WalletAccount rhs) {
            //HD Accounts always come first
            if (lhs instanceof Bip44Account && rhs instanceof SingleAddressAccount) return -1;
            if (lhs instanceof SingleAddressAccount && rhs instanceof Bip44Account) return 1;
            //when both are hd, we take the account index
            if (lhs instanceof Bip44Account) {
               int lid = ((Bip44Account) lhs).getAccountIndex();
               int rid = ((Bip44Account) rhs).getAccountIndex();
               if (lid < rid) {
                  return -1;
               } else if (lid > rid) {
                  return 1;
               } else {
                  return 0;
               }
            }
            //when both are single, we take label
            String lname = storage.getLabelByAccount(lhs.getId());
            String rname = storage.getLabelByAccount(rhs.getId());
            return lname.compareTo(rname);
         }
      };
      accounts = new ArrayList<WalletAccount>(accounts);
      Collections.sort(accounts, comparator);
      return accounts;
   }

   public static List<Address> sortAAddresses(List<Address> addresses) {
      return Ordering.usingToString().sortedCopy(addresses);
   }

   public static List<AddressBookManager.Entry> sortAddressbookEntries(List<AddressBookManager.Entry> entries) {
      return Ordering.natural().onResultOf(ENTRY_NAME).sortedCopy(entries);
   }

   public static Drawable getDrawableForAccount(WalletAccount walletAccount, Resources resources) {
      // Watch only
      if (!walletAccount.canSpend()){
         return null;
      }

      //trezor account
      if (walletAccount instanceof Bip44AccountExternalSignature) {
         return resources.getDrawable(R.drawable.trezor_icon_only);
      }
      //regular HD account
      if (walletAccount instanceof Bip44Account) {
         return  resources.getDrawable(R.drawable.multikeys_grey);
      }

      //single key account
      return resources.getDrawable(R.drawable.singlekey_grey);
   }

   public static String getNameForNewAccount(WalletAccount account, Context context) {
      if (account instanceof Bip44AccountExternalSignature) {
         return MbwManager.getInstance(context).getTrezorManager().getLabelOrDefault() + " #" + (((Bip44AccountExternalSignature) account).getAccountIndex() + 1);
      } else if (account instanceof Bip44PubOnlyAccount) {
         return context.getString(R.string.account_prefix_imported);
      } else if (account instanceof Bip44Account) {
         return context.getString(R.string.account) + " " + (((Bip44Account) account).getAccountIndex() + 1);
      } else {
         return DateFormat.getMediumDateFormat(context).format(new Date());
      }
   }
}
