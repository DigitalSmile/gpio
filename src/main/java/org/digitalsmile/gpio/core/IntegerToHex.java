package org.digitalsmile.gpio.core;

/**
 * Helper class to convert from integer (byte) to hex string.
 */
public class IntegerToHex {
    /**
     * Forbids creating an instance of this class.
     */
    private IntegerToHex() {
    }

    /**
     * Converts given integer (byte) input into hexadecimal string.
     *
     * @param input integer (byte) input to convert
     * @return hexadecimal string representation of integer (byte)
     */
    public static String convert(int input) {
        return String.format("0x%06x", input);
    }

    /**
     * Converts given long (byte) input into hexadecimal string.
     *
     * @param input long (byte) input to convert
     * @return hexadecimal string representation of long (byte)
     */
    public static String convert(long input) {
        return String.format("0x%06x", input);
    }
}
