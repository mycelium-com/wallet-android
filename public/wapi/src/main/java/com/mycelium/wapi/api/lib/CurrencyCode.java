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

package com.mycelium.wapi.api.lib;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public enum CurrencyCode {
   // Don't remove the // at the end of each line here, they prevent the
   // formatter from formatting
   UNKNOWN("UNKNOWN", "UNKNOWN"), //
   AED("AED", "United Arab Emirates Dirham"), //
   AFN("AFN", "Afghan Afghani"), //
   ALL("ALL", "Albanian Lek"), //
   AMD("AMD", "Armenian Dram"), //
   ANG("ANG", "Netherlands Antillean Guilder"), //
   AOA("AOA", "Angolan Kwanza"), //
   ARS("ARS", "Argentine Peso"), //
   AUD("AUD", "Australian Dollar"), //
   AWG("AWG", "Aruban Florin"), //
   AZN("AZN", "Azerbaijani Manat"), //
   BAM("BAM", "Bosnia-Herzegovina Convertible Mark"), //
   BBD("BBD", "Barbadian Dollar"), //
   BDT("BDT", "Bangladeshi Taka"), //
   BGN("BGN", "Bulgarian Lev"), //
   BHD("BHD", "Bahraini Dinar"), //
   BIF("BIF", "Burundian Franc"), //
   BMD("BMD", "Bermudan Dollar"), //
   BND("BND", "Brunei Dollar"), //
   BOB("BOB", "Bolivian Boliviano"), //
   BRL("BRL", "Brazilian Real"), //
   BSD("BSD", "Bahamian Dollar"), //
   BTN("BTN", "Bhutanese Ngultrum"), //
   BWP("BWP", "Botswanan Pula"), //
   BYR("BYR", "Belarusian Ruble"), //
   BZD("BZD", "Belize Dollar"), //
   CAD("CAD", "Canadian Dollar"), //
   CDF("CDF", "Congolese Franc"), //
   CHF("CHF", "Swiss Franc"), //
   CLF("CLF", "Chilean Unit of Account (UF)"), //
   CLP("CLP", "Chilean Peso"), //
   CNY("CNY", "Chinese Yuan"), //
   COP("COP", "Colombian Peso"), //
   CRC("CRC", "Costa Rican Col\u00F3n"), //
   CUP("CUP", "Cuban Peso"), //
   CVE("CVE", "Cape Verdean Escudo"), //
   CZK("CZK", "Czech Republic Koruna"), //
   DJF("DJF", "Djiboutian Franc"), //
   DKK("DKK", "Danish Krone"), //
   DOP("DOP", "Dominican Peso"), //
   DZD("DZD", "Algerian Dinar"), //
   EEK("EEK", "Estonian Kroon"), //
   EGP("EGP", "Egyptian Pound"), //
   ETB("ETB", "Ethiopian Birr"), //
   EUR("EUR", "Euro"), //
   FJD("FJD", "Fijian Dollar"), //
   FKP("FKP", "Falkland Islands Pound"), //
   GBP("GBP", "British Pound Sterling"), //
   GEL("GEL", "Georgian Lari"), //
   GHS("GHS", "Ghanaian Cedi"), //
   GIP("GIP", "Gibraltar Pound"), //
   GMD("GMD", "Gambian Dalasi"), //
   GNF("GNF", "Guinean Franc"), //
   GTQ("GTQ", "Guatemalan Quetzal"), //
   GYD("GYD", "Guyanaese Dollar"), //
   HKD("HKD", "Hong Kong Dollar"), //
   HNL("HNL", "Honduran Lempira"), //
   HRK("HRK", "Croatian Kuna"), //
   HTG("HTG", "Haitian Gourde"), //
   HUF("HUF", "Hungarian Forint"), //
   IDR("IDR", "Indonesian Rupiah"), //
   ILS("ILS", "Israeli New Sheqel"), //
   INR("INR", "Indian Rupee"), //
   IQD("IQD", "Iraqi Dinar"), //
   IRR("IRR", "Iranian Rial"), //
   ISK("ISK", "Icelandic Kr\u00F3na"), //
   JEP("JEP", "Jersey Pound"), //
   JMD("JMD", "Jamaican Dollar"), //
   JOD("JOD", "Jordanian Dinar"), //
   JPY("JPY", "Japanese Yen"), //
   KES("KES", "Kenyan Shilling"), //
   KGS("KGS", "Kyrgystani Som"), //
   KHR("KHR", "Cambodian Riel"), //
   KMF("KMF", "Comorian Franc"), //
   KPW("KPW", "North Korean Won"), //
   KRW("KRW", "South Korean Won"), //
   KWD("KWD", "Kuwaiti Dinar"), //
   KYD("KYD", "Cayman Islands Dollar"), //
   KZT("KZT", "Kazakhstani Tenge"), //
   LAK("LAK", "Laotian Kip"), //
   LBP("LBP", "Lebanese Pound"), //
   LKR("LKR", "Sri Lankan Rupee"), //
   LRD("LRD", "Liberian Dollar"), //
   LSL("LSL", "Lesotho Loti"), //
   LTL("LTL", "Lithuanian Litas"), //
   LYD("LYD", "Libyan Dinar"), //
   MAD("MAD", "Moroccan Dirham"), //
   MDL("MDL", "Moldovan Leu"), //
   MGA("MGA", "Malagasy Ariary"), //
   MKD("MKD", "Macedonian Denar"), //
   MMK("MMK", "Myanma Kyat"), //
   MNT("MNT", "Mongolian Tugrik"), //
   MOP("MOP", "Macanese Pataca"), //
   MRO("MRO", "Mauritanian Ouguiya"), //
   MTL("MTL", "Maltese Lira"), //
   MUR("MUR", "Mauritian Rupee"), //
   MVR("MVR", "Maldivian Rufiyaa"), //
   MWK("MWK", "Malawian Kwacha"), //
   MXN("MXN", "Mexican Peso"), //
   MYR("MYR", "Malaysian Ringgit"), //
   MZN("MZN", "Mozambican Metical"), //
   NAD("NAD", "Namibian Dollar"), //
   NGN("NGN", "Nigerian Naira"), //
   NIO("NIO", "Nicaraguan C\u00F3rdoba"), //
   NOK("NOK", "Norwegian Krone"), //
   NPR("NPR", "Nepalese Rupee"), //
   NZD("NZD", "New Zealand Dollar"), //
   OMR("OMR", "Omani Rial"), //
   PAB("PAB", "Panamanian Balboa"), //
   PEN("PEN", "Peruvian Nuevo Sol"), //
   PGK("PGK", "Papua New Guinean Kina"), //
   PHP("PHP", "Philippine Peso"), //
   PKR("PKR", "Pakistani Rupee"), //
   PLN("PLN", "Polish Zloty"), //
   PYG("PYG", "Paraguayan Guarani"), //
   QAR("QAR", "Qatari Rial"), //
   RON("RON", "Romanian Leu"), //
   RSD("RSD", "Serbian Dinar"), //
   RUB("RUB", "Russian Ruble"), //
   RWF("RWF", "Rwandan Franc"), //
   SAR("SAR", "Saudi Riyal"), //
   SBD("SBD", "Solomon Islands Dollar"), //
   SCR("SCR", "Seychellois Rupee"), //
   SDG("SDG", "Sudanese Pound"), //
   SEK("SEK", "Swedish Krona"), //
   SGD("SGD", "Singapore Dollar"), //
   SHP("SHP", "Saint Helena Pound"), //
   SLL("SLL", "Sierra Leonean Leone"), //
   SOS("SOS", "Somali Shilling"), //
   SRD("SRD", "Surinamese Dollar"), //
   STD("STD", "S\u00E3o Tom\u00E9 and Pr\u00EDncipe Dobra"), //
   SVC("SVC", "Salvadoran Col\u00F3n"), //
   SYP("SYP", "Syrian Pound"), //
   SZL("SZL", "Swazi Lilangeni"), //
   THB("THB", "Thai Baht"), //
   TJS("TJS", "Tajikistani Somoni"), //
   TMT("TMT", "Turkmenistani Manat"), //
   TND("TND", "Tunisian Dinar"), //
   TOP("TOP", "Tongan Pa\u0027anga"), //
   TRY("TRY", "Turkish Lira"), //
   TTD("TTD", "Trinidad and Tobago Dollar"), //
   TWD("TWD", "New Taiwan Dollar"), //
   TZS("TZS", "Tanzanian Shilling"), //
   UAH("UAH", "Ukrainian Hryvnia"), //
   UGX("UGX", "Ugandan Shilling"), //
   USD("USD", "United States Dollar"), //
   UYU("UYU", "Uruguayan Peso"), //
   UZS("UZS", "Uzbekistan Som"), //
   VEF("VEF", "Venezuelan Bol\u00EDvar Fuerte"), //
   VND("VND", "Vietnamese Dong"), //
   VUV("VUV", "Vanuatu Vatu"), //
   WST("WST", "Samoan Tala"), //
   XAF("XAF", "CFA Franc BEAC"), //
   XAG("XAG", "Silver (troy ounce)"), //
   XAR("XAR", "Argentine Peso Blue Rate"), //
   XAU("XAU", "Gold (troy ounce)"), //
   XCD("XCD", "East Caribbean Dollar"), //
   XDR("XDR", "Special Drawing Rights"), //
   XOF("XOF", "CFA Franc BCEAO"), //
   XPF("XPF", "CFP Franc"), //
   YER("YER", "Yemeni Rial"), //
   ZAR("ZAR", "South African Rand"), //
   ZMK("ZMK", "Zambian Kwacha (pre-2013)"), //
   ZMW("ZMW", "Zambian Kwacha"), //
   ZWL("ZWL", "Zimbabwean Dollar"); //

   private static final Map<String, CurrencyCode> _stringMap = new HashMap<String, CurrencyCode>();
   private static final CurrencyCode[] SORTED_ARRAY;

   static {
      // Construct map for fast lookup by string value
      for (CurrencyCode code : CurrencyCode.values()) {
         _stringMap.put(code.getShortString(), code);
      }

      // Construct a sorted array not including UNKNOWN
      SORTED_ARRAY = new CurrencyCode[CurrencyCode.values().length - 1];
      int index = 0;
      for (CurrencyCode code : CurrencyCode.values()) {
         if (code == UNKNOWN) {
            continue;
         }
         SORTED_ARRAY[index++] = code;
      }
      // Sort according to currency short string
      Arrays.sort(SORTED_ARRAY, new Comparator<CurrencyCode>() {
         @Override
         public int compare(CurrencyCode c1, CurrencyCode c2) {
            return c1.toString().compareTo(c2.toString());
         }
      });

   }

   /**
    * Get a currency code from its short string
    * 
    * @param shortString
    *           three-letter code
    * @return The corresponding CurrencyCode or UNKNOWN if no match was found
    */
   public static CurrencyCode fromShortString(String shortString) {
      CurrencyCode code = _stringMap.get(shortString);
      if (code == null) {
         code = UNKNOWN;
      }
      return code;
   }

   public static CurrencyCode[] sortedArray() {
      return SORTED_ARRAY;
   }

   private String _shortString;
   private String _name;

   private CurrencyCode(String shortString, String name) {
      _shortString = shortString;
      _name = name;
   }

   private CurrencyCode() {
      // Used by Jackson
   }

   @Override
   public String toString() {
      return _shortString;
   }

   public String getShortString() {
      return _shortString;
   }

   public String getName() {
      return _name;
   }

}
