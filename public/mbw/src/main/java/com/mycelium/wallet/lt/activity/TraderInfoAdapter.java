package com.mycelium.wallet.lt.activity;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.RatingBar;
import android.widget.TextView;

import com.google.common.base.Preconditions;
import com.mycelium.wallet.R;

public class TraderInfoAdapter extends ArrayAdapter<TraderInfoAdapter.InfoItem> {
   
   public static class InfoItem {
      final String label;
      final String value;
      final Float rating;

      public InfoItem(String label, String value) {
         this.label = label;
         this.value = value;
         this.rating = null;
      }

      public InfoItem(String label, Float rating) {
         this.label = label;
         this.value = null;
         this.rating = rating;
      }
   }
   
   private Context _context;

   
   public TraderInfoAdapter(Context context, List<InfoItem> objects) {
      super(context, R.layout.lt_trader_info_row, objects);
      _context = context;
   }

   @Override
   public View getView(int position, View convertView, ViewGroup parent) {
      View v = convertView;
      InfoItem o = getItem(position);
      if (o.value != null) {
         // Set String value
         LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         v = Preconditions.checkNotNull(vi.inflate(R.layout.lt_trader_info_row, null));
         ((TextView) v.findViewById(R.id.tvValue)).setText(o.value);
      } else if (o.rating != null) {
         // Set Rating
         LayoutInflater vi = (LayoutInflater) _context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
         v = Preconditions.checkNotNull(vi.inflate(R.layout.lt_trader_info_rating_row, null));
         RatingBar ratingBar = (RatingBar) v.findViewById(R.id.rating);
         ratingBar.setRating(o.rating);
      } else {
         throw new RuntimeException();
      }

      // Set label
      ((TextView) v.findViewById(R.id.tvLabel)).setText(o.label);
      return v;
   }
}
