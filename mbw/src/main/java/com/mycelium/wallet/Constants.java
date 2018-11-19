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

public interface Constants {
   long ONE_uBTC_IN_SATOSHIS = 100;
   long ONE_mBTC_IN_SATOSHIS = 1000 * ONE_uBTC_IN_SATOSHIS;
   long ONE_BTC_IN_SATOSHIS  = 1000 * ONE_mBTC_IN_SATOSHIS;

   long MS_PR_SECOND = 1000L;
   long MS_PR_MINUTE = MS_PR_SECOND * 60;
   long MS_PR_HOUR = MS_PR_MINUTE * 60;
   long MS_PR_DAY = MS_PR_HOUR * 24;

   int SHORT_HTTP_TIMEOUT_MS = 4000;

   /**
    * Settings and their default values
    */
   String SETTINGS_NAME = "settings";
   String PIN_SETTING = "PIN";
   String PIN_SETTING_RESETTABLE = "PinResettable";
   String RANDOMIZE_PIN = "randomizePin";
   String PIN_SETTING_REQUIRED_ON_STARTUP = "PinOnStartup";
   String PROXY_SETTING = "proxy";
   String FIAT_CURRENCY_SETTING = "FiatCurrency";
   String SELECTED_CURRENCIES = "selectedFiatCurrencies";
   String DEFAULT_CURRENCY = "USD";
   String BITCOIN_DENOMINATION_SETTING = "BitcoinDenomination";
   String MINER_FEE_SETTING = "MinerFeeEstimationSetting";
   String KEY_MANAGEMENT_LOCKED_SETTING = "KeyManagementLocked";
   String MYCELIUM_WALLET_HELP_URL = "http://www.mycelium.com/wallet/help_20.html";
   String PLAYSTORE_BASE_URL = "https://play.google.com/store/apps/details?id=";
   String DIRECT_APK_URL = "https://mycelium.com/download";
   String LANGUAGE_SETTING = "user_language";
   String IGNORED_VERSIONS = "ignored_versions";
   String TOR_MODE = "tor_mode";
   String BLOCK_EXPLORER = "BlockExplorer";
   String CHANGE_ADDRESS_MODE = "change_type";

   // Ledger preferences
   String LEDGER_SETTINGS_NAME = "ledger_settings";
   String LEDGER_DISABLE_TEE_SETTING = "ledger_disable_tee";
   String LEDGER_UNPLUGGED_AID_SETTING = "ledger_unplugged_aid";

   String TAG = "MyceliumWallet";

   // Local Trader constants
   String LOCAL_TRADER_SETTINGS_NAME = "localTrader.settings";
   String LOCAL_TRADER_ADDRESS_SETTING = "traderAddress";
   String LOCAL_TRADER_KEY_SETTING = "traderPrivateKey";
   String LOCAL_TRADER_ACCOUNT_ID_SETTING = "traderAccountId";
   String LOCAL_TRADER_NICKNAME_SETTING = "nickname";
   String LOCAL_TRADER_LAST_TRADER_SYNCHRONIZATION_SETTING = "lastTraderSync";
   String LOCAL_TRADER_LAST_TRADER_NOTIFICATION_SETTING = "lastTraderNotification";
   String LOCAL_TRADER_LOCATION_NAME_SETTING = "locationName";
   String LOCAL_TRADER_LOCATION_COUNTRY_CODE_SETTING = "locationCountryCode";
   String LOCAL_TRADER_LATITUDE_SETTING = "latitude";
   String LOCAL_TRADER_LONGITUDE_SETTING = "longitude";
   GpsLocationEx LOCAL_TRADER_DEFAULT_LOCATION = new GpsLocationEx(48.2162845, 16.2484715, "Penzing, Vienna", "AT");
   String LT_DISABLED = "isLocalTraderDisabled";
   String LT_ENABLED = "isLocalTraderEnabled";
   String LOCAL_TRADER_PLAY_SOUND_ON_TRADE_NOTIFICATION_SETTING = "playSoundOnTradeNotification";
   String LOCAL_TRADER_USE_MILES_SETTING = "useMiles";
   String LOCAL_TRADER_GCM_SETTINGS_NAME = "localTrader.gcm.settings";
   String LOCAL_TRADER_HELP_URL = "http://www.mycelium.com/lt/help.html";

   String LOCAL_TRADER_MAP_URL = "http://www.mycelium.com/lt/m";

   String IGNORE_NEW_API = "NewApi";

   String TRANSACTION_ID_INTENT_KEY = "transaction_id";
   String TRANSACTION_FIAT_VALUE_KEY = "transaction_fiat_value";

   int BITCOIN_BLOCKS_PER_DAY = (24 * 60) / 10;

   // Minimum age of the PIN in blocks, so that we allow a second wordlist backup
   int MIN_PIN_BLOCKHEIGHT_AGE_ADDITIONAL_BACKUP = 2 * BITCOIN_BLOCKS_PER_DAY;

   // Minimum age of the PIN in blocks, until you can reset the PIN
   int MIN_PIN_BLOCKHEIGHT_AGE_RESET_PIN = 7 * BITCOIN_BLOCKS_PER_DAY;
   // Force user to read the warnings about additional backups
   int WAIT_SECONDS_BEFORE_ADDITIONAL_BACKUP = 60;

   String FAILED_PIN_COUNT = "failedPinCount";

   String SETTING_TOR = "useTor";
   String SETTING_DENOMINATION = "bitcoin_denomination";
   String SETTING_MINER_FEE = "miner_fee";
}
