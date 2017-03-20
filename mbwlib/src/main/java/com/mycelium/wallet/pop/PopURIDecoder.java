package com.mycelium.wallet.pop;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class PopURIDecoder {

    static String popURIDecode(String value) {
        try {
            if (value == null) {
                return null;
            }
            // In a URI '+' means '+' and NOT <space>.
            String valueToURLDecode = value.replace("+", "%2B");
            return URLDecoder.decode(valueToURLDecode, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            // will not happen. Famous last words.
            return null;
        }
    }
}
