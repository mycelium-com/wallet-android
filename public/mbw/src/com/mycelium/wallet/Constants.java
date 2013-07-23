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

package com.mycelium.wallet;

import com.mrd.bitlib.model.NetworkParameters;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl.HttpEndpoint;
import com.mrd.mbwapi.impl.MyceliumWalletApiImpl.HttpsEndpoint;

public class Constants {

	/**
	 * The thumbprint of the Mycelium certificate. We use this for pinning the
	 * server certificate.
	 */
	private static final String myceliumThumbprint = "B3:42:65:33:40:F5:B9:1B:DA:A2:C8:7A:F5:4C:7C:5D:A9:63:C4:C3";

	/**
	 * Two redundant Mycelium wallet service servers
	 */
	private static final HttpsEndpoint smws1 = new HttpsEndpoint(
			"https://mws1.mycelium.com/mws", myceliumThumbprint);
	private static final HttpsEndpoint smws2 = new HttpsEndpoint(
			"https://mws2.mycelium.com/mws", myceliumThumbprint);

	/**
	 * Unencrypted wallet service endpoints, used for testing
	 */
	@SuppressWarnings("unused")
	private static final HttpEndpoint mws1 = new HttpEndpoint(
			"http://mws1.mycelium.com/mws");
	@SuppressWarnings("unused")
	private static final HttpEndpoint mws2 = new HttpEndpoint(
			"http://mws2.mycelium.com/mws");

	/**
	 * The set of endpoints we use. The wallet chooses a random endpoint and if
	 * it does not respond it round-robins through the list. This way we achieve
	 * client side load-balancing and fail-over.
	 */
	private static final HttpEndpoint[] _serverEndpoints = new HttpEndpoint[] {
			smws1, smws2 };
	public static final NetworkParameters network = NetworkParameters.productionNetwork;
	public static final MyceliumWalletApiImpl bccapi = new MyceliumWalletApiImpl(_serverEndpoints, network);
//	public static final MyceliumWalletApiImpl bccapi = new MyceliumWalletApiImpl(new HttpEndpoint[]{new HttpEndpoint("http://192.168.178.66:8080/mws")}, network);

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
	public static final String FIAT_CURRENCY_SETTING = "FiatCurrency";
	public static final String DEFAULT_CURRENCY = "USD";
	public static final String WALLET_MODE_SETTING = "WalletMode";
	public static final WalletMode DEFAULT_WALLET_MODE = WalletMode.Aggregated;
	public static final String BITCOIN_DENOMINATION_SETTING = "BitcoinDenomination";
	public static final String DEFAULT_BITCOIN_DENOMINATION = "BTC";
	public static final String CURRENT_HINT_INDEX_SETTING = "CurrentHintIndex";
	public static final String SHOW_HINTS_SETTING = "ShowHints";
	public static final String AUTOPAY_SETTING = "autopayAmount";

	public static final String TAG = "MyceliumWallet";
}
