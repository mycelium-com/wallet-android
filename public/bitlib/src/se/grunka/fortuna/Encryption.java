package se.grunka.fortuna;

import Rijndael.Rijndael;

public class Encryption {
    private final Rijndael rijndael = new Rijndael();

    public void setKey(byte[] key) {
        rijndael.makeKey(key, key.length * 8, Rijndael.DIR_ENCRYPT);
    }

    public byte[] encrypt(byte[] data) {
        byte[] result = new byte[data.length];
        rijndael.encrypt(data, result);
        return result;
    }
}
