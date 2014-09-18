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

package com.mycelium.wallet.lt;

import java.net.MalformedURLException;
import java.net.URL;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.mycelium.wallet.Constants;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

public class BannerFactory {

   private static final BannerInfo[] COUNTRIES = new BannerInfo[] { new BannerInfo("VN",
         R.drawable.background_banner_white, "Bitcoin Vietnam", R.color.red, R.drawable.vn_banner_icon,
         "http://bitcoinvietnam.com.vn", R.string.vn_banner_description) };

   private static class BannerInfo {

      public final String country;
      public int backgroundResourceId;
      public String name;
      public int nameColorResourceId;
      public int iconResourceId;
      public String url;
      public int descriptionResourceId;

      private BannerInfo(String country, int backgroundResourceId, String name, int nameColorResourceId,
            int iconResourceId, String url, int descriptionResourceId) {
         this.country = country;
         this.backgroundResourceId = backgroundResourceId;
         this.name = name;
         this.nameColorResourceId = nameColorResourceId;
         this.iconResourceId = iconResourceId;
         this.url = url;
         this.descriptionResourceId = descriptionResourceId;
      }
   }

   public static View createBanner(Context context, String country) {
      MbwManager manager = MbwManager.getInstance(context);
      if (!manager.getBrand().equals(Constants.BRAND_BITS_OF_GOLD)) {
         // Banners only supported for Bits of Gold release
         return null;
      }
      for (BannerInfo info : COUNTRIES) {
         if (info.country.equals(country)) {
            return createBanner(context, info);
         }
      }
      return null;
   }

   private static View createBanner(Context context, BannerInfo info) {

      URL url;
      try {
         url = new URL(info.url);
      } catch (MalformedURLException e) {
         Log.e("BannerFactory", "Malformed URL '" + info.url + "'");
         return null;
      }

      LayoutInflater vi = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

      // Banner
      View banner = vi.inflate(R.layout.lt_banner_card, null);

      // Banner background
      View background = banner.findViewById(R.id.llBackground);
      //background.setBackgroundResource(info.backgroundResourceId);
      setBackgroundResource(background, info.backgroundResourceId);
      
      // Icon
       ImageView icon = (ImageView) banner.findViewById(R.id.ivIcon);
       icon.setImageResource(info.iconResourceId);

      // Name
      TextView name = (TextView) banner.findViewById(R.id.tvName);
      name.setText(info.name);
      name.setTextColor(context.getResources().getColor(info.nameColorResourceId));

      // URL
      TextView urlText = (TextView) banner.findViewById(R.id.tvUrl);
      urlText.setText(url.getHost());

      // Description
      TextView description = (TextView) banner.findViewById(R.id.tvDesc);
      description.setText(info.descriptionResourceId);

      banner.setTag(url);
      return banner;
   }
   
   private static void setBackgroundResource(View theView, int backgroundResource) {
      int bottom = theView.getPaddingBottom();
      int top = theView.getPaddingTop();
      int right = theView.getPaddingRight();
      int left = theView.getPaddingLeft();
      theView.setBackgroundResource(backgroundResource);
      theView.setPadding(left, top, right, bottom);
   }

}
