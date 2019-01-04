package com.mycelium.wallet.simplex;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.vending.licensing.AESObfuscator;
import com.google.android.vending.licensing.LicenseChecker;
import com.google.android.vending.licensing.LicenseCheckerCallback;
import com.google.android.vending.licensing.ServerManagedPolicy;
import com.google.common.base.Charsets;
import com.mycelium.wallet.R;
import com.mycelium.wallet.activity.modern.ModernMain;
import com.squareup.otto.Bus;
import com.squareup.otto.Subscribe;
import com.squareup.otto.ThreadEnforcer;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;

public class SimplexMainActivity extends Activity {
    private static final int ERROR_CONTACTING_SERVER = 0x101;
    private static final int ERROR_INVALID_PACKAGE_NAME = 0x102;
    private static final int ERROR_NON_MATCHING_UID = 0x103;

    //the event bus entities
    private static final Bus _eventBus = new Bus(ThreadEnforcer.ANY, "Simplex");
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


        Button retryButton = findViewById(R.id.btSimplexRetry);
        retryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setLayout(SimplexUITypes.Loading);
                simplexAsync.run();
            }
        });

        Button cancelButton = findViewById(R.id.btCancel);
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        //simplex app auth
        simplexAsync.run();
    }

    @Override
    protected void onResume() {
        super.onResume();
        _eventBus.register(this);

        if (this._simplexUIType == SimplexUITypes.WebView) {
            Intent i = new Intent(SimplexMainActivity.this, ModernMain.class);
            i.setFlags(FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(i);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        _eventBus.unregister(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyCheckers();
    }

    private Runnable simplexAsync = new Runnable() {
        @Override
        public void run() {
            _server.getNonceAsync(_eventBus, getApplicationContext());
        }
    };

    @Subscribe
    public void getNonceCallback(SimplexNonceResponse nonceResponse) {
        _simplexNonce = nonceResponse;
        initChecker();
        //execute the lvl code here
        _checker.checkAccess(_licenseCheckerCallback);
    }

    private void redirectWebView(String siteUrl, int responseCode, String signedData, String signature) {
        boolean isReadyToTrade =
                responseCode != ERROR_CONTACTING_SERVER &&
                responseCode != ERROR_INVALID_PACKAGE_NAME &&
                responseCode != ERROR_NON_MATCHING_UID &&
                signedData != null &&
                signature != null;
        if (isReadyToTrade) {
            String fullSiteUrl = String.format("%s?nonce=%s&wallet_address=%s&lvlcode=%s&lvlsignedData=%s&signature=%s", siteUrl, _simplexNonce.simplexNonce, _walletAddress, responseCode, signedData, signature);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(fullSiteUrl));
            setLayout(SimplexUITypes.WebView);
            startActivity(browserIntent);
        } else {
            String message = getString(R.string.gp_required);
            SimplexError error = new SimplexError(new Handler(getMainLooper()), message);
            displayError(error);
        }
    }

    /* Simplex App Auth logic End **/


    /**
     * Activity UI logic Start
     **/
    private void setLayout(SimplexUITypes uiType) {
        _simplexUIType = uiType;
        switch (uiType) {
            case Loading:
                findViewById(R.id.llSimplexValidationWait).setVisibility(View.VISIBLE);
                findViewById(R.id.llSimplexLoadingProgress).setVisibility(View.GONE);
                findViewById(R.id.llSimplexErrorWrapper).setVisibility(View.GONE);
                break;
            case RetryLater:
                findViewById(R.id.llSimplexValidationWait).setVisibility(View.GONE);
                findViewById(R.id.llSimplexLoadingProgress).setVisibility(View.GONE);
                findViewById(R.id.llSimplexErrorWrapper).setVisibility(View.VISIBLE);
                break;
        }
    }

    public void displayError(final SimplexError error) {
        // Don't update UI if Activity is finishing.
        if (isFinishing()) {
            return;
        }

        error.handler.post(new Runnable() {
            public void run() {
                //update the UI
                TextView errorTextView = findViewById(R.id.tvSimplexError);
                TextView cancelButton = findViewById(R.id.btCancel);
                cancelButton.setVisibility(View.VISIBLE);
                String errorMessage;
                errorMessage = error.message;

                errorTextView.setText(errorMessage);
                setLayout(SimplexUITypes.RetryLater);
            }
        });
    }

    @Subscribe
    public void displayRetryError(final SimplexError error) {
        // Don't update UI if Activity is finishing.
        if (isFinishing()) {
            return;
        }

        error.handler.post(new Runnable() {
            public void run() {
                //update the UI
                TextView errorTextView = findViewById(R.id.tvSimplexError);
                String errorMessage;
                if (error.message != null && !error.message.isEmpty()) {
                    errorMessage = generateDisplayErrorMessage(error.message);
                } else {
                    errorMessage = generateDisplayErrorMessage("we have connectivity error");
                }

                errorTextView.setText(errorMessage);
                setLayout(SimplexUITypes.RetryLater);
            }
        });
    }

    private static String generateDisplayErrorMessage(String errorMessage) {
        return errorMessage + System.getProperty("line.separator") + "please try again later.";
    }

    @Subscribe
    public void startAuth(final AuthEvent eventData) {
        if (isFinishing()) {
            // Don't update UI if Activity is finishing.
            return;
        }
        eventData.activityHandler.post(new Runnable() {
            public void run() {
                redirectWebView(_server.getAuthRequestUrl(), eventData.responseData.responseCode,
                        eventData.responseData.signedData, eventData.responseData.signature);

            }
        });
    }

    public enum SimplexUITypes {
        Loading,
        WebView,
        RetryLater
    }

    /* Activity UI logic End */

    /**
     * LVL context start
     **/

    private void initChecker() {
        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Library calls this when it's done.
        _licenseCheckerCallback = new SimplexLicenseCheckerCallback(_activityHandler, _eventBus);

        // Construct the LicenseChecker with a policy.
        destroyCheckers();
        _checker = new LicenseChecker(
                getApplicationContext(), new ServerManagedPolicy(this,
                new AESObfuscator(_simplexNonce.simplexNonce.getBytes(Charsets.UTF_8), getPackageName(), deviceId)), _simplexNonce.googleNonce);
    }

    private void destroyCheckers() {
        if (_checker != null) {
            _checker.onDestroy();
        }
    }

    /* LVL context end */
}
