package com.mycelium.wallet.pop;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

class PopEncodeDecode {

    static String popURIDecode(String value) {
        try {
            if (value == null) {
                return null;
            }
            StringBuilder result = new StringBuilder();

            char[] chars = value.toCharArray();
            StringBuilder encodedCharacter = null;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '%') {
                    if (encodedCharacter == null) {
                        encodedCharacter = new StringBuilder();
                    }
                    if (chars.length < i+3) {
                        throw new RuntimeException("Bad format of input "  + value);
                    }
                    encodedCharacter.append(c);
                    encodedCharacter.append(chars[++i]);
                    encodedCharacter.append(chars[++i]);
                } else {
                    if (encodedCharacter != null) {
                        result.append(URLDecoder.decode(encodedCharacter.toString(), "UTF-8"));
                        encodedCharacter = null;
                    }
                    result.append(c);
                }
            }
            if (encodedCharacter != null) {
                result.append(URLDecoder.decode(encodedCharacter.toString(), "UTF-8"));
            }
            return result.toString();
        } catch (UnsupportedEncodingException e) {
            // will not happen. Famous last words.
            return null;
        }
    }
}
