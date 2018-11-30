package com.mrd.bitlib.model;

/* Copyright (c) 2018 Coinomi Ltd
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

import java.util.Arrays;
import java.util.Locale;

/**
 * Class taken from: https://github.com/sipa/bech32/pull/40
 */
public class Bech32 {
    /**
     * The Bech32 character set for encoding.
     */
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    /**
     * The Bech32 character set for decoding.
     */
    private static final byte[] CHARSET_REV = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            15, -1, 10, 17, 21, 20, 26, 30, 7, 5, -1, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
            1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25, 9, 8, 23, -1, 18, 22, 31, 27, 19, -1,
            1, 0, 3, 16, 11, 28, 12, 14, 6, 4, 2, -1, -1, -1, -1, -1
    };

    public static class Bech32Exception extends Exception {
        Bech32Exception(final String s) {
            super(s);
        }
    }

    public static class Bech32Data {
        final String hrp;
        final byte[] values;

        private Bech32Data(final String hrp, final byte[] values) {
            this.hrp = hrp;
            this.values = values;
        }
    }

    /**
     * Find the polynomial with value coefficients mod the generator as 30-bit.
     */
    private static int polymod(final byte[] values) {
        int c = 1;
        for (byte v_i : values) {
            int c0 = (c >>> 25) & 0xff;
            c = ((c & 0x1ffffff) << 5) ^ (v_i & 0xff);
            if ((c0 & 1) != 0) c ^= 0x3b6a57b2;
            if ((c0 & 2) != 0) c ^= 0x26508e6d;
            if ((c0 & 4) != 0) c ^= 0x1ea119fa;
            if ((c0 & 8) != 0) c ^= 0x3d4233dd;
            if ((c0 & 16) != 0) c ^= 0x2a1462b3;
        }
        return c;
    }

    /**
     * Expand a HRP for use in checksum computation.
     */
    private static byte[] expandHrp(final String hrp) {
        int hrpLength = hrp.length();
        byte ret[] = new byte[hrpLength * 2 + 1];
        for (int i = 0; i < hrpLength; ++i) {
            int c = hrp.charAt(i) & 0x7f; // Limit to standard 7-bit ASCII
            ret[i] = (byte) ((c >>> 5) & 0x07);
            ret[i + hrpLength + 1] = (byte) (c & 0x1f);
        }
        ret[hrpLength] = 0;
        return ret;
    }

    /**
     * Verify a checksum.
     */
    private static boolean verifyChecksum(final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] combined = new byte[hrpExpanded.length + values.length];
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.length);
        System.arraycopy(values, 0, combined, hrpExpanded.length, values.length);
        return polymod(combined) == 1;
    }

    /**
     * Create a checksum.
     */
    private static byte[] createChecksum(final String hrp, final byte[] values) {
        byte[] hrpExpanded = expandHrp(hrp);
        byte[] enc = new byte[hrpExpanded.length + values.length + 6];
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.length);
        System.arraycopy(values, 0, enc, hrpExpanded.length, values.length);
        int mod = polymod(enc) ^ 1;
        byte[] ret = new byte[6];
        for (int i = 0; i < 6; ++i) {
            ret[i] = (byte) ((mod >>> (5 * (5 - i))) & 31);
        }
        return ret;
    }

    /**
     * Encode a Bech32 string.
     */
    public static String encode(final Bech32Data bech32) throws Bech32Exception {
        return encode(bech32.hrp, bech32.values);
    }

    /**
     * Encode a Bech32 string.
     */
    public static String encode(String hrp, final byte[] values) throws Bech32Exception {
        if (hrp.length() < 1) throw new Bech32Exception("Human-readable part is too short");
        if (hrp.length() > 83) throw new Bech32Exception("Human-readable part is too long");
        hrp = hrp.toLowerCase(Locale.ROOT);
        byte[] checksum = createChecksum(hrp, values);
        byte[] combined = new byte[values.length + checksum.length];
        System.arraycopy(values, 0, combined, 0, values.length);
        System.arraycopy(checksum, 0, combined, values.length, checksum.length);
        StringBuilder sb = new StringBuilder(hrp.length() + 1 + combined.length);
        sb.append(hrp);
        sb.append('1');
        for (byte b : combined) {
            sb.append(CHARSET.charAt(b));
        }
        return sb.toString();
    }

    /**
     * Decode a Bech32 string.
     */
    public static Bech32Data decode(final String str) throws Bech32Exception {
        boolean lower = false, upper = false;
        if (str.length() < 8) throw new Bech32Exception("Input too short");
        if (str.length() > 90) throw new Bech32Exception("Input too long");
        for (int i = 0; i < str.length(); ++i) {
            char c = str.charAt(i);
            if (c < 33 || c > 126) throw new Bech32Exception("Characters out of range");
            if (c >= 'a' && c <= 'z') lower = true;
            if (c >= 'A' && c <= 'Z') upper = true;
        }
        if (lower && upper) throw new Bech32Exception("Cannot mix upper and lower cases");
        int pos = str.lastIndexOf('1');
        if (pos < 1) throw new Bech32Exception("Missing human-readable part");
        if (pos + 7 > str.length()) throw new Bech32Exception("Data part too short");
        byte[] values = new byte[str.length() - 1 - pos];
        for (int i = 0; i < str.length() - 1 - pos; ++i) {
            char c = str.charAt(i + pos + 1);
            if (CHARSET_REV[c] == -1) throw new Bech32Exception("Characters out of range");
            values[i] = CHARSET_REV[c];
        }
        String hrp = str.substring(0, pos).toLowerCase(Locale.ROOT);
        if (!verifyChecksum(hrp, values)) throw new Bech32Exception("Invalid checksum");
        return new Bech32Data(hrp, Arrays.copyOfRange(values, 0, values.length - 6));
    }
}
