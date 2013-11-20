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

public class Constants {

   public static final long ONE_BTC_IN_SATOSHIS = 100000000L;

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
   public static final String WALLET_MODE_SETTING = "WalletMode";
   public static final WalletMode DEFAULT_WALLET_MODE = WalletMode.Segregated;
   public static final String BITCOIN_DENOMINATION_SETTING = "BitcoinDenomination";
   public static final String DEFAULT_BITCOIN_DENOMINATION = "BTC";
   public static final String CURRENT_HINT_INDEX_SETTING = "CurrentHintIndex";
   public static final String ENABLE_CONTINUOUS_FOCUS_SETTING = "EnableContinuousFocusSetting";
   public static final String EXPERT_MODE_SETTING = "ExpertMode";
   public static final String KEY_MANAGEMENT_LOCKED_SETTING = "KeyManagementLocked";
   public static final String LAST_OBSERVED_BLOCK_HEIGHT_SETTING = "LastObservedBlockHeight";
   public static final String EXCHANGE_RATE_CALCULATION_METHOD_SETTING = "ExchangeRateCalculationMethod";
   public static final ExchangeRateCalculationMode DEFAULT_EXCHANGE_RATE_CALCULATION_METHOD = ExchangeRateCalculationMode.BITSTAMP;
   public static final String MAIN_VIEW_FRAGMENT_INDEX_SETTING = "MainViewFragmentIndex";
   public static final String MYCELIUM_WALLET_HELP_URL = "http://www.mycelium.com/wallet/help.html";

   public static final String TAG = "MyceliumWallet";
}
