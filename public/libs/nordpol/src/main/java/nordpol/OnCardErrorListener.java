package nordpol;

import java.io.IOException;

public interface OnCardErrorListener {
    void error(IsoCard card, IOException exeption);
}
