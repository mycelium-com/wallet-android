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

import com.mycelium.wallet.GpsLocationFetcher.GpsLocationEx;

public class Constants {

   public static final long ONE_BTC_IN_SATOSHIS = 100000000L;

   public static final long MS_PR_SECOND = 1000L;
   public static final long MS_PR_MINUTE = MS_PR_SECOND * 60;
   public static final long MS_PR_HOUR = MS_PR_MINUTE * 60;
   public static final long MS_PR_DAY = MS_PR_HOUR * 24;

   /**
    * The number of transactions to display in our transaction history. The
    * higher the number the more bandwidth we require from the smartphone.
    */
   public static final int TRANSACTION_HISTORY_LENGTH = 20;

   /**
    * Settings and their default values
    */
   public static final String SETTINGS_NAME = "settings";
   public static final String PIN_SETTING = "PIN";
   public static final String PROXY_SETTING = "proxy";
   public static final String FIAT_CURRENCY_SETTING = "FiatCurrency";
   public static final String DEFAULT_CURRENCY = "USD";
   public static final String BITCOIN_DENOMINATION_SETTING = "BitcoinDenomination";
   public static final String ENABLE_CONTINUOUS_FOCUS_SETTING = "EnableContinuousFocusSetting";
   public static final String KEY_MANAGEMENT_LOCKED_SETTING = "KeyManagementLocked";
   public static final String LAST_OBSERVED_BLOCK_HEIGHT_SETTING = "LastObservedBlockHeight";
   public static final String MAIN_VIEW_FRAGMENT_INDEX_SETTING = "MainViewFragmentIndex";
   public static final String MYCELIUM_WALLET_HELP_URL = "http://www.mycelium.com/wallet/help.html";
   public static final String LANGUAGE_SETTING = "user_language";
   public static final String IGNORED_VERSIONS = "ignored_versions";
   public static final String LAST_UPDATE_CHECK = "last_update_check";

   public static final String TAG = "MyceliumWallet";

   // Brands
   public static final String BRAND_MYCELIUM = "mycelium";
   public static final String BRAND_BITS_OF_GOLD = "bog";
   
   // Local Trader constants
   public static final String LOCAL_TRADER_SETTINGS_NAME = "localTrader.settings";
   public static final String LOCAL_TRADER_ADDRESS_SETTING = "traderAddress";
   public static final String LOCAL_TRADER_KEY_SETTING = "traderPrivateKey";
   public static final String LOCAL_TRADER_ACCOUNT_ID_SETTING = "traderAccountId";
   public static final String LOCAL_TRADER_NICKNAME_SETTING = "nickname";
   public static final String LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING = "lastTraderSync";
   public static final String LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING = "lastTraderNotification";
   public static final String LOCAL_TRADER_LOCATION_NAME_SETTING = "locationName";
   public static final String LOCAL_TRADER_LOCATION_COUNTRY_CODE_SETTING = "locationCountryCode";
   public static final String LOCAL_TRADER_LATITUDE_SETTING = "latitude";
   public static final String LOCAL_TRADER_LONGITUDE_SETTING = "longitude";
   public static final GpsLocationEx LOCAL_TRADER_DEFAULT_LOCATION = new GpsLocationEx(48.2162845, 16.2484715,
         "Penzing, Vienna", "AT");
   public static final String LOCAL_TRADER_DISABLED_SETTING = "isLocalTraderDisabled";
   public static final String LOCAL_TRADER_PLAY_SOUND_ON_TRADE_NOTIFICATION_SETTING = "playSoundOnTradeNotification";
   public static final String LOCAL_TRADER_USE_MILES_SETTING = "useMiles";
   public static final String LOCAL_TRADER_GCM_SETTINGS_NAME = "localTrader.gcm.settings";
   public static final String LOCAL_TRADER_HELP_URL = "http://www.mycelium.com/lt/help.html";
   public static final String LOCAL_TRADER_MAP_URL = "http://www.mycelium.com/lt/m";

   public static final String IGNORE_NEW_API = "NewApi";

   public static final String TRANSACTION_HASH_INTENT_KEY = "transaction_hash";

   public static final String PRODNET_DONATION_ADDRESS = "13YxhmcAyr9W1frumWr3trXLAj2hSHWBmo";
   public static final String TESTNET_DONATION_ADDRESS = "mtjVQkh6kZbixqwLPMinWievE5jWSHdovP";

}
