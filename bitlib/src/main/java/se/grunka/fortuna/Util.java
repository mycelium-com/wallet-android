package se.grunka.fortuna;

public class Util {
    public static byte[] twoLeastSignificantBytes(long value) {
        byte[] result = new byte[2];
        result[0] = (byte) (value & 0xff);
        result[1] = (byte) ((value & 0xff00) >> 8);
        return result;
    }

    public static int ceil(int value, int divisor) {
        return (value / divisor) + (value % divisor == 0 ? 0 : 1);
    }

   public static byte[] arrayCopyOf(byte[] seed, int seedLength) {
      byte[] copy = new byte[seedLength];
      System.arraycopy(seed, 0, copy, 0, Math.min(seed.length, seedLength));
      return copy;
   }
}
