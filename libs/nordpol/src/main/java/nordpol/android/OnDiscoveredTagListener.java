package nordpol.android;

import android.nfc.Tag;

public interface OnDiscoveredTagListener {
    public void tagDiscovered(Tag t);
}
