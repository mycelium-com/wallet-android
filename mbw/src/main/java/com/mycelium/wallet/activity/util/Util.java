package com.mycelium.wallet.activity.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class Util {

    public static void toClipboard(Context context, String address) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", address);
        clipboard.setPrimaryClip(clip);
//        Toast.makeText(context, context.getString(R.string.copied_to_clipboard), Toast.LENGTH_LONG).show();
    }

    public static String fromClipboard(Context context) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard.getPrimaryClip() != null && clipboard.getPrimaryClip().getItemCount() > 0)
            return clipboard.getPrimaryClip().getItemAt(0).getText().toString();
        else
            return "";
    }
}
