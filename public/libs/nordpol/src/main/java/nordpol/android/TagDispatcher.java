package nordpol.android;

import android.nfc.Tag;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

public class TagDispatcher {
    private OnDiscoveredTagListener listener;
    private Activity activity;

    private TagDispatcher(Activity activity,
                          OnDiscoveredTagListener listener) {
        this.activity = activity;
        this.listener = listener;
    }

    public static TagDispatcher get(Activity activity,
                                    OnDiscoveredTagListener listener) {
        return new TagDispatcher(activity, listener);
    }

    /** Enable exclusive NFC access for the given activity.
     * @returns true if NFC was available and false if no NFC is available
     *          on device.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean enableExclusiveNfc() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                enableReaderMode(adapter);
            } else {
                enableForegroundDispatch(adapter);
            }
            return true;
        }
        return false;
    }

    /** Disable exclusive NFC access for the given activity.
     * @returns true if NFC was available and false if no NFC is available
     *          on device.
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public boolean disableExclusiveNfc() {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(activity);
        if (adapter != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                disableReaderMode(adapter);
            } else {
                disableForegroundDispatch(adapter);
            }
            return true;
        }
        return false;
    }

    public boolean interceptIntent(Intent intent) {
        Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
        if(tag != null) {
            listener.tagDiscovered(tag);
            return true;
        } else {
            return false;
        }
    }

    private void enableReaderMode(NfcAdapter adapter) {
        Bundle options = new Bundle();
        /* This is a work around for some Broadcom chipsets that does
         * the presence check by sending commands that interrupt the
         * processing of the ongoing command.
         */
        //options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 5000);
 	options.putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 30000);
        NfcAdapter.ReaderCallback callback = new NfcAdapter.ReaderCallback() {
                public void onTagDiscovered(Tag tag) {
                    listener.tagDiscovered(tag);
                }
            };
        adapter.enableReaderMode(activity,
                                 callback,
                                 NfcAdapter.FLAG_READER_NFC_A |
                                 NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK |
                                 NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
                                 options);
    }

    private void disableReaderMode(NfcAdapter adapter) {
        adapter.disableReaderMode(activity);
    }

    private void enableForegroundDispatch(NfcAdapter adapter) {
        // activity.getIntent() can not be use due to issues with pending intents containg extras of custom classes (https://code.google.com/p/android/issues/detail?id=6822)
        Intent intent = new Intent(activity, activity.getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        enableForegroundDispatch(adapter, intent);
    }

    private void enableForegroundDispatch(NfcAdapter adapter, Intent intent) {
        if(adapter.isEnabled()) {
            PendingIntent tagIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            IntentFilter tag = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);

            adapter.enableForegroundDispatch(activity, tagIntent, new IntentFilter[]{tag},
                                             new String[][]{new String[]{IsoDep.class.getName()}});
        }
    }

    private void disableForegroundDispatch(NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }

}
