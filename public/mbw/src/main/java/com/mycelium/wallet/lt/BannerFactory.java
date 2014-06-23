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
      View background = (View) banner.findViewById(R.id.llBackground);
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
