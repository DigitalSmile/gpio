package org.digitalsmile.gpio.core;

/**
 * Helper class to convert from integer (byte) to hex string.
 */
public class IntegerToHex {
    private static final String digits = "0123456789abcdef";

    /**
     * Forbids creating an instance of this class.
     */
    private IntegerToHex() {
    }

    /**
     * Converts given integer (byte) input into hexadecimal string.
     *
     * @param input - integer (byte) input to convert
     * @return hexadecimal string representation of integer (byte)
     */
    public static String convert(int input) {
        var builder = new StringBuilder();
        builder.setLength(8);
        for (int i = 7; i >= 0; i--) {
            builder.setCharAt(i, digits.charAt(input & 15));
            input >>= 4;
        }
        builder.insert(0, "0x");
        return builder.toString();
    }
}
