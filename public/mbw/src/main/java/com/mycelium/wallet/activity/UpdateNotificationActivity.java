package com.mycelium.wallet.activity;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Preconditions;

import com.mrd.mbwapi.api.WalletVersionResponse;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.VersionManager;

public class UpdateNotificationActivity extends Activity {

   public static final String RESPONSE = "WalletVersionResponse";

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setTitle(R.string.new_version_exists);
      setContentView(R.layout.update_notification);

      final WalletVersionResponse response = Preconditions.checkNotNull((WalletVersionResponse) getIntent().getSerializableExtra(RESPONSE));
      Button ignoreButton = (Button) findViewById(R.id.ignoreUpdate);
      Button playButton = (Button) findViewById(R.id.getPlay);
      Button myceliumButton = (Button) findViewById(R.id.getMycelium);
      TextView versionNumber = (TextView) findViewById(R.id.versionNumber);
      TextView message = (TextView) findViewById(R.id.updateMessage);

      versionNumber.setText(response.versionNumber);
      message.setText(response.message);
      if ("".equals(response.message)){
         message.setVisibility(View.GONE);
      }

      myceliumButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            Uri parse = Uri.parse(response.directDownload.toString());
            startActivity(new Intent(Intent.ACTION_VIEW, parse));
         }
      });

      playButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            Uri parse = Uri.parse("market://details?id=com.mycelium.wallet");
            startActivity(new Intent(Intent.ACTION_VIEW, parse));
         }
      });

      ignoreButton.setOnClickListener(new View.OnClickListener() {
         @Override
         public void onClick(View v) {
            VersionManager versionManager = MbwManager.getInstance(UpdateNotificationActivity.this).getVersionManager();
            versionManager.ignoreVersion(response.versionNumber);
            finish();
         }
      });

   }


}
