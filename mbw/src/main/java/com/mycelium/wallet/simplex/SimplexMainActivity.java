package com.mycelium.wallet.simplex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.common.base.Charsets;
import com.mycelium.wallet.R;

import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.ServerManagedPolicy;

import com.mycelium.wallet.activity.modern.ModernMain;
import com.squareup.otto.Bus;
import com.squareup.otto.ThreadEnforcer;
import com.squareup.otto.Subscribe;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

/**
 * Created by tomb on 11/17/16.
 */

public class SimplexMainActivity extends Activity {

    //the event bus entities
    private static final Bus _eventBus = new Bus(ThreadEnforcer.ANY,"Simplex");
    private Handler _activityHandler;

    //simplex server
    private SimplexServer _server = new SimplexServer();
    private SimplexNonceResponse _simplexNonce;

    //the user wallet address
    private String _walletAddress;

    //lvl entities
    private LicenseCheckerCallback _licenseCheckerCallback;
    private LicenseChecker _checker;

    //the activity ui type
    private SimplexUITypes _simplexUIType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.simplex_main_activity);

        _walletAddress = getIntent().getExtras().getString("walletAddress");
        _activityHandler = new Handler();
        //display the loading spinner
        setLayout(SimplexUITypes.Loading);


        Button retryButton = (Button) findViewById(R.id.btSimplexRetry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLayout(SimplexUITypes.Loading);
                simplexAsync.run();
            }
        });

        //simplex app auth
        simplexAsync.run();
    }

    @Override protected void onResume() {
        super.onResume();

        // Register ourselves so that we can provide the initial value.
        _eventBus.register(this);

        if(this._simplexUIType == SimplexUITypes.WebView) {
            Log.d("simplex","onResume WebView");
            Intent i = new Intent(SimplexMainActivity.this, ModernMain.class);
            i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override protected void onPause() {
        super.onPause();

        // Always unregister when an object no longer should be on the bus.
        _eventBus.unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCheckers();
    }
    /** Simplex App Auth logic Start **/

    private Runnable simplexAsync = new Runnable() {
        @Override
        public void run() {
            Log.d("simplex","get nonce...");
            _server.getNonceAsync(_eventBus,getApplicationContext());
        }
    };

    @Subscribe
    public void getNonceCallback(SimplexNonceResponse nonceResponse)
    {
        Log.d("Simplex","GetNonceCallback...");
        Log.d("Simplex","simplex nonce: "+ nonceResponse.simplexNonce);
        Log.d("Simplex","google nonce: "+ nonceResponse.googleNonce);
        _simplexNonce = nonceResponse;
        initChecker();
        //execute the lvl code here
        _checker.checkAccess(_licenseCheckerCallback);
    }

    private void redirectWebView(String siteUrl,int responseCode, String signedData, String signature)
    {
        Log.d("Simplex","RedirectWebView...");
        String fullSiteUrl = String.format("%s?nonce=%s&wallet_address=%s&lvlcode=%s&lvlsignedData=%s&signature=%s",siteUrl,_simplexNonce.simplexNonce,_walletAddress,responseCode,signedData,signature);
        Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullSiteUrl));
        Log.d("webview post nonce",String.valueOf(_simplexNonce.simplexNonce));
        Log.d("webview post wallet",_walletAddress);
        Log.d("webview post lvlCode",String.valueOf(responseCode));
        Log.d("webview post signedData",signedData);
        Log.d("webview post signature",signature);
        setLayout(SimplexUITypes.WebView);
        startActivity(browserIntent);

    }

    /** Simplex App Auth logic End **/


    /** Activity UI logic Start **/
    private void setLayout(SimplexUITypes uiType)
    {
        _simplexUIType = uiType;
        switch (uiType)
        {
            case Loading:
                findViewById(R.id.llSimplexValidationWait).setVisibility(View.VISIBLE);
                findViewById(R.id.llSimplexLoadingProgress).setVisibility(View.GONE);
                findViewById(R.id.llSimplexErrorWrapper).setVisibility(View.GONE);
                break;

            case RetryLater:
                findViewById(R.id.llSimplexValidationWait).setVisibility(View.GONE);
                findViewById(R.id.llSimplexLoadingProgress).setVisibility(View.GONE);
                findViewById(R.id.llSimplexErrorWrapper).setVisibility(View.VISIBLE);

        }
    }

    @Subscribe
    public void displayError(final SimplexError error){
        Log.i("simplex", error.message);
        // Don't update UI if Activity is finishing.
        if (isFinishing())
            return;

        error.activityHandler.post(new Runnable() {
            public void run() {
                //update the UI
                TextView errorTextView = (TextView) findViewById(R.id.tvSimplexError);
                String errorMessage;
                if (error.message != null && !error.message.isEmpty())
                    errorMessage = generateDisplayErrorMessage(error.message);
                else
                    errorMessage = generateDisplayErrorMessage("we have connectivity error");

                errorTextView.setText(errorMessage);
                setLayout(SimplexUITypes.RetryLater);
            }
        });
    }

    private static String generateDisplayErrorMessage(String errorMessage)
    {
        return errorMessage + System.getProperty ("line.separator") + "please try again later.";
    }

    @Subscribe
    public void startAuth(final AuthEvent eventData){
        if (isFinishing()) {
            // Don't update UI if Activity is finishing.
            return;
        }
        eventData.activityHandler.post(new Runnable() {
            public void run() {
                redirectWebView(_server.getAuthRequestUrl(),eventData.responseData.responseCode,
                        eventData.responseData.signedData, eventData.responseData.signature);

            }
        });
    }

    public enum SimplexUITypes {
        Loading,
        WebView,
        RetryLater
    }

    /** Activity UI logic End **/


    /** LVL context start **/

    private void initChecker()
    {
        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Library calls this when it's done.
        _licenseCheckerCallback = new SimplexLicenseCheckerCallback(_activityHandler,_eventBus);

        // Construct the LicenseChecker with a policy.
        destroyCheckers();
        _checker = new LicenseChecker(
                getApplicationContext(), new ServerManagedPolicy(this,
                new AESObfuscator(_simplexNonce.simplexNonce.getBytes(Charsets.UTF_8), getPackageName(), deviceId)),_simplexNonce.googleNonce);
    }

    private void destroyCheckers()
    {
        if(_checker != null)
            _checker.onDestroy();
    }

    /** LVL context end **/

}
