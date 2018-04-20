package com.mycelium.wallet.external.changelly.bch;


import com.mrd.bitlib.model.Address;
import com.mrd.bitlib.model.NetworkParameters;

import java.util.Arrays;
import java.util.Locale;

/**
 * This class was created to support new Bech32 like BCH addresses to legacy convertation.
 */
public class BCHBechAddress {
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";

    /**
     * Converts new Bech32 like address to {@link BechAddressParams} class, which could be used to construct legacy address.
     */
    public static BechAddressParams bchBechDecode(String bech) throws Exception {
        byte[] buffer = bech.getBytes();
        for (byte b : buffer) {
            if (b < 0x21 || b > 0x7e) {
                throw new Exception("bech32 characters out of range");
            }
        }
        if (!bech.equals(bech.toLowerCase(Locale.ROOT)) && !bech.equals(bech.toUpperCase(Locale.ROOT))) {
            throw new Exception("BCH bech32 cannot mix upper and lower case");
        }
        bech = bech.toLowerCase();
        int position = bech.lastIndexOf(":");
        if (position < 1) {
            throw new Exception("BCH bech32 missing separator");
        } else if (position + 7 > bech.length()) {
            throw new Exception("BCH bech32 separator misplaced");
        } else if (bech.length() < 8) {
            throw new Exception("BCH bech32 input too short");
        } else if (bech.length() > 90) {
            throw new Exception("BCH bech32 input too long");
        }
        String s = bech.substring(position + 1);
        for (int i = 0; i < s.length(); i++) {
            if (CHARSET.indexOf(s.charAt(i)) == -1) {
                throw new Exception("BCH bech32 characters  out of range");
            }
        }
        byte[] humanReadablePart = bech.substring(0, position).getBytes();
        byte[] data = new byte[bech.length() - position - 1];
        for (int j = 0, i = position + 1; i < bech.length(); i++, j++) {
            data[j] = (byte) CHARSET.indexOf(bech.charAt(i));
        }
        if (!verifyChecksum(humanReadablePart, data)) {
            throw new Exception("invalid BCH bech32 checksum");
        }
        byte[] payloadData = fromBase5Array(Arrays.copyOfRange(data, 0, data.length - 8));
        byte[] hash = Arrays.copyOfRange(payloadData, 1, payloadData.length);
        String type = getType(payloadData[0]);
        return new BechAddressParams(type, hash, new String(humanReadablePart));
    }

    private static String getType(byte versionByte) throws Exception {
        switch (versionByte & 120) {
            case 0:
                return "P2PKH";
            case 8:
                return "P2SH";
            default:
                throw new Exception(String.format("Invalid address type in version byte: %d.", versionByte));
        }
    }

    private static long polymod(byte[] values) {
        final long[] GENERATORS = {0x98f2bc8e61L, 0x79b76d99e2L, 0xf33e5fb3c4L, 0xae2eabe2a8L, 0x1e4f43e470L};
        long chk = 1;
        for (byte b : values) {
            byte top = (byte) (chk >> 0x23);
            chk = ((chk & 0x07ffffffffL) << 5) ^ b;
            for (int i = 0; i < 5; i++) {
                chk ^= ((top >> i) & 1) == 1 ? GENERATORS[i] : 0;
            }
        }
        return chk ^ 1;
    }

    private static byte[] hrpExpand(byte[] hrp) {
        byte[] buffer = new byte[hrp.length];
        for (int i = 0; i < hrp.length; i++) {
            buffer[i] = (byte) (hrp[i] & 0x1f);
        }
        byte[] ret = new byte[(hrp.length) + 1];
        System.arraycopy(buffer, 0, ret, 0, buffer.length);
        ret[buffer.length] = 0x00;
        return ret;
    }

    private static boolean verifyChecksum(byte[] hrp, byte[] data) {
        byte[] exp = hrpExpand(hrp);
        byte[] values = new byte[exp.length + data.length];
        System.arraycopy(exp, 0, values, 0, exp.length);
        System.arraycopy(data, 0, values, exp.length, data.length);
        return (polymod(values) == 0);
    }

    private static byte[] fromBase5Array(byte[] data) throws Exception {
        return convertBits(data, 5, 8);
    }

    private static byte[] convertBits(byte[] data, int from, int to) throws Exception {
        int length = (int) Math.floor(data.length * from / to);
        int mask = (1 << to) - 1;
        byte[] result = new byte[length];
        int index = 0;
        int accumulator = 0;
        int bits = 0;
        for (byte value : data) {
            if (!((0 <= value) && ((value >> from) == 0))) {
                throw new Exception(String.format("Invalid value %d", value));
            }
            accumulator = ((accumulator << from) | value);
            bits += from;
            while (bits >= to) {
                bits -= to;
                result[index] = (byte) ((accumulator >> bits) & mask);
                ++index;
            }
        }

        if (!((bits < from) && (((accumulator << (to - bits)) & mask) == 0))) {
            throw new Exception(String.format("Input cannot be converted to %d bits without padding," +
                    " but strict mode was used.", to));
        }
        return result;
    }

    /**
     * Class to handle converted info and construct legacy addresses.
     */
    public static class BechAddressParams {
        private String type;
        private byte[] hash;
        private String humanReadablePart;

        BechAddressParams(String type, byte[] hash, String humanReadablePart) {
            this.type = type;
            this.hash = hash;
            this.humanReadablePart = humanReadablePart;
        }

        public String getType() {
            return type;
        }

        public byte[] getHash() {
            return hash;
        }

        public String getHumanReadablePart() {
            return humanReadablePart;
        }

        public Address constructLegacyAddress(NetworkParameters networkParameters) {
            switch (type) {
                case "P2PKH":
                    return Address.fromStandardBytes(hash, networkParameters);
                case "P2SH":
                    return Address.fromP2SHBytes(hash, networkParameters);
            }
            throw new IllegalStateException("Type not supported.");
        }
    }
}