package com.mycelium.wallet.pop;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class PopEncodeDecode {
    static String popURIEncode(String value) {
        try {
            if (value == null) {
                return null;
            }
            StringBuffer buffer = new StringBuffer();
            Character highSurrogate = null;
            for (char c : value.toCharArray()) {
                if (Character.isHighSurrogate(c)) {
                    highSurrogate = c;
                } else if (Character.isLowSurrogate(c)) {
                    if (highSurrogate == null) {
                        throw new RuntimeException("Found low surroggate without preceeding high surrogate!");
                    } else {
                        buffer.append(URLEncoder.encode(new String(new char[]{highSurrogate, c}), "UTF-8"));
                        highSurrogate = null;
                    }
                } else if (c > '~' || c < ' ' || c == '&' || c == '%' || c == '=' || c == '#') {
                    buffer.append(URLEncoder.encode(c + "", "UTF-8"));
                } else {
                    buffer.append(c);
                }
            }
            return buffer.toString();
        } catch (UnsupportedEncodingException e) {
            // will not happen. Famous last words.
            return null;
        }
    }

    static String popURIDecode(String value) {
        try {
            if (value == null) {
                return null;
            }
            StringBuffer result = new StringBuffer();

            char[] chars = value.toCharArray();
            StringBuffer encodedCharacter = null;
            for (int i = 0; i < chars.length; i++) {
                char c = chars[i];
                if (c == '%') {
                    if (encodedCharacter == null) {
                        encodedCharacter = new StringBuffer();
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
