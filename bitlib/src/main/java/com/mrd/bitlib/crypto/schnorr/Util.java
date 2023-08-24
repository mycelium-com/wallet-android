package com.mrd.bitlib.crypto.schnorr;

import java.io.File;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class Util {

    public static final int INT = 4;
    public static final int LONG = 8;
    public static final int DOUBLE = LONG;
    public static final int BOOLEAN = 1;
    static final byte TRUE = (byte) 0xFF;
    static final byte FALSE = 0;
    /**
     * Don't allow instantiation
     */
    private Util() {
    }

    public static void int2bin(byte[] out, int offset, int i) {
        out[offset + 0] = (byte) (i >> 24);
        out[offset + 1] = (byte) (i >> 16 & 0xff);
        out[offset + 2] = (byte) (i >> 8 & 0xff);
        out[offset + 3] = (byte) (i & 0xff);
    }

    public static byte[] int2bin(int i) {
        byte[] out = new byte[4];
        int2bin(out, 0, i);
        return out;
    }

    public static void long2bin(byte[] out, int offset, long i) {
        out[offset + 0] = (byte) (i >> 56);
        out[offset + 1] = (byte) (i >> 48 & 0xff);
        out[offset + 2] = (byte) (i >> 40 & 0xff);
        out[offset + 3] = (byte) (i >> 32 & 0xff);
        out[offset + 4] = (byte) (i >> 24 & 0xff);
        out[offset + 5] = (byte) (i >> 16 & 0xff);
        out[offset + 6] = (byte) (i >> 8 & 0xff);
        out[offset + 7] = (byte) (i & 0xff);
    }

    public static byte[] long2bin(long i) {
        byte[] answer = new byte[8];
        long2bin(answer, 0, i);
        return answer;
    }

    public static byte[] double2bin(double d) {
        return long2bin(Double.doubleToLongBits(d));
    }

    public static void double2bin(byte[] out, int offset, double d) {
        long2bin(out, offset, Double.doubleToLongBits(d));
    }

    public static byte[] boolean2bin(boolean b) {
        byte[] answer = new byte[1];
        answer[0] = ((b) ? TRUE : FALSE);
        return answer;
    }

    public static void boolean2bin(byte[] out, int offset, boolean b) {
        out[offset] = ((b) ? TRUE : FALSE);
    }

    public static int bin2int(byte[] array, int offset) {
        return ((array[offset + 0] & 0xff) << 24) + ((array[offset + 1] & 0xff) << 16)
                + ((array[offset + 2] & 0xff) << 8) + (array[offset + 3] & 0xff);
    }

    public static long bin2long(byte[] array, int offset) {
        return ((array[offset + 0] & 0xffL) << 56) + ((array[offset + 1] & 0xffL) << 48)
                + ((array[offset + 2] & 0xffL) << 40) + ((array[offset + 3] & 0xffL) << 32)
                + ((array[offset + 4] & 0xffL) << 24) + ((array[offset + 5] & 0xffL) << 16)
                + ((array[offset + 6] & 0xffL) << 8) + (array[offset + 7] & 0xffL);
    }

    public static long bin2long(byte[] array) {
        return bin2long(array, 0);
    }

    public static double bin2double(byte[] array, int offset) {
        return Double.longBitsToDouble(bin2long(array, offset));
    }

    public static double bin2double(byte[] array) {
        return Double.longBitsToDouble(bin2long(array));
    }

    public static boolean bin2boolean(byte[] array, int offset) {
        return array[offset] == TRUE;
    }

    public static boolean bin2boolean(byte[] array) {
        return array[0] == TRUE;
    }

    public static byte[] twosComplimentToPositiveInt(byte[] input) {
        byte[] output = input;
        if (input != null && input.length > 1 && input[0] == 0) {
            output = new byte[input.length - 1];
            System.arraycopy(input, 1, output, 0, output.length);
        }
        return output;
    }

    public static byte[] positiveIntToTwosCompliment(byte[] input) {
        byte[] output = input;
        if (input != null && input.length > 0 && input[0] < 0) {
            output = new byte[input.length + 1];
            System.arraycopy(input, 0, output, 1, input.length);
        }
        return output;
    }

    public static BigInteger byteToBigInt(byte[] input) {
        return new BigInteger(1, input);
    }

    public static byte[] bigIntToByte(BigInteger input) {
        return input.toByteArray();
    }

    public static byte[] joinBytes(byte[]... bytes) {
        int length = 0;
        for (byte[] byt : bytes) {
            length += byt.length;
        }
        byte[] answer = new byte[length + bytes.length * INT];// Content length + length fields
        int offset = 0;
        for (byte[] byt : bytes) {
            System.arraycopy(Util.int2bin(byt.length), 0, answer, offset, INT);
            offset += INT;
            System.arraycopy(byt, 0, answer, offset, byt.length);
            offset += byt.length;
        }
        return answer;
    }

    public static List<byte[]> splitBytes(byte[] bytes) {
        if (bytes == null) {
            throw new IllegalArgumentException("bytes cannot be null");
        }
        List<byte[]> answer = new ArrayList<byte[]>();
        if (bytes.length != 0) {
            if (bytes.length > INT) {
                int offset = 0;
                while (offset < bytes.length) {
                    int length = Util.bin2int(bytes, offset);
                    if (bytes.length >= offset + length) {
                        offset += INT;
                        byte[] dest = new byte[length];
                        System.arraycopy(bytes, offset, dest, 0, length);
                        offset += length;
                        answer.add(dest);
                    } else {
                        throw new IllegalArgumentException(
                                "bytes not long enough to contain as much as it says it does");
                    }
                }
            } else {
                throw new IllegalArgumentException(
                        "bytes must be at least big enough to have one length field in it or 0 length but was: "
                                + bytes.length);
            }
        }
        return answer;
    }
    public static byte[][] listByte2Array(List<byte[]> bytes){
        int size = bytes.size();
        byte[][] answer = new byte[size][];
        for (int i = 0; i < size; ++i){
            answer[i] = bytes.get(i);
        }
        return answer;
    }

    public static int compareByteArrays(byte[] first, byte[] second) {
        if (first.length < second.length) {
            return -1;
        } else if (first.length > second.length) {
            return 1;
        }
        if (Arrays.equals(first, second)) {
            return 0;
        }
        for (int i = 0; i < first.length; ++i) {
            if (first[i] < second[i]) {
                return -1;
            } else if (first[i] > second[i]) {
                return 1;
            }
        }
        return 0;
    }

    private static final int MIN_LENGTH = 8;
    private static final String SHORT_PASSWORD = "Password too short, must be at least " + MIN_LENGTH;
    private static final String SHORT_USERNAME = "Username too short, must be at least " + MIN_LENGTH;

    private static final String NULL_PASSWORD = "Password null, must be at least " + MIN_LENGTH;
    private static final String NULL_USERNAME = "Username null, must be at least " + MIN_LENGTH;

    public static void checkPassword(final String password) throws UnauthorisedException {
        if (password == null){
            throw new UnauthorisedException(NULL_PASSWORD);
        }
        if (password.length() < MIN_LENGTH) {
            throw new UnauthorisedException(SHORT_PASSWORD);
        }
    }

    public static void checkUsername(final String username) throws UnauthorisedException {
        if (username == null){
            throw new UnauthorisedException(NULL_USERNAME);
        }
        if (username.length() < MIN_LENGTH) {
            throw new UnauthorisedException(SHORT_USERNAME);
        }
    }

    /**
     * Deletes all files and subdirectories under dir. Returns true if all deletions were successful.
     * If a deletion fails, the method stops attempting to delete and returns false.
     *
     * From http://www.exampledepot.com/egs/java.io/DeleteDir.html
     *
     * @param dir
     * @return
     */
    public static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i = 0; i < children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so delete it
        return dir.delete();
    }

    /**
     * Add a Throwable containing information about where this method was originally called from to t
     *
     * @param t
     * @param from
     */
    public static void addFrom(Throwable t, Throwable from) {
        if (t.getCause() == null) {
            t.initCause(from);
        }
    }
    public static byte[] hashKey(byte[] publicKey) throws NoSuchAlgorithmException{
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        digest.update(publicKey);
        return digest.digest();
    }
}