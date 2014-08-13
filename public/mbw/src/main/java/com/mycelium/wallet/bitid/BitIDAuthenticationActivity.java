package com.mycelium.wallet.bitid;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.squareup.otto.Subscribe;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;
import com.mycelium.wallet.Record;
import com.mycelium.wallet.RecordManager;
import com.squareup.otto.Bus;

public class BitIDAuthenticationActivity extends ActionBarActivity  {

   private static final String ERROR_TEXT = "errortext";
   private static final String QUESTION_TEXT = "questiontext";
   private static final String SIGNBUTTON_VISIBLE = "signbuttonvisible";
   private static final String ERRORVIEW_VISIBLE = "errorviewvisible";

   private BitIDSignRequest request;
   private TextView errorView;
   private Button signInButton;
   private TextView question;
   private ProgressDialog progress;

   public static void callMe(Activity currentActivity, BitIDSignRequest request) {
      Intent intent = new Intent(currentActivity, BitIDAuthenticationActivity.class);
      intent.putExtra("request", request);
      currentActivity.startActivity(intent);
   }

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_bit_idauthentication);
      request = (BitIDSignRequest) getIntent().getSerializableExtra("request");
      signInButton = (Button) findViewById(R.id.bitidsign);
      errorView = (TextView) findViewById(R.id.tvbitiderror);
      question = (TextView) findViewById(R.id.tvbitidwebsite);
      question.setText(getString(R.string.bitid_question, request.getHost()));
      TextView warning = (TextView) findViewById(R.id.tvunsecurewarning);
      if (request.isSecure()) {
         warning.setVisibility(View.INVISIBLE);
      } else {
         warning.setVisibility(View.VISIBLE);
      }
      progress = new ProgressDialog(this);
      if (savedInstanceState != null) {
         errorView.setText(savedInstanceState.getString(ERROR_TEXT));
         question.setText(savedInstanceState.getString(QUESTION_TEXT));
         if (savedInstanceState.getBoolean(SIGNBUTTON_VISIBLE))  signInButton.setVisibility(View.VISIBLE); else signInButton.setVisibility(View.INVISIBLE);
         if (savedInstanceState.getBoolean(ERRORVIEW_VISIBLE)) errorView.setVisibility(View.VISIBLE); else errorView.setVisibility(View.INVISIBLE);
      }
   }

   @Override
   public void onResume() {
      getEventBus().register(this);
      super.onResume();
   }

   @Override
   public void onPause() {
      progress.dismiss();
      getEventBus().unregister(this);
      super.onPause();
   }

   @Override
   protected void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);
      outState.putString(ERROR_TEXT, errorView.getText().toString());
      outState.putString(QUESTION_TEXT, question.getText().toString());
      outState.putBoolean(SIGNBUTTON_VISIBLE, signInButton.getVisibility() == View.VISIBLE);
      outState.putBoolean(ERRORVIEW_VISIBLE, errorView.getVisibility() == View.VISIBLE);
   }

   private void showDialog(String message) {
      AlertDialog.Builder builder = new AlertDialog.Builder(BitIDAuthenticationActivity.this);
      builder.setTitle(getString(R.string.bitid_sslproblem));
      builder.setMessage(getString(R.string.bitid_sslquestion) + "\n" + message);

      DialogInterface.OnClickListener yesListen = new DialogInterface.OnClickListener() {
         @Override
         public void onClick(DialogInterface dialog, int which) {
            signAndSend(true);
            dialog.dismiss();
         }
      };

      DialogInterface.OnClickListener noListen = new DialogInterface.OnClickListener() {
         public void onClick(DialogInterface dialog, int which) {
            Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_aborted, Toast.LENGTH_LONG).show();
            dialog.dismiss();
         }
      };

      builder.setPositiveButton(getString(R.string.yes), yesListen);
      builder.setNegativeButton(getString(R.string.no), noListen);
      AlertDialog alert = builder.create();
      alert.show();
   }

   @Subscribe
   public void onTaskCompleted(BitIDResponse response) {
      progress.dismiss();
      if (BitIDResponse.ResponseStatus.NOCONNECTION == response.status) {
         Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_noconnection, Toast.LENGTH_LONG).show();
      } else if (BitIDResponse.ResponseStatus.TIMEOUT == response.status) {
         Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_timeout, Toast.LENGTH_LONG).show();
      } else if (BitIDResponse.ResponseStatus.SSLPROBLEM == response.status) {
         showDialog(response.message);
      } else if (BitIDResponse.ResponseStatus.SUCCESS == response.status) {
         showLoggedIn();
      } else if (BitIDResponse.ResponseStatus.ERROR == response.status) {
         handleError(response);
      } else {
         throw new RuntimeException("Invalid Status in BitIDResponse - this should not be possible.");
      }
   }

   private void handleError(BitIDResponse response) {
      String message = response.message;
      int code = response.code;
      String userInfo;
      if (code >= 400 && code < 500) {
         //Display the error message if its short enough. Most probably the nonce has timed out.
         if (message.contains("NONCE has expired") || message.contains("NONCE is illegal")) {
            userInfo = getString(R.string.bitid_expired);
         } else if (message.length() > 500) {
            userInfo = getString(R.string.bitid_error);
         } else {
            userInfo = getString(R.string.bitid_errorheader) + code + ": " + response.message;
         }
      } else if (code >= 500 && code < 600) {
         //server-side error
         userInfo = getString(R.string.bitid_error);
      } else {
         //redirect or strange status code
         userInfo = getString(R.string.bitid_error);
      }
      errorView.setText(userInfo);
      errorView.setVisibility(View.VISIBLE);
   }

   private void showLoggedIn() {
      //Success - we have been logged in
      Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_loggedin, Toast.LENGTH_LONG).show();
      signInButton.setVisibility(View.INVISIBLE);
      question.setText(getString(R.string.bitid_success, request.getHost()));
   }

   public void sign(View view) {
      signAndSend(true);
   }

   private void signAndSend(boolean enforceSslCorrectness) {
      progress.setCancelable(false);
      progress.setMessage(getString(R.string.bitid_processing));
      progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
      progress.show();
      errorView.setVisibility(View.INVISIBLE);
      try {
         new BitIdAsyncTask(request, enforceSslCorrectness, getRecord(), getEventBus()).execute();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public void abort(View view) {
      finish();
   }

   private MbwManager getMbwManager() {
      return MbwManager.getInstance(this);
   }

   private Bus getEventBus() {
      return getMbwManager().getEventBus();
   }

   Record getRecord() {
      return getRecordManager().getSelectedRecord();
   }

   private RecordManager getRecordManager() {
      return getMbwManager().getRecordManager();
   }
}

