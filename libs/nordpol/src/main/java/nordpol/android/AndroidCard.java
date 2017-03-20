package nordpol.android;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;

import nordpol.IsoCard;
import nordpol.OnCardErrorListener;

public class AndroidCard implements IsoCard {
    private static final int DEFAULT_TIMEOUT = 15000;
    private static final int SAMSUNG_S5_MINI_MAX = 253;

    private IsoDep card;
    private List<OnCardErrorListener> errorListeners =
        new CopyOnWriteArrayList<OnCardErrorListener>();

    private AndroidCard(IsoDep card) {
        this.card = card;
    }

    public static AndroidCard get(Tag tag) throws IOException {
        IsoDep card = IsoDep.get(tag);

        /* Workaround for the Samsung Galaxy S5 (since the
         * first connection always hangs on transceive).
         * TODO: This could be improved if we could identify
         * Samsung Galaxy S5 devices
         */
        card.connect();
        card.close();

        if(card != null) {
            return new AndroidCard(card);
        } else {
            return null;
        }
    }

    private void notifyListeners(IOException exception) {
        for(OnCardErrorListener listener: errorListeners) {
            listener.error(this, exception);
        }
    }

    public void addOnCardErrorListener(OnCardErrorListener listener) {
        errorListeners.add(listener);
    }

    public void removeOnCardErrorListener(OnCardErrorListener listener) {
        errorListeners.remove(listener);
    }

    public boolean isConnected() {
        return card.isConnected();
    }

    public void connect() throws IOException {
        try {
            card.connect();
            card.setTimeout(DEFAULT_TIMEOUT);
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    public int getMaxTransceiveLength() throws IOException {
        /* TODO: This could be improved if we could identify
         * Samsung Galaxy S5 mini devices
         */
        return Math.min(card.getMaxTransceiveLength(), SAMSUNG_S5_MINI_MAX);
    }

    public int getTimeout() {
        return card.getTimeout();
    }

    public void setTimeout(int timeout) {
        card.setTimeout(timeout);
    }

    public void close() throws IOException {
        try {
            card.close();
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    public byte[] transceive(byte [] command) throws IOException {
        try {
            return card.transceive(command);
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    public List<byte[]> transceive(List<byte[]> commands) throws IOException {
        try {
            ArrayList<byte[]> responses = new ArrayList<byte[]>();
            for(byte[] command: commands) {
                responses.add(card.transceive(command));
            }
            return responses;
        } catch(IOException e) {
            notifyListeners(e);
            throw e;
        }
    }

    public Tag getTag() {
        return card.getTag();
    }
}
