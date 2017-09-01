package com.mycelium.wallet.service;

import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ClipboardManager.OnPrimaryClipChangedListener;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.IBinder;
import android.util.Log;

import com.mycelium.wallet.activity.ClipboardAlertActivity;

import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.crypto.MnemonicCode;
import org.bitcoinj.crypto.MnemonicException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Monitors the {@link ClipboardManager} for private keys and warns the user.
 */
// Check if we have lingering exported private keys, we want to warn
// the user if that is the case
public class ClipboardMonitorService extends Service {
    private static final String TAG = ClipboardMonitorService.class.getSimpleName();
    public static boolean stoppedExplicitly = false;

    private ClipboardManager clipboardManager;
    private OnPrimaryClipChangedListener primaryClipChangedListener;

    @Override
    public void onCreate() {
        super.onCreate();
        clipboardManager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        primaryClipChangedListener = new OnPrimaryClipChangedListener() {
            @Override
            public void onPrimaryClipChanged() {
                checkClipboard();
            }
        };
        clipboardManager.addPrimaryClipChangedListener(primaryClipChangedListener);
        checkClipboard();
    }

    private void checkClipboard() {
        Log.d(TAG, "onPrimaryClipChanged");
        CharSequence charSequence = clipboardManager.getPrimaryClip().getItemAt(0).getText();
        if (charSequence == null) {
            Log.d(TAG, "empty clipboard");
            return;
        }
        String text = charSequence.toString();
        new AsyncTask<String, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(String... params) {
                String text = params[0];
                Log.d(TAG, "Checking " + text);
                try {
                    DumpedPrivateKey.fromBase58(null, text);
                    return true;
                } catch (AddressFormatException ignore) {
                }
                try {
                    String[] words = text.split(" ");
                    if(words.length >= 12 && words.length % 3 == 0) {
                        List<String> wordList = new ArrayList<>(words.length);
                        Collections.addAll(wordList, words);
                        MnemonicCode.INSTANCE.check(wordList);
                        return true;
                    }
                } catch (MnemonicException ignored) {
                }
                try {
                    DeterministicKey key = DeterministicKey.deserializeB58(text, NetworkParameters.fromID(NetworkParameters.ID_MAINNET));
                    if(!key.isPubKeyOnly()) {
                        return true;
                    }
                } catch (IllegalArgumentException ignored) {
                }
                // TODO: 12/21/16 check for other private key representations, bip39 phrases, collections of keys, ...
                return false;
            }

            @Override
            protected void onPostExecute(Boolean found) {
                super.onPostExecute(found);
                if (found) {
                    Log.e(TAG, "priv key found!");
                    clipboardManager.setPrimaryClip(ClipData.newPlainText("deleted", ""));
                    Intent intent = new Intent(ClipboardMonitorService.this, ClipboardAlertActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                }
            }
        }.execute(text);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (clipboardManager != null) {
            clipboardManager.removePrimaryClipChangedListener(primaryClipChangedListener);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}