package nordpol;

import java.io.IOException;
import java.util.List;

public interface IsoCard {
    public void addOnCardErrorListener(OnCardErrorListener listener);
    public void removeOnCardErrorListener(OnCardErrorListener listener);
    public void close() throws IOException;
    public void connect() throws IOException;
    public int getMaxTransceiveLength() throws IOException;
    public int getTimeout();
    public boolean isConnected();
    public void setTimeout(int timeout);
    public byte[] transceive(byte[] data) throws IOException;
    public List<byte[]> transceive(List<byte[]> data) throws IOException;
}
