package com.mycelium.wallet.activity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.io.CharStreams;
import com.google.common.io.InputSupplier;
import com.mrd.bitlib.crypto.InMemoryPrivateKey;
import com.mycelium.wallet.AndroidRandomSource;
import com.mycelium.wallet.BitIDSignRequest;
import com.mycelium.wallet.MbwManager;
import com.mycelium.wallet.R;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;

import javax.net.ssl.SSLException;

public class BitIDAuthenticationActivity extends ActionBarActivity {

   private enum BitIdStatus {STARTED, SSLPROBLEM, TIMEOUT, NOCONNECTION}

   BitIDSignRequest request;
   TextView errorView;
   Button signInButton;
   TextView question;

   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_bit_idauthentication);
      request = (BitIDSignRequest) getIntent().getSerializableExtra("request");
      signInButton = (Button) findViewById(R.id.bitidsign);
      question = (TextView) findViewById(R.id.tvbitidwebsite);
      question.setText(getString(R.string.bitid_question, request.getHost()));
      question.setVisibility(View.VISIBLE);
      TextView warning = (TextView) findViewById(R.id.tvunsecurewarning);
      if (request.isSecure()) {
         warning.setVisibility(View.INVISIBLE);
      } else {
         warning.setVisibility(View.VISIBLE);
      }
      errorView = (TextView) findViewById(R.id.tvbitiderror);
   }

   //Asynctask to calculate signature and contact server
   //todo? make testable and run as a standalone class and pass in/out parameter objects
   private class BitidAsyncTask extends AsyncTask<Void, Integer, String> {
      ProgressDialog progress;
      BitIdStatus status;
      String sslMessage = "";
      int responseCode = 0;

      public final boolean igNoreSslProblems;

      private BitidAsyncTask(boolean igNoreSslProblems) {
         this.igNoreSslProblems = igNoreSslProblems;
      }

      @Override
      protected String doInBackground(Void... params) {
         try {
            MbwManager manager = MbwManager.getInstance(BitIDAuthenticationActivity.this);
            String uri = request.getFullUri();
            InMemoryPrivateKey privateKey = manager.getRecordManager().getSelectedRecord().key;
            String signature = privateKey.signMessage(uri, new AndroidRandomSource()).getBase64Signature();
            String address = privateKey.getPublicKey().toAddress(manager.getNetwork()).toString();

            final HttpURLConnection conn = (HttpURLConnection) new URL(request.getCallbackUri()).openConnection();
            //todo evaluate igNoreSslProblems to disable verification stuff
            conn.setReadTimeout(5000);
            conn.setConnectTimeout(5000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
            writer.write(request.getJsonString(address, signature));
            writer.flush();
            writer.close();
            os.close();

            conn.connect();
            responseCode = conn.getResponseCode();

            return CharStreams.toString(CharStreams.newReaderSupplier(new InputSupplier<InputStream>() {
               @Override
               public InputStream getInput() throws IOException {
                  if (responseCode >= 200 && responseCode < 300) {
                     return conn.getInputStream();
                  }
                  return conn.getErrorStream();
               }
            }, Charsets.UTF_8));
         } catch (SocketTimeoutException ste) {
            //connection timed out
            status = BitIdStatus.TIMEOUT;
            return null;
         } catch (UnknownHostException uhe) {
            //host not known, most probably the device has no internet connection
            status = BitIdStatus.NOCONNECTION;
            return null;
         } catch (SSLException ssle) {
            //ask user whether he wants to proceed although there is a problem with the certificate
            sslMessage = ssle.getLocalizedMessage();
            status = BitIdStatus.SSLPROBLEM;
            return null;
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
      }

      @Override
      protected void onPreExecute() {
         progress = new ProgressDialog(BitIDAuthenticationActivity.this);
         progress.setCancelable(false);
         progress.setMessage(getString(R.string.bitid_processing));
         progress.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         progress.setProgress(0);
         progress.show();
         status = BitIdStatus.STARTED;
      }

      @Override
      protected void onPostExecute(String result) {
         progress.dismiss();
         if (null == result) {
            if (BitIdStatus.NOCONNECTION == status) {
               //No connection could be established
               Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_noconnection, Toast.LENGTH_LONG).show();
            } else if (BitIdStatus.TIMEOUT == status) {
               //timeout occured
               Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_timeout, Toast.LENGTH_LONG).show();
            } else if (BitIdStatus.SSLPROBLEM == status) {
               showDialog(sslMessage);
            }
            return;
         }
         if (responseCode >= 200 && responseCode < 300) {
            //Success - we have been logged in
            Toast.makeText(BitIDAuthenticationActivity.this, R.string.bitid_loggedin, Toast.LENGTH_LONG).show();
            signInButton.setVisibility(View.INVISIBLE);
            question.setText(getString(R.string.bitid_success, request.getHost()));
         } else if (responseCode >= 400 && responseCode < 500) {
            try {
               //Display the error message if its short enough. Most probably the nonce has timed out.
               if (result.contains("NONCE has expired")) {
                  result = getString(R.string.bitid_expired);
               } else if (result.length() > 500) {
                  result = getString(R.string.bitid_error);
               } else {
                  result = getString(R.string.bitid_errorheader) + responseCode + ": " + result;
               }
               errorView.setText(result);
               errorView.setVisibility(View.VISIBLE);
            } catch (Exception e) {
               throw new RuntimeException(e);
            }
         } else if (responseCode >= 500 && responseCode < 600) {
            //server-side error
            result = getString(R.string.bitid_error);
            errorView.setText(result);
            errorView.setVisibility(View.VISIBLE);
         } else {
            //redirect or strange status code
            result = getString(R.string.bitid_error);
            errorView.setText(result);
            errorView.setVisibility(View.VISIBLE);
         }
      }
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

   public void sign(View view) {
      signAndSend(false);
   }

   private void signAndSend(boolean ignoreSSL) {
      errorView.setVisibility(View.INVISIBLE);
      try {
         new BitidAsyncTask(ignoreSSL).execute();
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   public void abort(View view) {
      finish();
   }
}
